# Trade-offs

This document explains the main technical decisions made for the ACME Employee Salary Management application, why they were chosen, and what we give up by choosing them.

## Architecture

| Decision | Why we chose it | Trade-off |
|---|---|---|
| **Modular monolith** instead of microservices | 10,000 employees can easily be handled by one Spring Boot application. It keeps development, testing, and deployment simple. | If the system grows significantly, some modules may need to become separate services later. |
| **UUID** instead of Long IDs | UUIDs are harder to guess when IDs are exposed in URLs and are useful if the system becomes distributed later. | UUIDs use more database space and are less readable than numeric IDs. |
| **PostgreSQL** | Provides strong transactions, good reporting capabilities, and useful date-range features for salary history. | Some database features are PostgreSQL-specific. |

## Employee & Compensation

| Decision | Why we chose it | Trade-off |
|---|---|---|
| **Simple compensation: amount + currency** | The assignment focuses on salary management and analytics, not detailed payroll processing. | We cannot separately analyze base salary, bonus, or equity. |
| **Immutable salary history** | Historical salary records should not be changed after the fact. Corrections are represented by a new record. | Incorrect historical data cannot simply be edited. |
| **Salary periods use start and end dates** | This clearly defines which salary is active on a particular date and helps prevent gaps and overlaps. | The date rules need to be handled carefully. |
| **Database prevents overlapping salary periods** | Application checks alone can fail when two requests happen at the same time. The database provides a final safety check. | This uses PostgreSQL-specific functionality. |
| **One future salary change at a time** | Keeps the MVP simple and avoids complicated rules for editing or cancelling multiple future changes. | HR cannot schedule a long chain of future salary changes. |
| **Employees are terminated, not deleted** | Employee and salary history must remain available for reporting. | Old employee records remain in the database. |

## Analytics

| Decision | Why we chose it | Trade-off |
|---|---|---|
| **Calculate analytics in PostgreSQL** | The database can efficiently calculate totals, averages, medians, and groupings without loading all employees into Java. | Some analytics logic lives in SQL. |
| **Convert currencies before aggregation** | Employees may have different currencies, so values must be converted to a common reporting currency before calculating totals and averages. | Results depend on the exchange rates being used. |
| **Exchange rates behind an interface** | Keeps analytics independent from a specific exchange-rate source and makes testing easier. | The current implementation does not use live exchange rates. |
| **Show employees without salary separately** | Missing salary data is a data-quality issue and should not silently disappear from the dashboard. | Adds another metric to the dashboard. |

## Security

| Decision | Why we chose it | Trade-off |
|---|---|---|
| **JWT authentication** | The application has one main user type, the HR Manager. JWT keeps authentication simple and stateless. | It is not a full enterprise identity-management solution. |
| **Single HR Manager role** | More roles are not required for the MVP. | Different HR users cannot currently have different permissions. |

## Frontend

| Decision | Why we chose it | Trade-off |
|---|---|---|
| **Chart.js for analytics** | Provides readable charts, tooltips, and responsive visualizations with less custom chart code. | Adds a third-party charting dependency. |
| **Plain CSS** | The application is small enough that SCSS is unnecessary. CSS variables are sufficient for theming. | A larger application might benefit from a more structured styling system. |

## Testing & Data

| Decision | Why we chose it | Trade-off |
|---|---|---|
| **Unit tests for business rules** | They are fast and verify important employee and compensation behavior. | They do not prove database-specific behavior. |
| **PostgreSQL integration tests with Testcontainers** | Important because the application uses PostgreSQL-specific constraints and queries. | Tests require Docker. |
| **10,000 deterministic seed employees** | Makes the application realistic and allows analytics and basic performance testing. | Seed data is only for demonstration/testing and does not represent real employees. |

## Deliberately Out of Scope

| Feature | Reason |
|---|---|
| **Excel/CSV upload** | Useful for the HR team, but requires template validation, row-level error handling, and batch processing. It can be added later. |
| **Detailed pay components** | Base pay, bonus, equity, etc. would add complexity without being required for the core MVP. |
| **Payroll processing** | Taxes, deductions, payslips, and proration are a separate, country-specific domain. |
| **Employee self-service** | The current product is designed for the HR Manager persona. |
| **Approval workflows** | Not required by the assignment and would add another business process. |
| **Live exchange-rate integration** | A configurable provider keeps the dashboard predictable and tests deterministic. A live provider can be added later. |
| **AI/chatbot salary queries** | The requirement is satisfied through structured dashboards, filters, tables, and charts. |
| **Microservices** | The current scale does not justify the additional deployment and operational complexity. |

## Guiding Principle

> Keep the MVP simple enough to understand and maintain, while protecting important business rules and leaving clear extension points for future growth.

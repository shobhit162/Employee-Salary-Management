# Employee Salary Management System — Detailed Technical Document

## 1. Goal

Build a web-based salary management system that enables ACME's HR team to manage employee salary data and gain clear, actionable insights into how the organization pays its employees.

The system will replace the current Excel-based workflow with a centralized, maintainable application capable of supporting an initial organization of 10,000 employees and scaling beyond that.

The product will have two primary capabilities:

1. **Salary Management** — maintain employee salary information and salary history.
2. **Compensation Intelligence** — provide analytical dashboards, interactive filtering, and visualizations that help HR understand organizational pay patterns.

---

## 2. Primary User

### HR Manager

The HR Manager is responsible for:

* Viewing and searching employee information.
* Managing employee salary information.
* Viewing salary history.
* Analyzing compensation across organizational dimensions.
* Drilling down from analytical results to relevant employee cohorts.

The MVP is designed primarily for the HR Manager persona.

---

## 3. MVP Scope

## 3.1 Employee Management

The HR Manager can:

* View a paginated list of employees.
* Search employees.
* Filter employees by country, department, and employment status.
* Sort employee results.
* View an individual employee's details.
* Create an employee.
* Update employee information.
* Mark an employee as terminated/inactive.

Employees will not be physically deleted through normal HR workflows because employee and salary history must remain available for historical reporting and auditability.

---

## 3.2 Salary Management

Each employee can have salary records containing:

* Compensation amount.
* Currency.
* Effective start date.
* Effective end date where applicable.

For the MVP, compensation represents the employee's **annualized salary/compensation rate**.

Salary history must be preserved when compensation changes.

A salary change creates a new compensation record rather than modifying an existing historical record.

### Salary rules

* Salary records for the same employee must not overlap.
* A new salary change cannot have an effective date in the past.
* Future-dated salary changes are supported.
* Mid-month salary changes are supported.
* Historical salary records are immutable.
* An active employee should have continuous salary coverage.
* Monetary values must use precise decimal representation.
* Salary amounts must have an associated currency.

Actual payroll processing, salary proration, taxes, deductions, and payslip generation are outside the scope of the MVP.

---

## 3.3 Compensation Analytics & Business Intelligence

Analytics is a core product capability.

The HR Manager should be able to understand the organization's compensation structure through KPI cards, charts, tables, filtering, sorting, and drill-down.

### Top-level KPIs

The dashboard will provide:

* Active employee count.
* Total annualized payroll.
* Average salary.
* Median salary.
* Minimum salary.
* Maximum salary.

### Compensation breakdowns

Compensation can be analyzed by:

* Country.
* Department.
* Employment status.

For each applicable grouping, the system should support metrics such as:

* Employee count.
* Total payroll.
* Average salary.
* Median salary.
* Minimum salary.
* Maximum salary.

### Salary distribution

The dashboard will visualize how employees are distributed across salary ranges.

Salary distribution will use a selected reporting currency so that employees from different countries can be compared consistently.

### Interactive exploration

The HR Manager can:

* Filter by country.
* Filter by department.
* Filter by employment status.
* Select a reporting currency.
* Search and sort employees.
* Drill down from analytical results into relevant employee groups.
* Navigate from aggregated compensation information to individual employee records.

### Currency normalization

Salary records are stored in their original currency.

For organization-level analytics involving multiple currencies, compensation will be normalized into a selected reporting currency using configured exchange rates.

The currency conversion mechanism will be isolated behind an abstraction so that a more sophisticated exchange-rate provider can be introduced later without changing the core compensation domain.

---

## 4. Non-Functional Requirements

### 4.1 Scalability

The initial dataset will contain 10,000 employees.

The system should be designed so that increasing employee volume does not require a fundamental architectural change.

Employee APIs must use database-level pagination rather than loading the complete employee dataset into application memory.

Filtering, sorting, and aggregation should be performed at the database level where appropriate.

The backend will be implemented as a modular monolith. Microservices are intentionally not required for the initial scale.

### 4.2 Maintainability

The backend will use clear separation between:

* API/controller layer.
* Application/service layer.
* Domain model.
* Persistence layer.

Business logic should not be implemented inside controllers.

Dependencies should be injected rather than instantiated directly by business components.

Interfaces/abstractions should be introduced where they provide meaningful flexibility, such as exchange-rate providers.

The system should follow SOLID principles pragmatically without introducing unnecessary abstractions or design patterns.

### 4.3 Data Integrity

The system must:

* Preserve salary history.
* Prevent overlapping salary records.
* Prevent invalid salary effective dates.
* Maintain employee/salary relationships through database constraints.
* Use precise decimal representation for monetary values.
* Validate API input.
* Provide consistent error responses.
* Version-control database schema changes.

### 4.4 Security

Salary information is sensitive.

The application must establish authentication and authorization boundaries so that compensation APIs are not publicly accessible.

The MVP will focus on the HR Manager role and will not implement a complex enterprise-wide role hierarchy.

### 4.5 Testing

The application must contain meaningful automated tests covering core business functionality.

Tests should be:

* Fast.
* Deterministic.
* Readable.
* Focused on business behavior.

The test strategy will include:

* Unit tests for core services and business rules.
* API/controller tests.
* Integration tests for important persistence behavior.

### 4.6 Deployment

The complete application must be deployable with:

* Angular frontend.
* Spring Boot backend.
* PostgreSQL database.

The repository should contain the necessary configuration and documentation to run the system locally and deploy it to a suitable hosting environment.

---

## 5. Data Model — MVP Simplification

The MVP deliberately keeps the compensation model simple.

A compensation record contains:

```text
Compensation
------------
id
employeeId
amount
currency
effectiveFrom
effectiveTo
createdAt
```

The system does **not** separately model:

* Base pay.
* Variable pay.
* Bonuses.
* Stock/equity units.
* Benefits.
* Allowances.
* Payroll deductions.

For the purpose of this assessment, the compensation amount represents the employee's annualized salary/compensation rate.

### Reasoning

Modeling every compensation component would introduce additional domain complexity around:

* Different compensation frequencies.
* Bonus periods.
* Variable compensation rules.
* Equity valuation.
* Vesting schedules.
* Benefits.
* Payroll calculations.
* Country-specific compensation rules.

Those concerns are not necessary to demonstrate the core product value: managing salary data and understanding organizational pay.

The simplified model provides a clean foundation that can be extended later if product requirements require more detailed total-rewards modeling.

---

## 6. Employee Lifecycle

Employees are not hard-deleted through normal application workflows.

Instead, an employee's lifecycle is represented through employment status and relevant dates.

For example:

```text
ACTIVE
TERMINATED
```

Terminated employees remain in the database so that their historical salary information remains available for reporting and audit purposes.

### Reasoning

Hard deletion would destroy historical compensation information and could make historical analytics inaccurate.

For example, deleting an employee who previously earned a salary would change historical payroll and compensation statistics.

---

## 7. Explicitly Out of Scope

The following are deliberately excluded from the MVP.

### 7.1 Detailed Compensation Components

The MVP does not separately model base salary, variable pay, bonuses, stock units, benefits, or other compensation components.

**Reason:** The assessment focuses on salary management and organizational compensation analytics. A detailed total-rewards model would add substantial complexity without materially improving the core workflow.

The architecture should allow the compensation model to evolve later if required.

---

### 7.2 Payroll Processing

The system does not calculate:

* Payroll.
* Taxes.
* Deductions.
* Benefits.
* Payslips.
* Salary proration.
* Country-specific payroll rules.

**Reason:** Payroll is a separate and significantly more complex domain. The system manages compensation information rather than processing payroll.

Salary amounts are treated as annualized compensation rates for the purposes of analytics.

---

### 7.3 Employee Self-Service

Employees cannot log into the system to view or manage their compensation.

**Reason:** The defined primary persona is the HR Manager. Employee-facing workflows are not required to demonstrate the core product.

---

### 7.4 Complex Role-Based Access Control

The MVP does not implement a large hierarchy of roles such as:

* HR Admin.
* Finance Admin.
* Country HR Admin.
* Department Manager.
* Employee.

**Reason:** A single HR Manager persona is sufficient for the assessment. The application will establish authorization boundaries so the security model can be extended later.

---

### 7.5 Live Foreign Exchange Integration

The MVP does not depend on a third-party live FX service.

**Reason:** External service availability should not be required for core salary management or deterministic testing.

The application will use an exchange-rate abstraction so a live provider can be introduced later.

---

### 7.6 Excel/CSV Upload

The MVP does not provide an Excel upload workflow.

A future version may allow the HR Manager to upload a predefined Excel template containing employee and salary data, validate the contents, and import up to 10,000 employees in a controlled batch process.

**Reason:** Excel import directly addresses the organization's existing workflow and is a valuable future capability. However, a robust import feature introduces additional concerns such as:

* Template validation.
* File validation.
* Row-level validation.
* Duplicate detection.
* Partial failures.
* Error reporting.
* Batch processing.
* Transaction boundaries.

These concerns are not required to demonstrate the core salary management and analytics capabilities.

The MVP will instead provide deterministic seed data for the initial 10,000 employees.

---

### 7.7 HR/Payroll System Integrations

Integrations with systems such as Workday, SAP, ADP, or other external HR/payroll platforms are excluded.

**Reason:** External system contracts, authentication, synchronization, retries, and data reconciliation are outside the scope of the assessment.

---

### 7.8 AI/Chat-Based Compensation Queries

The product will not include a natural-language compensation chatbot or AI query interface.

**Reason:** Structured dashboards, analytical reporting, filtering, and visualization are the intended solution for answering compensation questions.

AI may be used as a development accelerator, but it is not a product dependency.

---

### 7.9 Notifications

Email, SMS, Slack, and other salary-change notifications are excluded.

**Reason:** Notifications are not necessary for the core HR salary management workflow.

---

## 8. Key Business Assumptions

1. Every employee has a unique employee identifier.
2. An employee can have multiple salary records over time.
3. An employee can have only one effective salary record at any point in time.
4. Active employees should have continuous salary coverage.
5. Historical salary records are immutable.
6. New salary changes must have a future effective date.
7. Compensation amounts represent annualized salary rates.
8. Monetary compensation has an associated ISO currency code.
9. Organization-level cross-currency analytics use a selected reporting currency.
10. Stock/equity value is not part of the MVP compensation calculation.
11. Terminated employees remain available for historical reporting.
12. Normal HR workflows do not hard-delete employees.
13. The initial system contains approximately 10,000 employees.
14. PostgreSQL is the production relational database.
15. The backend uses a modular monolithic architecture.

---

## 9. Edge Cases

The following cases must be explicitly handled by the application:

| Edge Case                                          | Expected Behavior                                             |
| -------------------------------------------------- | ------------------------------------------------------------- |
| Overlapping salary records                         | Reject the new record                                         |
| Salary change effective in the past                | Reject                                                        |
| Salary change effective in the future              | Allow                                                         |
| Mid-month salary change                            | Allow; treat salary as an annualized rate                     |
| Historical salary modification                     | Reject                                                        |
| Employee termination                               | Mark employee as terminated; retain history                   |
| Employee hard deletion                             | Not available through normal HR workflow                      |
| Active employee without salary                     | Reject or prevent activation until valid compensation exists  |
| Invalid/negative salary amount                     | Reject                                                        |
| Invalid currency                                   | Reject                                                        |
| Duplicate employee identifier                      | Reject                                                        |
| Invalid salary effective period                    | Reject                                                        |
| Multiple currencies in organization-wide analytics | Normalize into selected reporting currency                    |
| Missing exchange rate                              | Do not silently calculate; surface an appropriate error/state |
| Terminated employees in current metrics            | Exclude from active-employee metrics                          |
| Terminated employees in historical analysis        | Include when the selected analysis requires historical data   |
| Salary history gap for active employee             | Prevent or flag as invalid                                    |
| Future salary already scheduled                    | Prevent conflicting/overlapping future records                |


---

## 10. Future Enhancements

Potential future capabilities include:

* Excel/CSV employee and salary import.
* Detailed compensation components.
* Bonus and variable compensation management.
* Equity/stock-unit management.
* Employee self-service.
* Advanced role-based access control.
* Live foreign-exchange integration.
* HR/payroll system integrations.
* Notifications and approval workflows.
* Advanced compensation benchmarking.
* Compensation planning and salary review cycles.

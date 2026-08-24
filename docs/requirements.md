# Employee Salary Management — Requirements

## 1. Goal

Build a web-based salary management system for ACME's HR Manager to replace the current Excel-based workflow for managing salary data across approximately 10,000 employees in multiple countries.

The product has two primary goals:

1. Enable HR to efficiently manage employee salary information and salary history.
2. Enable HR to understand organizational compensation through business intelligence, analytical reporting, interactive filtering, and data visualization.

---

## 2. Primary User

**HR Manager**

The HR Manager should be able to manage employee salary information and answer questions such as:

* How much does the organization spend on salaries?
* What is the average/median salary?
* How does compensation differ by country or department?
* What is the organization's salary distribution?
* Which employee groups fall within a particular salary range?

---

## 3. MVP Scope & Features

### Employee Management

* View, search, sort, filter, and paginate employees.
* Filter by country, department, and employment status.
* View employee details.
* Create and update employee information.
* Mark employees as inactive/terminated without deleting historical data.

### Salary Management

* Store an employee's annualized salary amount and currency.
* Maintain salary history using effective dates.
* Support future-dated salary changes.
* Prevent overlapping salary periods.
* Keep historical salary records immutable.
* Support multiple currencies.

### Compensation Analytics

The application will provide an interactive dashboard containing:

* Active employee count.
* Total annualized payroll.
* Average salary.
* Median salary.
* Minimum and maximum salary.
* Salary distribution/bands.
* Salary breakdown by country.
* Salary breakdown by department.
* Employee count and payroll by selected dimensions.

The dashboard will support interactive filtering, sorting, and drill-down into employee cohorts.

For cross-country comparisons, salary values will be normalized into a selected reporting currency using a configurable exchange-rate mechanism.

### Platform

* Angular frontend.
* Java 21 + Spring Boot backend.
* PostgreSQL relational database.
* Versioned database migrations.
* Automated unit and integration tests.
* Seed data for 10,000 employees.
* Deployable end-to-end application.

---

## 4. Deliberately Out of Scope

### Detailed compensation components

The MVP models compensation as:

`amount + currency + effective dates`

It does not separately model bonuses, variable pay, stock/equity units, benefits, or allowances.

**Reason:** These introduce significant additional domain complexity while the core problem is salary management and organizational pay analytics. The model can be extended later if required.

### Payroll processing

Taxes, deductions, payslips, payroll calculations, and salary proration are excluded.

**Reason:** Payroll is a separate, country-specific domain. Salary amounts are treated as annualized compensation rates for this product.

### Excel/CSV upload

The MVP will not provide an Excel import workflow.

**Reason:** Excel migration is valuable but introduces template validation, row-level validation, duplicate handling, batch processing, partial failures, and error reporting. The MVP will instead use deterministic seed data for the initial 10,000 employees. Import can be added as a future capability.

### AI / Chatbot

Natural-language salary queries are excluded.

**Reason:** Structured dashboards, analytics, filtering, and visualization are the intended solution for this product. AI may be used during development but is not a product dependency.

### Other exclusions

The MVP does not include employee self-service, complex multi-role authorization, payroll/HR system integrations, notifications, or live third-party FX integration.


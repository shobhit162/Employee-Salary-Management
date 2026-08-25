# Architecture

## 1. System Overview

The application is a **modular monolith** with one backend, one database, and one frontend.

```text
Angular 20
    |
    | HTTPS / JWT
    v
Spring Boot
    |
    v
PostgreSQL
```

The backend is divided into feature-based modules:

```text
employee
compensation
analytics
security
common
seed
```

These are code modules, not separate services. This keeps the application simple while keeping clear boundaries for future growth.

## 2. Backend Structure

Each feature follows a simple layered structure:

```text
Controller
    ↓
Service
    ↓
Repository
    ↓
PostgreSQL
```

- **Controller** — handles HTTP requests, validation, and responses.
- **Service** — contains business rules and transaction handling.
- **Repository** — handles database access.
- **Entity/Domain** — represents the core business data and protects important rules.

Business logic is kept out of controllers.

### Employee

Handles:

- employee creation and updates
- search, filtering, and pagination
- employee termination

Employees are never physically deleted. A terminated employee keeps their salary history.

### Compensation

Handles the salary timeline.

A compensation record contains:

```text
amount
currency
effectiveFrom
effectiveTo
```

Salary periods use:

```text
[effectiveFrom, effectiveTo)
```

Important rules:

- salary periods cannot overlap
- historical salary records cannot be changed
- only future salary changes can be cancelled
- only one future salary change is allowed at a time
- terminated employees cannot receive new compensation
- an employee cannot be terminated while a future salary change is pending

The application validates these rules, while PostgreSQL also prevents overlapping salary periods at the database level.

### Analytics

Analytics is read-only.

The database calculates:

- employee count
- total payroll
- average and median salary
- minimum and maximum salary
- breakdown by country and department
- salary distribution

Calculations are performed in PostgreSQL rather than loading all employees into Java.

For different currencies, values are converted to the selected reporting currency before aggregation.

### Security

The application uses JWT authentication.

The MVP has one role:

```text
HR_MANAGER
```

All application APIs require authentication.

### Common

Contains shared functionality such as:

- global error handling
- application clock used for testable date logic
- currency conversion abstraction

## 3. Database

PostgreSQL is used because the application needs:

- transactions
- relational constraints
- reporting queries
- salary date-range validation

Main tables:

```text
employees
    id
    employee_code
    name
    country
    department
    job_title
    employment_status
    termination_date

compensations
    id
    employee_id
    amount
    currency
    effective_from
    effective_to
```

Money uses:

```text
Java       → BigDecimal
PostgreSQL → NUMERIC
```

Flyway manages database migrations and Hibernate runs with:

```properties
spring.jpa.hibernate.ddl-auto=validate
```

This ensures database schema changes are controlled through migrations.

## 4. Frontend

The frontend uses Angular 20 with standalone components.

Feature structure:

```text
core/
shared/
features/
    employees/
    compensation/
    analytics/
```

The frontend contains:

- employee management screens
- compensation management screens
- analytics dashboard
- authentication

Angular services handle API communication.

The dashboard provides filters, KPI cards, tables, and charts for compensation analysis.

## 5. Testing

Testing is divided into:

### Unit tests

Used for:

- employee business rules
- compensation rules
- validation
- service behavior

These tests are fast and use mocked dependencies.

### Integration tests

Use real PostgreSQL through Testcontainers to verify:

- Flyway migrations
- database constraints
- salary overlap prevention
- analytics queries
- API behavior

This is important because some salary rules depend on PostgreSQL-specific behavior.

## 6. Deployment

The intended deployment is:

```text
Angular / Nginx
       |
       v
Spring Boot
       |
       v
PostgreSQL
```

Docker Compose can be used to run the complete application locally or in a simple deployment environment.

## 7. Scalability

The current architecture is appropriate for 10,000 employees.

If the system grows significantly, the following can be added without changing the core product design:

- database read replicas
- caching
- materialized analytics views
- separate analytics infrastructure
- independent services for heavily used modules

The application deliberately avoids these until there is a real need for them.

# Employee Salary Management — Architecture

## 1. Architecture Style

The application will use a **modular monolith**.

For the initial scale of approximately 10,000 employees, microservices would add operational and deployment complexity without providing meaningful benefits.

The system will have:

```text
                    Angular
                       |
                    REST API
                       |
              Spring Boot Backend
                       |
        +--------------+--------------+
        |              |              |
     Employee     Compensation    Analytics
      Module         Module         Module
        |              |              |
        +--------------+--------------+
                       |
                   PostgreSQL
```

## 2. Backend

Technology:

* Java 21
* Spring Boot
* Spring Web
* Spring Data JPA / Hibernate
* PostgreSQL
* Flyway
* Spring Security
* Bean Validation
* JUnit 5 / Mockito

The backend will follow clear separation of responsibilities:

```text
Controller
    ↓
Service / Application Layer
    ↓
Domain
    ↓
Repository
    ↓
Database
```

Controllers will handle HTTP concerns only. Business rules will reside in services/domain components, and persistence will be isolated behind repositories.

## 3. Core Modules

### Employee

Responsible for:

* Employee information.
* Search/filtering.
* Employee lifecycle/status.
* Employee retrieval.

### Compensation

Responsible for:

* Salary records.
* Salary history.
* Effective dates.
* Salary business rules.
* Currency information.

Historical compensation records will be immutable.

### Analytics

Responsible for:

* Compensation KPIs.
* Aggregations.
* Salary distributions.
* Country/department analysis.
* Filtering and drill-down.

Analytics aggregation will be performed at the database level where appropriate to avoid loading large datasets into application memory.

## 4. Database

PostgreSQL will be the primary relational database.

The core relationship is:

```text
Employee 1 ─────────── * Compensation
```

An employee may have many historical compensation records, but only one compensation record may be effective at a given point in time.

Database indexes will be added based on query patterns such as:

* Employee identifier.
* Email.
* Country.
* Department.
* Employment status.
* Compensation employee/date fields.

## 5. Scalability

The application is designed to scale beyond the initial 10,000 employees through:

* Database-level pagination.
* Indexed filtering/search.
* Database-side aggregation.
* Stateless REST APIs.
* Connection pooling.
* Efficient SQL queries.

Caching or asynchronous processing will only be introduced if actual performance requirements justify them.

## 6. Currency Conversion

Salary records retain their original currency.

Analytics requiring cross-country comparison will use a reporting currency.

The application will depend on an abstraction such as:

```text
ExchangeRateProvider
        |
        +-- Configured/Static Provider
        |
        +-- Future External Provider
```

This keeps the compensation domain independent from any particular FX provider.

## 7. Testing

Testing will be layered:

```text
Unit Tests
    ↓
Service / Business Rules

Integration Tests
    ↓
API + Database

End-to-End Tests
    ↓
Critical user workflows
```

The primary focus will be fast, deterministic unit tests for core salary and analytics rules, supplemented by integration tests for important persistence/API behavior.

## 8. Deployment

The target deployment consists of:

```text
Angular Frontend
       |
       v
Spring Boot Backend
       |
       v
PostgreSQL
```

The application will be packaged so that the complete system can be run locally and deployed to a suitable cloud environment.

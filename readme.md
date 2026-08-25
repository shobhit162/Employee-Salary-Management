# ACME Salary Management

Web-based salary management for ACME's HR team — replacing the spreadsheets
currently used to track pay for ~10,000 employees across different countries.

It does two things:

1. **Manage salaries** — hire, search, update and terminate employees; set and
   schedule salary changes; keep an immutable salary history.
2. **Answer questions about pay** — total payroll, average and median salary,
   how pay differs by country and department, and how salaries are distributed —
   all in a single reporting currency, with drill-down into the employees behind
   any number.

| | |
| --- | --- |
| **Backend** | Java 21 · Spring Boot 4.1 · Spring Data JPA · Flyway · Spring Security (JWT) |
| **Database** | PostgreSQL 16 |
| **Frontend** | Angular 20 (standalone, signals, zoneless) |
| **Tests** | JUnit 5 · Mockito · AssertJ · Testcontainers |

---

## Quick start

### With Docker (everything, seeded)

```bash
docker compose up --build
```

Then open <http://localhost:4200> and sign in with
`hr.manager@acme.com` / `ChangeMe123!`.

The first start creates the schema and seeds 10,000 employees with salary
history — roughly 40,000 compensation records. Seeding is skipped on later
starts.

### Without Docker

You need a PostgreSQL 16 database, JDK 21+, and Node 20+.

```bash
createdb employee_management

# Backend, seeding on first run
cd backend
SEED_ENABLED=true ./mvnw spring-boot:run

# Frontend, in a second terminal
cd frontend
npm install
npm start          # http://localhost:4200, proxied to the backend
```

`ng serve` proxies `/api` to `localhost:8080` (`proxy.conf.json`), so the browser
only ever talks to one origin and CORS never comes into play.

---

## Configuration

Everything is overridable by environment variable. The defaults exist so a clean
checkout starts; **override the two marked below in any shared environment.**

| Variable | Default | Purpose |
| --- | --- | --- |
| `DB_URL` | `jdbc:postgresql://localhost:5432/employee_management` | Database |
| `DB_USERNAME` / `DB_PASSWORD` | `postgres` / `postgres` | Database credentials |
| `JWT_SECRET` | dev value | **Override.** Token signing key, min 32 characters |
| `HR_USERNAME` / `HR_PASSWORD` | `hr.manager@acme.com` / `ChangeMe123!` | **Override.** The HR Manager account; the password is bcrypt-hashed at startup and never stored |
| `SEED_ENABLED` | `false` | Seed 10,000 employees, if the database is empty |
| `ALLOWED_ORIGINS` | `http://localhost:4200` | CORS origins, only needed when the UI is served from another origin |
| `SERVER_PORT` | `8080` | HTTP port |

Exchange rates live in `application.properties` under `app.exchange-rates`.

---

## Tests

```bash
cd backend
./mvnw test      # 30 unit tests, ~1.5s, no Docker or database needed
./mvnw verify    # + 20 integration tests against a real PostgreSQL container
```

`mvn test` covers the business rules with mocked repositories and a fixed
`Clock`, so date-dependent behaviour is deterministic.

`mvn verify` adds the tests that can only be written against a real database:
Flyway migrations, the exclusion constraint that stops salary periods
overlapping, the analytics SQL, and the HTTP contract end to end including
authentication. They need Docker; without it they are reported as **skipped**
rather than failing, so `mvn verify` still works on a machine without Docker —
the skip count makes the gap visible.

To run them against a PostgreSQL you already have running (CI service container,
or a dev machine without Docker):

```bash
./mvnw verify \
  -Dtest.postgres.container=false \
  -Dspring.datasource.url=jdbc:postgresql://localhost:5432/employee_management_test \
  -Dspring.datasource.username=postgres \
  -Dspring.datasource.password=postgres
```

Frontend:

```bash
cd frontend
npm run test:ci    # 40 tests, headless Chrome, ~0.3s
npm run build
```

The frontend tests cover the parts with real logic: session handling and token
expiry, the auth interceptor, how filters are turned into query parameters,
money formatting, error-message extraction, and the salary form's mirroring of
the server's effective-date rule. They run zoneless, exactly as the app does.

---

## API

All endpoints require `Authorization: Bearer <token>` except `login` and
`/actuator/health`. Errors are RFC 9457 problem responses.

```
POST   /api/v1/auth/login                                  → token
GET    /api/v1/auth/me                                      → current user

GET    /api/v1/employees?search=&countryCode=&department=&status=
                        &page=&size=&sortBy=&direction=      → page of employees + current salary
POST   /api/v1/employees                                     → hire
GET    /api/v1/employees/{id}
PUT    /api/v1/employees/{id}
POST   /api/v1/employees/{id}/termination                    → terminate (history is kept)
GET    /api/v1/employees/filter-options                      → countries and departments in use

GET    /api/v1/employees/{id}/compensations                  → current + scheduled + history
POST   /api/v1/employees/{id}/compensations                  → set or schedule a salary
DELETE /api/v1/employees/{id}/compensations/{compensationId} → cancel a scheduled change

GET    /api/v1/analytics/summary                             → KPIs
GET    /api/v1/analytics/breakdown?dimension=COUNTRY|DEPARTMENT
GET    /api/v1/analytics/distribution?bandSize=              → salary histogram
GET    /api/v1/analytics/currencies                          → reporting currencies
```

Analytics endpoints share the filters `countryCode`, `department`,
`status` (`ACTIVE` by default, `ALL` to include leavers), `currency` and `asOf`.

### Example

```bash
TOKEN=$(curl -s localhost:8080/api/v1/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"hr.manager@acme.com","password":"ChangeMe123!"}' \
  | python -c 'import json,sys; print(json.load(sys.stdin)["token"])')

curl -s -H "Authorization: Bearer $TOKEN" \
  'localhost:8080/api/v1/analytics/summary?currency=USD'
```

---

## How the salary model works

A salary is an **annualised amount in a currency, effective over a period**.
Periods are half-open — `[effectiveFrom, effectiveTo)` — so consecutive periods
share a boundary date without overlapping.

```
   hired            raise            raise           today      scheduled
     │                │                │               │            │
     ▼                ▼                ▼               ▼            ▼
     ├── 82,000 ──────┼── 89,500 ──────┼── 96,000 ─────────────────►│
     │                │                │                            │
     └── history ─────┴────────────────┴─ current (open-ended) ─────┘
```

The rules the system enforces:

* A salary change creates a new record; historical records are never modified.
* Two periods for the same employee can never overlap — enforced by a PostgreSQL
  exclusion constraint, not only by application code.
* A **first** salary may take effect today. **Changing** an existing salary must
  take effect on a future date, because the current period is already being paid.
* One pending change at a time. Cancelling it re-opens the previous period, so
  the employee is never left without a salary.
* An employee cannot be terminated while a change is pending.
* Termination keeps the salary history — historical payroll stays accurate.
* Money is `BigDecimal` end to end; `NUMERIC(19,2)` in the database.

---

## Design notes

**Aggregation runs in the database.** Analytics queries return one row per
reported group, never one row per employee, so response size and memory stay
flat as headcount grows.

**Currency conversion happens before aggregation,** by passing the rate table
into the query. Converting afterwards would be wrong for the median: the median
of per-currency medians is not the organisation's median.

**A missing exchange rate is an error, not a zero.** If a currency in the data
cannot be converted, the request is refused and names the currency, rather than
silently dropping those salaries and reporting a plausible-looking smaller
payroll.

**Employees without a salary are counted separately** and surfaced on the
dashboard. An active employee with no pay on record is a data-quality problem HR
should see, not one to average away.

The reasoning behind these and every other significant choice — including what
was deliberately left out — is in [docs/trade-offs.md](docs/trade-offs.md).

---

## Repository layout

```
backend/                          Spring Boot application
  src/main/java/com/acme/employeemanagement/
    employee/                     employee records and lifecycle
    compensation/                 the salary timeline
    analytics/                    aggregation and reporting
    common/                       exception handling, currency, clock
    security/                     authentication
    seed/                         10,000-employee generator
  src/main/resources/db/migration Flyway migrations
frontend/                         Angular application
  src/app/core/                   API clients, auth, models
  src/app/shared/                 pipes reused across features
  src/app/features/               auth, employees, analytics —
                                  one folder per component (ts/html/css/spec)
docs/
  requirements.md                 goal, scope, what is out of scope and why
  architecture.md                 structure and data model
  trade-offs.md                   every significant decision and its cost
  ai-usage.md                     how AI was used, and what it got wrong
docker-compose.yml                full stack
```

---

## Status

Verified on a development machine:

* Backend builds; 30 unit tests and 20 integration tests pass against
  PostgreSQL 16.4.
* Flyway migrations apply to an empty database.
* The seeder produces 10,000 employees, 41,477 compensation records, 10
  countries, 10 departments, 10 currencies.
* The API was exercised end to end against the seeded database — login,
  authorisation, employee search, the salary lifecycle, and every analytics
  endpoint.
* The Angular app builds (78 kB initial transfer, lazy chunks per feature) and
  its 40 unit tests pass in headless Chrome.

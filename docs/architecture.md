# Architecture

## 1. Shape of the system

A modular monolith. One Spring Boot application, one PostgreSQL database, one
Angular client.

```text
                      Angular 20 (browser)
                             │
                     JWT over HTTPS
                             │
              ┌──────────────▼──────────────┐
              │      Spring Boot 4.1        │
              │                             │
              │   security ── employee      │
              │      │           │          │
              │      │      compensation    │
              │      │           │          │
              │      └──────  analytics     │
              │                  │          │
              │      common (errors, clock, │
              │              currency)      │
              └──────────────┬──────────────┘
                             │
                       PostgreSQL 16
```

Modules are Java packages, not deployables. The boundaries are drawn where
service boundaries would go if the system were ever split — see
[trade-offs.md](trade-offs.md#architecture).

Dependency direction: `analytics` reads `compensation` and `employee`;
`employee` reads `compensation` (to show current pay in the list and to block a
termination with a pending change); nothing depends on `analytics`.

## 2. Layers

```text
Controller     HTTP only: parameter binding, validation, status codes
     ↓
Service        business rules, transaction boundaries, locking
     ↓
Domain         entities that enforce their own invariants
     ↓
Repository     Spring Data JPA, plus JdbcClient for aggregation
     ↓
PostgreSQL     constraints that make the invariants true under concurrency
```

Controllers contain no business logic. Entities are not anaemic: `Compensation`
refuses to close a period that is already closed or to close one before it
starts, and `Employee` refuses to be terminated twice.

## 3. Modules

### employee

Employee records and lifecycle. Search, filtering and paging are pushed to the
database through the Specification pattern — the four filters compose into one
query rather than being applied in memory.

Employees are never hard-deleted. `ACTIVE → TERMINATED` is the whole lifecycle,
and a terminated employee keeps every salary record, so historical payroll
figures stay accurate.

Listing employees costs two queries per page regardless of page size: one for
the page, one to fetch the current salary for exactly those employees. Fetching
salaries lazily per row would be an N+1.

### compensation

The salary timeline. This is where the product's real complexity lives.

A compensation record is an amount, a currency, and a half-open period
`[effectiveFrom, effectiveTo)`. Consecutive periods share a boundary date, so no
date arithmetic is needed to make them adjacent.

```text
Employee 1 ──────── * Compensation

  history (closed)          current (open-ended)        scheduled
  ├── 82,000 ──┼── 89,500 ──┼─────── 96,000 ──────────►│── 104,000 ──►
              past          today                    future
```

Invariants:

| Rule | Enforced by |
| --- | --- |
| No two periods overlap for one employee | PostgreSQL exclusion constraint, plus a service check for the error message |
| Historical and current records are immutable | Service — only a future-dated record can be cancelled |
| One pending change at a time | Service, under a row lock on the employee |
| A first salary may start today; a change may not | Service |
| Cannot pay a terminated employee | Service |
| Cannot terminate with a change pending | Service, under the same row lock |
| Amount > 0, currency is a 3-letter code | Bean Validation, service, and a database check constraint |

Concurrency: salary changes and terminations both take
`SELECT ... FOR UPDATE` on the *employee* row. That makes the employee the
serialisation point for every decision that reads the timeline before writing to
it. Different employees never contend.

Ordering: writes that close one period and open another must reach the database
in that order. Hibernate flushes inserts before updates, so both the "schedule a
change" and "cancel a change" paths flush explicitly between the two statements —
otherwise the rows briefly overlap and the exclusion constraint rejects the
transaction.

### analytics

Read-only aggregation. Three questions, three endpoints: headline KPIs, a
breakdown by country or department, and a salary distribution.

Every query runs entirely in PostgreSQL and returns **one row per reported
group**, never one row per employee. With 10,000 employees the difference is
already an order of magnitude in response size; the point is that it does not
change as headcount grows.

Cross-currency reporting works by injecting the rate table into the query as two
parallel arrays and converting **before** aggregating:

```sql
with fx as (
    select t.currency, t.rate
    from unnest(string_to_array(:currencies, ','),
                string_to_array(:rates, ',')::numeric[]) as t(currency, rate)
),
salary as (
    select e.id, e.country_code, e.department, c.amount * fx.rate as amount
    from employees e
    left join compensations c on ... and c.effective_from <= :asOf
                             and (c.effective_to is null or c.effective_to > :asOf)
    left join fx on fx.currency = c.currency
    where ...
)
```

Two details matter:

* **Conversion before aggregation.** Sums survive being converted afterwards;
  medians do not. `percentile_cont` over already-converted amounts is the
  organisation's median. A median of per-currency medians is not.
* **`LEFT JOIN`, not `JOIN`.** An employee with no salary must still be counted.
  The response reports `employeeCount` and `compensatedEmployeeCount`
  separately, so a gap is visible instead of quietly shrinking headcount.

A currency with no configured rate would be dropped by the join, so the service
checks the rate table covers every currency in use *before* querying and refuses
the request otherwise.

### security

Stateless bearer tokens. `POST /api/v1/auth/login` verifies credentials against
accounts held in configuration (bcrypt-hashed at startup) and issues an HS256
JWT carrying the caller's roles. Every other endpoint requires
`ROLE_HR_MANAGER`.

No sessions, so no CSRF surface and nothing to replicate between instances.

### common

Cross-cutting pieces: the `@RestControllerAdvice` that turns exceptions into
RFC 9457 problem responses, the injected `Clock` that makes date-dependent rules
testable, and the `ExchangeRateProvider` abstraction.

### seed

An opt-in `ApplicationRunner` that generates a realistic 10,000-employee
organisation with batched JDBC inserts. Deterministic from a fixed random seed,
and refuses to run against a non-empty table.

## 4. Data model

```sql
employees
  id UUID PK, employee_code UNIQUE, email UNIQUE,
  first_name, last_name, country_code, department, job_title,
  employment_status CHECK (ACTIVE|TERMINATED),
  termination_date  CHECK (null iff ACTIVE),
  created_at, updated_at

compensations
  id UUID PK, employee_id FK,
  amount NUMERIC(19,2) CHECK (> 0),
  currency VARCHAR(3)  CHECK (~ '^[A-Z]{3}$'),
  effective_from DATE, effective_to DATE CHECK (> effective_from),
  created_at,
  EXCLUDE USING GIST (employee_id WITH =,
                      daterange(effective_from, effective_to, '[)') WITH &&)
```

Money is `BigDecimal` in Java and `NUMERIC` in PostgreSQL, never a floating-point
type.

Indexes follow the query patterns: `country_code`, `department` and
`employment_status` on employees for filtering; `(employee_id, effective_from)`
on compensations for timeline reads. The unique constraints on `employee_code`
and `email` are index-backed and serve the duplicate checks.

Flyway owns the schema and Hibernate runs with `ddl-auto=validate`, so a drift
between the entities and the migrations fails at startup rather than silently
altering a database.

## 5. Frontend

Angular 20: standalone components, signals for state, zoneless change detection,
and the built-in control flow. No state-management library and no component
library — the app is three screens.

```text
core/                      singletons: API clients, auth, models
  api/ auth/ models/
shared/                    reusable across features
  pipes/money/
features/                  one folder per area, mirroring the backend modules
  auth/login/
  employees/employee-list/ employee-detail/ employee-form/ salary-change-form/
  analytics/dashboard/ bar-chart/ histogram/
```

**One folder per component**, each holding `.ts`, `.html`, `.css` and
`.spec.ts` — the Angular CLI's default layout, so `ng generate component`
produces something that fits without rearranging.

Splitting the template out of the `.ts` matters once a component has real
markup: the dashboard has a filter bar, six KPI cards, two charts and a table,
and as an inline string that was a 450-line file. Separated, no component `.ts`
exceeds ~170 lines, templates get proper HTML tooling, and review diffs show
markup changes apart from logic changes. It costs nothing at runtime — the
compiler inlines templates into the bundle either way.

**Grouped by feature rather than by type** (`features/employees/…` rather than
`components/`, `services/`, `models/`). Angular's style guide recommends
folders-by-feature, and the practical argument is that a change to the salary
timeline touches its component, template, styles and test together — under
folders-by-type those four files sit in four different places. The two ideas are
independent: per-component folders are what the CLI generates, and they nest
inside feature folders unchanged. `core/` and `shared/` remain type-oriented
because their contents genuinely are cross-cutting.

Styles are plain CSS. Component styles are already scoped by Angular, theming
uses custom properties, and nothing needs mixins or `@extend`, so a preprocessor
would add a build step without buying anything.

Feature components are lazily loaded per route, so the login screen ships
without the dashboard or the employee tables.

The auth interceptor attaches the bearer token and, on a 401, signs the user out —
an expired session becomes a login screen rather than a wall of failed requests.

Charts are hand-written components (~120 lines each) rather than a charting
library; see [trade-offs.md](trade-offs.md#frontend).

## 6. Testing

```text
Unit          business rules, mocked repositories, fixed Clock
              30 tests, ~1.5s, no Docker or database
                        ↓
Integration   Flyway migrations · the exclusion constraint · the analytics SQL
              · the HTTP contract end to end including auth
              20 tests, real PostgreSQL via Testcontainers

Frontend      session and token expiry · auth interceptor · query-parameter
              construction · money formatting · error-message extraction ·
              the salary form's effective-date rule
              40 tests, headless Chrome, ~0.3s
```

The split is deliberate. Unit tests are where the rules are pinned down, because
they are fast enough to run constantly. Integration tests cover exactly what
cannot be verified any other way — a mocked repository has no exclusion
constraint, no flush ordering, and no `percentile_cont`.

The API test is intentionally *not* transactional: each request commits, as in
production, so rules that only hold inside one transaction fail there. A shared
base class truncates before each test to keep runs repeatable.

Frontend tests run zoneless, matching the application. Loading `zone.js` only in
tests would mean change detection behaves differently there than in production —
a test could pass because zone.js triggered a refresh the real app never would.

## 7. Deployment

```text
nginx (Angular, serves the app and proxies /api)
        │
Spring Boot (fat jar, non-root, health-checked)
        │
PostgreSQL 16 (named volume)
```

`docker-compose.yml` brings up all three. nginx proxies `/api` to the backend, so
the browser sees a single origin and CORS is not involved; the backend's CORS
configuration only matters if the two are deployed to different origins.

Both images are multi-stage: dependencies resolve in their own layer so a
source-only change does not re-download them. The backend runs as a non-root
user and exposes `/actuator/health` for readiness.

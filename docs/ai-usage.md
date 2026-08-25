# How AI was used

AI wrote most of the code. It did not decide what the code should do. This note
records how it was directed, what it got wrong, and what was verified by hand.

## Method

1. **Decide first, generate second.** Scope and the data model were written
   before any code and handed to the model as the specification. The choices
   with real consequences — half-open periods, database-enforced non-overlap,
   aggregation in SQL — were made deliberately and are recorded in
   [trade-offs.md](trade-offs.md).
2. **Generate, then read every line.** The review question was always *can I
   state the reason this line is here?* Several passes were re-specified rather
   than patched.
3. **Verify against something real.** The build, tests, migrations, analytics
   SQL and the live API were executed, not assumed. What could not be executed
   is marked as such below.

## Where it paid off

- Volume with a consistent house style: ~60 backend and ~60 frontend files.
- Enumerating edge cases — zero and negative amounts, today vs tomorrow,
  cross-currency medians, cancelling a historical record.
- API archaeology: Spring Boot 4 and Testcontainers 2 both renamed things.
- Turning decisions already made into readable prose.

## Where it was wrong

Each of these shipped looking correct. The third column is what actually caught
it — worth noting how rarely that column says "reading the code".

| What was wrong | Why it slipped through | Caught by |
| --- | --- | --- |
| `pom.xml` used Testcontainers 1.x artifact names; the Maven wrapper config was missing entirely | Artifact renames are invisible in source | Running the build |
| Two test files had never compiled — undefined variables, missing imports, wall-clock assertions in a codebase that injects a `Clock` | Plausible test code reads exactly like passing test code | Compiling |
| Cancelling a scheduled raise deleted the future period but left the previous one closed, leaving the employee **unpaid from that date** | Every rule was correct in isolation; nothing composed them | Tracing state transitions by hand |
| Every salary had to be future-dated, so someone hired today could not be paid | Faithful to the written spec, wrong for the user | Asking what HR does on a Monday morning |
| Login returned 500 — `NimbusJwtEncoder` defaults to RS256 and cannot select an HMAC key | A library default, invisible in the code | Starting the application |
| Scheduling a raise violated the exclusion constraint — Hibernate flushes inserts before updates | A mocked repository has no flush ordering | Integration tests on real PostgreSQL |
| Tokens carried `ROLE_FACTOR_PASSWORD` | Spring Security grants an authority describing *how* you authenticated | Reading the login response |
| The API test suite passed once, then failed — it commits deliberately and never cleaned up | The first run is always clean | Running it twice |
| Analytics converted currency *after* aggregating, so medians were wrong | Sums survive that; medians do not | Reasoning about the statistic |
| The Angular app had zero tests, and components sat flat in one folder instead of one folder each | It compiled and looked finished | A direct question about `.spec` files |

Two patterns run through these. Bugs live in the **seam between the code and a
real runtime** — a library default, an ORM's flush order, a framework's
authority model — which a model reasons about least well. And a model checks
whether code is *correct*, not whether it is *idiomatic* for the ecosystem it
lives in.

## What was verified

| Claim | How |
| --- | --- |
| Backend unit tests | `./mvnw test` — 30 tests, ~1.5s |
| Integration tests | `./mvnw verify` against PostgreSQL 16.4 — 20 tests, green |
| Migrations apply to an empty database | Flyway history table inspected after startup |
| Exclusion constraint prevents overlap | Integration test asserts the rejection |
| Seeder | 10,000 employees (9,229 active / 771 terminated), 41,477 compensation records, 10 countries, 10 departments, 10 currencies |
| Analytics correctness | Queried the seeded database: $926.9M payroll, $100,890 average, $85,338 median (below average, matching the right skew); EUR figures exactly 0.92× USD |
| Business rules over HTTP | Twelve-step lifecycle against the live API: first salary today, same-day change refused, negative amount refused, second pending change refused, termination blocked while pending, cancel restores coverage, current salary immutable, history survives termination, paying a leaver refused |
| Authentication enforced | Anonymous request → 401; token issued and accepted |
| Angular build | `npm run build`, Angular 20.3 — 78 kB initial transfer |
| Frontend tests | `npm run test:ci` — 40 tests, headless Chrome, ~0.1s |
| **Browser behaviour** | **Not verified in this session** — no browser automation was run |
| **Docker deployment** | **Not executed** — no Docker daemon available; wiring checked statically only |
| **Cloud deployment** | **Not executed** |

## Prompting, briefly

Stating invariants as constraints ("periods are `[from, to)`; the database
enforces non-overlap; history is immutable") produced code that respected them.
Stating them as goals did not.

Asking for "tests for the compensation service" produced a test per rule in
isolation — which is exactly why the cancellation bug survived. Asking for "a
test that follows one employee through hire, raise, cancellation and
termination, asserting the timeline stays continuous" found it. A model tests
the code it wrote against the understanding it used to write it; it will not
independently question that understanding.

## Decided by hand, not delegated

Scope and exclusions · the compensation data model and half-open periods ·
enforcing non-overlap in the database · reporting in one currency and treating a
missing rate as an error · surfacing employees with no salary · the auth model
and token lifetime · no charting library · everything in
[trade-offs.md](trade-offs.md).

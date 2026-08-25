# Trade-offs

Every decision below had a defensible alternative. Recorded here: what was
chosen, what it costs, and what would make us revisit it.

## Architecture

| Decision | Why | Cost |
| --- | --- | --- |
| **Modular monolith**, not microservices | 10,000 employees, one team, one workflow. Package boundaries already sit where service boundaries would go. | Modules are not independently deployable; only code review stops a cross-boundary call. |
| **UUID keys**, not sequential longs | Employee ids appear in URLs HR can share. Sequential ids leak headcount and invite enumeration. | 16 bytes and random insert order. UUIDv7 if insert throughput ever matters. |

## Compensation model

| Decision | Why | Cost |
| --- | --- | --- |
| **One amount + currency**, no pay components | Bonus accrual, equity vesting and country-specific rules would dominate the codebase without improving the core workflow. | Cannot answer base-vs-bonus or equity questions. |
| **Half-open periods** `[effectiveFrom, effectiveTo)` | Inclusive end dates turn every adjacency into an off-by-one, which is where date bugs live. Maps directly onto `daterange(..., '[)')`. | Reads oddly: a period ending 1 Sep was not in effect on 1 Sep. |
| **Overlap enforced by the database**, via a GiST exclusion constraint | A service check is read-then-write and races under concurrency; the constraint does not. | Ties the schema to PostgreSQL; integration tests need a real instance. |
| **Row lock on salary changes and termination** | "Only one scheduled change" is read-then-decide — two requests could both read "none scheduled". | Changes to the *same* employee serialise. Different employees never contend. |
| **Close a period before opening the next** (explicit `flush()`) | Hibernate flushes inserts before updates, so the two rows would briefly overlap and the constraint would reject the transaction. | Every future write to the timeline must remember the ordering. |
| **One pending change per employee** | A chain of future changes needs rules for editing and cancelling the middle of it. The data model already allows more. | A two-step plan must be entered in two sittings. |
| **First salary may start today; changes must be future-dated** | The reason to forbid back-dating — the period is already paid — does not apply when there is no period. | Two rules instead of one. |
| **Cancelling re-opens the previous period** | Otherwise cancelling silently leaves the employee unpaid from that date onward. | The delete must be flushed before the update. |

## Analytics

| Decision | Why | Cost |
| --- | --- | --- |
| **Aggregate in the database** | Returns one row per reported group, never one per employee, so cost does not grow with headcount. | Logic lives in SQL rather than Java. |
| **Convert currency inside the query**, before aggregating | A median of per-currency medians is not the organisation's median. | The query is PostgreSQL-specific and denser than converting in Java. |
| **Static rates behind `ExchangeRateProvider`** | Analytics must be reproducible, and tests must not depend on a third party being up. | Rates drift; historical figures use today's rates, not the day's. |
| **A missing rate is an error, not a zero** | Dropping those rows produces a smaller payroll that still looks entirely plausible. | One unconvertible currency blocks the whole dashboard. |
| **Count employees without a salary separately** | An active employee with no pay on record is a data-quality problem HR should see, not average away. | One more number on an already dense dashboard. |

## Security

| Decision | Why | Cost |
| --- | --- | --- |
| **Stateless JWT, accounts in configuration** | One persona and a few named accounts. A user table brings invitations, resets and lockout policy — an identity platform, not a salary product. | No revocation before expiry; adding a user needs a restart. Lifetime kept to 8 hours. |
| **Token in `localStorage`** | An httpOnly cookie makes the API stateful and needs CSRF on every write. No third-party scripts load, and the token is short-lived. | Readable by script, so a successful XSS could exfiltrate it. |

## Frontend

| Decision | Why | Cost |
| --- | --- | --- |
| **No charting library** | Two chart shapes are needed. A library adds hundreds of kilobytes plus its own theming and accessibility model to reconcile. | No zooming, tooltips or animated transitions. |
| **Separate `.ts` / `.html` / `.css` per component** | The CLI default: templates get real HTML tooling and diffs show markup apart from logic. | Three files per component. |
| **Plain CSS, not SCSS** | Component styles are already scoped, theming uses custom properties, and nothing needs mixins. | None at this size; one line in `angular.json` to switch. |

## Testing and tooling

| Decision | Why | Cost |
| --- | --- | --- |
| **Seed via JDBC, not the service layer** | The services reject back-dated salaries, and realistic seed data *is* years of back-dated history. | The seeder can create data the API would refuse. It is opt-in and refuses a non-empty table. |
| **Integration tests skip when Docker is absent** | A build that fails on every Docker-less machine buries real failures in noise. | A green `mvn verify` does not by itself prove they ran — the skip count makes the gap visible. |

## Deliberately out of scope

| Excluded | Reason |
| --- | --- |
| Excel/CSV import | The highest-value next feature, and a project in itself: template validation, row-level errors, partial failures, batch transactions. Seed data covers the need for 10,000 employees. |
| Payroll processing | Tax, deductions, proration and payslips are a separate, country-specific domain. This product manages compensation data; it does not run payroll. |
| Employee self-service | The persona is the HR Manager. Employee access needs its own authorisation model and a very different UI. |
| Role hierarchy | One role, `HR_MANAGER`. The authorisation boundary exists and is enforced; a hierarchy can grow into it. |
| Approval workflows | A change taking effect on save matches how a small HR team works. Multi-step approval is organisational policy, not a data model. |
| Notifications | Nothing in the workflow waits on an email. |
| Audit log | Salary history is itself an audit trail of what changed and when. A full actor/timestamp log is the next step once more than one person has write access. |
| Live FX integration | Covered above: reproducibility and test independence. The provider interface is the seam for adding it. |

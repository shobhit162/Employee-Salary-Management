package com.acme.employeemanagement.seed;

import com.acme.employeemanagement.employee.EmploymentStatus;
import com.acme.employeemanagement.seed.OrganisationProfile.Country;
import com.acme.employeemanagement.seed.OrganisationProfile.Department;
import com.acme.employeemanagement.seed.OrganisationProfile.Level;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.UUID;

/**
 * Populates a fresh database with a realistic ACME-sized organisation.
 *
 * <p>Runs only when {@code app.seed.enabled=true} and only against an empty
 * employees table, so it can never overwrite real data. Everything derives from
 * a fixed random seed, so two runs produce identical data and a screenshot taken
 * today still matches the database tomorrow.
 *
 * <p>Rows are written with batched JDBC rather than through the services: the
 * services deliberately refuse back-dated salaries, and seeding needs exactly
 * that — years of salary history behind each employee.
 */
@Component
@ConditionalOnProperty(prefix = "app.seed", name = "enabled", havingValue = "true")
public class SeedDataRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(SeedDataRunner.class);

    private static final int BATCH_SIZE = 1_000;

    /** Share of the organisation that has left. */
    private static final double TERMINATED_RATE = 0.08;

    /** Share of active employees with a raise already scheduled. */
    private static final double SCHEDULED_RAISE_RATE = 0.03;

    /**
     * A deliberate handful of active employees have no salary on record. They
     * make the dashboard's "missing salary" figure meaningful instead of always
     * being a reassuring zero.
     */
    private static final double MISSING_SALARY_RATE = 0.004;

    private static final String INSERT_EMPLOYEE = """
            insert into employees (
                id, employee_code, first_name, last_name, email, country_code,
                department, job_title, employment_status, termination_date,
                created_at, updated_at
            ) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;

    private static final String INSERT_COMPENSATION = """
            insert into compensations (
                id, employee_id, amount, currency, effective_from, effective_to,
                created_at
            ) values (?, ?, ?, ?, ?, ?, ?)
            """;

    private final JdbcTemplate jdbcTemplate;
    private final SeedProperties seedProperties;
    private final Clock clock;

    public SeedDataRunner(
            JdbcTemplate jdbcTemplate,
            SeedProperties seedProperties,
            Clock clock
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.seedProperties = seedProperties;
        this.clock = clock;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        Integer existing = jdbcTemplate.queryForObject(
                "select count(*) from employees",
                Integer.class
        );

        if (existing != null && existing > 0) {
            log.info(
                    "Seed skipped: employees table already holds {} rows",
                    existing
            );
            return;
        }

        long startedAt = System.nanoTime();
        LocalDate today = LocalDate.now(clock);
        Random random = new Random(seedProperties.getRandomSeed());

        List<Object[]> employeeRows = new ArrayList<>(BATCH_SIZE);
        List<Object[]> compensationRows = new ArrayList<>(BATCH_SIZE * 2);

        int employeeCount = seedProperties.getEmployeeCount();
        int terminatedCount = 0;
        int compensationCount = 0;

        for (int index = 1; index <= employeeCount; index++) {
            Country country = weighted(
                    OrganisationProfile.COUNTRIES,
                    Country::headcountWeight,
                    random
            );
            Department department = weighted(
                    OrganisationProfile.DEPARTMENTS,
                    Department::headcountWeight,
                    random
            );
            Level level = weighted(
                    OrganisationProfile.LEVELS,
                    Level::weight,
                    random
            );

            String firstName = pick(OrganisationProfile.FIRST_NAMES, random);
            String lastName = pick(OrganisationProfile.LAST_NAMES, random);
            String employeeCode = "ACME-%05d".formatted(index);
            String email = "%s.%s.%d@acme.com".formatted(
                    firstName.toLowerCase(Locale.ROOT),
                    lastName.toLowerCase(Locale.ROOT),
                    index
            );

            // Tenure drives both the hire date and how many raises they have had.
            int tenureDays = 60 + random.nextInt(8 * 365);
            LocalDate hiredOn = today.minusDays(tenureDays);

            boolean terminated = random.nextDouble() < TERMINATED_RATE;
            LocalDate terminationDate = null;

            if (terminated) {
                // Somewhere between joining and today, never on the hire date.
                int servedDays = 30 + random.nextInt(Math.max(tenureDays - 30, 1));
                terminationDate = hiredOn.plusDays(servedDays);

                if (!terminationDate.isBefore(today)) {
                    terminationDate = today.minusDays(1);
                }

                terminatedCount++;
            }

            UUID employeeId = deterministicId(random);
            OffsetDateTime createdAt = hiredOn.atStartOfDay(ZoneOffset.UTC)
                    .toOffsetDateTime();

            employeeRows.add(new Object[]{
                    employeeId,
                    employeeCode,
                    firstName,
                    lastName,
                    email,
                    country.code(),
                    department.name(),
                    pick(department.jobTitles(), random),
                    terminated
                            ? EmploymentStatus.TERMINATED.name()
                            : EmploymentStatus.ACTIVE.name(),
                    terminationDate == null ? null : java.sql.Date.valueOf(terminationDate),
                    Timestamp.from(createdAt.toInstant()),
                    Timestamp.from(createdAt.toInstant())
            });

            boolean withoutSalary = !terminated
                    && random.nextDouble() < MISSING_SALARY_RATE;

            if (!withoutSalary) {
                compensationCount += appendSalaryHistory(
                        compensationRows,
                        random,
                        employeeId,
                        country,
                        department,
                        level,
                        hiredOn,
                        terminated ? terminationDate : today,
                        !terminated && random.nextDouble() < SCHEDULED_RAISE_RATE
                                ? today.plusDays(15 + random.nextInt(75))
                                : null
                );
            }

            if (employeeRows.size() >= BATCH_SIZE) {
                flush(employeeRows, compensationRows);
            }
        }

        flush(employeeRows, compensationRows);

        log.info(
                "Seeded {} employees ({} terminated) and {} compensation records in {} ms",
                employeeCount,
                terminatedCount,
                compensationCount,
                (System.nanoTime() - startedAt) / 1_000_000
        );
    }

    /**
     * Builds a non-overlapping salary timeline: a starting salary on the hire
     * date, then a raise roughly once a year, and optionally a pending raise.
     *
     * @param openUntil the day the timeline stops growing — today for an active
     *                  employee, the termination date for someone who has left
     * @return how many rows were appended
     */
    private int appendSalaryHistory(
            List<Object[]> rows,
            Random random,
            UUID employeeId,
            Country country,
            Department department,
            Level level,
            LocalDate hiredOn,
            LocalDate openUntil,
            LocalDate scheduledRaiseOn
    ) {
        BigDecimal salary = startingSalary(random, country, department, level);

        List<LocalDate> boundaries = new ArrayList<>();
        boundaries.add(hiredOn);

        LocalDate next = hiredOn.plusDays(300 + random.nextInt(200));
        while (next.isBefore(openUntil)) {
            boundaries.add(next);
            next = next.plusDays(300 + random.nextInt(200));
        }

        if (scheduledRaiseOn != null) {
            boundaries.add(scheduledRaiseOn);
        }

        int written = 0;

        for (int i = 0; i < boundaries.size(); i++) {
            if (i > 0) {
                salary = raise(salary, random);
            }

            LocalDate from = boundaries.get(i);
            LocalDate to = i + 1 < boundaries.size()
                    ? boundaries.get(i + 1)
                    : null;

            rows.add(new Object[]{
                    deterministicId(random),
                    employeeId,
                    salary,
                    country.currency(),
                    java.sql.Date.valueOf(from),
                    to == null ? null : java.sql.Date.valueOf(to),
                    Timestamp.from(
                            from.atStartOfDay(ZoneOffset.UTC).toInstant()
                    )
            });

            written++;
        }

        return written;
    }

    private BigDecimal startingSalary(
            Random random,
            Country country,
            Department department,
            Level level
    ) {
        // Vary within the level's band, then apply the department's pay factor
        // and the country's local pay level.
        double variation = 1 + (random.nextDouble() * 2 - 1) * level.spread();

        return level.baseSalary()
                .multiply(BigDecimal.valueOf(variation))
                .multiply(BigDecimal.valueOf(department.payFactor()))
                .multiply(country.salaryMultiplier())
                .setScale(2, RoundingMode.HALF_UP);
    }

    /** Between 3% and 12%, the range a normal review cycle produces. */
    private BigDecimal raise(BigDecimal salary, Random random) {
        double factor = 1.03 + random.nextDouble() * 0.09;

        return salary
                .multiply(BigDecimal.valueOf(factor))
                .setScale(2, RoundingMode.HALF_UP);
    }

    private void flush(
            List<Object[]> employeeRows,
            List<Object[]> compensationRows
    ) {
        if (!employeeRows.isEmpty()) {
            jdbcTemplate.batchUpdate(INSERT_EMPLOYEE, employeeRows);
            employeeRows.clear();
        }

        if (!compensationRows.isEmpty()) {
            jdbcTemplate.batchUpdate(INSERT_COMPENSATION, compensationRows);
            compensationRows.clear();
        }
    }

    private static UUID deterministicId(Random random) {
        return new UUID(random.nextLong(), random.nextLong());
    }

    private static <T> T pick(List<T> options, Random random) {
        return options.get(random.nextInt(options.size()));
    }

    private static <T> T weighted(
            List<T> options,
            java.util.function.ToIntFunction<T> weight,
            Random random
    ) {
        int total = options.stream().mapToInt(weight).sum();
        int target = random.nextInt(total);

        for (T option : options) {
            target -= weight.applyAsInt(option);
            if (target < 0) {
                return option;
            }
        }

        return options.getLast();
    }
}

package com.acme.employeemanagement.analytics;

import com.acme.employeemanagement.analytics.dto.SalaryBreakdownResponse.SalaryBreakdownRow;
import com.acme.employeemanagement.analytics.dto.SalaryDistributionResponse.SalaryBand;
import com.acme.employeemanagement.analytics.dto.SalaryStatistics;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Compensation aggregation, executed entirely in PostgreSQL.
 *
 * <p>With 10,000 employees it would still be possible to pull every salary into
 * the JVM and aggregate there, but that cost grows linearly with headcount and
 * the database already does this work well. Every query here therefore returns
 * one row per reported group, never one row per employee.
 *
 * <p>Cross-currency reporting is handled by injecting the exchange-rate table
 * into the query as two parallel arrays, so conversion happens before
 * aggregation. That matters for the median in particular: a median of
 * per-currency medians is not the median of the organisation.
 */
@Repository
public class AnalyticsRepository {

    /**
     * Employees in scope joined to the salary effective on the reporting date,
     * already converted into the reporting currency.
     *
     * <p>The join to compensations is a LEFT JOIN on purpose: an employee with no
     * salary must still be counted, otherwise a missing salary would quietly
     * shrink headcount instead of showing up as a gap.
     *
     * <p>Nullable bind parameters are cast explicitly because PostgreSQL cannot
     * infer the type of an untyped NULL in {@code :param is null} comparisons.
     */
    private static final String SALARY_SCOPE = """
            with fx as (
                select t.currency, t.rate
                from unnest(
                    string_to_array(:currencies, ','),
                    string_to_array(:rates, ',')::numeric[]
                ) as t(currency, rate)
            ),
            salary as (
                select e.id            as employee_id,
                       e.country_code  as country_code,
                       e.department    as department,
                       c.amount * fx.rate as amount
                from employees e
                left join compensations c
                       on c.employee_id = e.id
                      and c.effective_from <= :asOf
                      and (c.effective_to is null or c.effective_to > :asOf)
                left join fx
                       on fx.currency = c.currency
                where (cast(:status as varchar) is null
                       or e.employment_status = cast(:status as varchar))
                  and (cast(:countryCode as varchar) is null
                       or e.country_code = cast(:countryCode as varchar))
                  and (cast(:department as varchar) is null
                       or lower(e.department) = lower(cast(:department as varchar)))
            )
            """;

    private static final String STATISTICS_COLUMNS = """
            count(*)                        as employee_count,
            count(amount)                   as compensated_count,
            coalesce(sum(amount), 0)        as total,
            avg(amount)                     as average,
            percentile_cont(0.5) within group (
                order by amount::double precision
            )                               as median,
            min(amount)                     as minimum,
            max(amount)                     as maximum
            """;

    private final JdbcClient jdbcClient;

    public AnalyticsRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    public SalaryStatistics summarise(
            AnalyticsFilter filter,
            Map<String, BigDecimal> ratesToReportingCurrency
    ) {
        return bind(
                SALARY_SCOPE + "select " + STATISTICS_COLUMNS + " from salary",
                filter,
                ratesToReportingCurrency
        )
                .query(AnalyticsRepository::readStatistics)
                .single();
    }

    public List<SalaryBreakdownRow> breakdown(
            AnalyticsFilter filter,
            BreakdownDimension dimension,
            Map<String, BigDecimal> ratesToReportingCurrency
    ) {
        String sql = SALARY_SCOPE + """
                select %s as group_key,
                       %s
                from salary
                group by group_key
                order by total desc, group_key asc
                """.formatted(dimension.column(), STATISTICS_COLUMNS);

        return bind(sql, filter, ratesToReportingCurrency)
                .query((rs, rowNum) -> new SalaryBreakdownRow(
                        rs.getString("group_key"),
                        readStatistics(rs, rowNum)
                ))
                .list();
    }

    /**
     * @param maxBandIndex salaries at or above {@code maxBandIndex * bandSize}
     *                     are collected into a single open-ended top band, which
     *                     bounds the response size regardless of outliers
     */
    public List<SalaryBand> distribution(
            AnalyticsFilter filter,
            Map<String, BigDecimal> ratesToReportingCurrency,
            BigDecimal bandSize,
            int maxBandIndex
    ) {
        String sql = SALARY_SCOPE + """
                select least(
                           floor(amount / :bandSize),
                           cast(:maxBandIndex as numeric)
                       )::int         as band_index,
                       count(*)       as employee_count
                from salary
                where amount is not null
                group by band_index
                order by band_index
                """;

        return bind(sql, filter, ratesToReportingCurrency)
                .param("bandSize", bandSize)
                .param("maxBandIndex", maxBandIndex)
                .query((rs, rowNum) -> {
                    int bandIndex = rs.getInt("band_index");
                    BigDecimal lower = bandSize.multiply(
                            BigDecimal.valueOf(bandIndex)
                    );

                    return new SalaryBand(
                            lower,
                            bandIndex == maxBandIndex ? null : lower.add(bandSize),
                            rs.getLong("employee_count")
                    );
                })
                .list();
    }

    private JdbcClient.StatementSpec bind(
            String sql,
            AnalyticsFilter filter,
            Map<String, BigDecimal> rates
    ) {
        // Ordering matters: the two arrays are zipped positionally by unnest.
        List<Map.Entry<String, BigDecimal>> orderedRates = rates.entrySet()
                .stream()
                .sorted(Map.Entry.comparingByKey())
                .toList();

        return jdbcClient.sql(sql)
                .param("currencies", orderedRates.stream()
                        .map(Map.Entry::getKey)
                        .collect(Collectors.joining(",")))
                .param("rates", orderedRates.stream()
                        .map(entry -> entry.getValue().toPlainString())
                        .collect(Collectors.joining(",")))
                .param("asOf", filter.asOf())
                .param("status", filter.status() == null
                        ? null
                        : filter.status().name())
                .param("countryCode", filter.countryCode())
                .param("department", filter.department());
    }

    private static SalaryStatistics readStatistics(ResultSet rs, int rowNum)
            throws SQLException {
        return new SalaryStatistics(
                rs.getLong("employee_count"),
                rs.getLong("compensated_count"),
                money(rs.getBigDecimal("total")),
                money(rs.getBigDecimal("average")),
                money(rs.getBigDecimal("median")),
                money(rs.getBigDecimal("minimum")),
                money(rs.getBigDecimal("maximum"))
        );
    }

    private static BigDecimal money(BigDecimal value) {
        return value == null ? null : value.setScale(2, RoundingMode.HALF_UP);
    }
}

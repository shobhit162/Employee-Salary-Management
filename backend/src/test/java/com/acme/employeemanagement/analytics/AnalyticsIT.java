package com.acme.employeemanagement.analytics;

import com.acme.employeemanagement.analytics.dto.SalaryBreakdownResponse;
import com.acme.employeemanagement.analytics.dto.SalaryDistributionResponse.SalaryBand;
import com.acme.employeemanagement.analytics.dto.SalaryStatistics;
import com.acme.employeemanagement.common.exception.BusinessRuleViolationException;
import com.acme.employeemanagement.employee.Employee;
import com.acme.employeemanagement.employee.EmploymentStatus;
import com.acme.employeemanagement.support.IntegrationTest;
import com.acme.employeemanagement.support.TestData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Verifies the aggregation SQL against real PostgreSQL.
 *
 * <p>The rates used here are configured to round numbers in
 * {@code application-integration-test.properties} (EUR 0.5, GBP 0.25, INR 100 per
 * USD) so every converted figure below is exact:
 *
 * <pre>
 *   US  Engineering  ACTIVE      120,000 USD  ->  120,000
 *   US  Engineering  ACTIVE       80,000 USD  ->   80,000
 *   GB  Engineering  ACTIVE       25,000 GBP  ->  100,000
 *   IN  Support      ACTIVE    2,000,000 INR  ->   20,000
 *   US  Support      TERMINATED  200,000 USD  ->  excluded by default
 *   DE  Product      ACTIVE      (no salary)  ->  counted, not compensated
 * </pre>
 */
@Transactional
class AnalyticsIT extends IntegrationTest {

    private static final LocalDate AS_OF = LocalDate.of(2026, 6, 30);
    private static final LocalDate STARTED = AS_OF.minusYears(1);

    @Autowired
    private AnalyticsService analyticsService;

    @Autowired
    private TestData testData;

    @BeforeEach
    void seedOrganisation() {
        Employee usHigh = testData.employee("US", "Engineering");
        testData.salary(usHigh, "120000", "USD", STARTED);

        Employee usLow = testData.employee("US", "Engineering");
        testData.salary(usLow, "80000", "USD", STARTED);

        Employee uk = testData.employee("GB", "Engineering");
        testData.salary(uk, "25000", "GBP", STARTED);

        Employee india = testData.employee("IN", "Customer Support");
        testData.salary(india, "2000000", "INR", STARTED);

        Employee leaver = testData.terminatedEmployee("US", "Customer Support");
        testData.salary(leaver, "200000", "USD", STARTED);

        testData.employee("DE", "Product");
    }

    @Test
    @DisplayName("KPIs cover active employees and convert every currency")
    void summarisesActivePayroll() {
        SalaryStatistics statistics = analyticsService
                .summary(filter(EmploymentStatus.ACTIVE, null, null, "USD"))
                .statistics();

        assertThat(statistics.employeeCount())
                .as("the employee with no salary is still an employee")
                .isEqualTo(5);
        assertThat(statistics.compensatedEmployeeCount()).isEqualTo(4);

        // 120,000 + 80,000 + 100,000 + 20,000
        assertThat(statistics.totalAnnualCompensation())
                .isEqualByComparingTo("320000");
        assertThat(statistics.average()).isEqualByComparingTo("80000");
        // Middle two of 20,000 / 80,000 / 100,000 / 120,000
        assertThat(statistics.median()).isEqualByComparingTo("90000");
        assertThat(statistics.minimum()).isEqualByComparingTo("20000");
        assertThat(statistics.maximum()).isEqualByComparingTo("120000");
    }

    @Test
    @DisplayName("terminated employees are excluded from current pay by default")
    void excludesLeaversUnlessAsked() {
        SalaryStatistics activeOnly = analyticsService
                .summary(filter(EmploymentStatus.ACTIVE, null, null, "USD"))
                .statistics();

        SalaryStatistics everyone = analyticsService
                .summary(filter(null, null, null, "USD"))
                .statistics();

        assertThat(activeOnly.employeeCount()).isEqualTo(5);
        assertThat(everyone.employeeCount()).isEqualTo(6);
        assertThat(everyone.totalAnnualCompensation())
                .isEqualByComparingTo("520000");
    }

    @Test
    @DisplayName("reporting in another currency rescales every figure")
    void reportsInARequestedCurrency() {
        SalaryStatistics inEuro = analyticsService
                .summary(filter(EmploymentStatus.ACTIVE, null, null, "EUR"))
                .statistics();

        // Every amount halves: 1 USD = 0.5 EUR under the test rates.
        assertThat(inEuro.totalAnnualCompensation())
                .isEqualByComparingTo("160000");
        assertThat(inEuro.maximum()).isEqualByComparingTo("60000");
    }

    @Test
    @DisplayName("a country breakdown ranks by total payroll")
    void breaksDownByCountry() {
        SalaryBreakdownResponse breakdown = analyticsService.breakdown(
                filter(EmploymentStatus.ACTIVE, null, null, "USD"),
                BreakdownDimension.COUNTRY
        );

        assertThat(breakdown.rows())
                .extracting(SalaryBreakdownResponse.SalaryBreakdownRow::key)
                .containsExactly("US", "GB", "IN", "DE");

        SalaryBreakdownResponse.SalaryBreakdownRow us = breakdown.rows().getFirst();
        assertThat(us.statistics().employeeCount()).isEqualTo(2);
        assertThat(us.statistics().totalAnnualCompensation())
                .isEqualByComparingTo("200000");
        assertThat(us.statistics().average()).isEqualByComparingTo("100000");

        SalaryBreakdownResponse.SalaryBreakdownRow germany = breakdown.rows()
                .getLast();
        assertThat(germany.key()).isEqualTo("DE");
        assertThat(germany.statistics().compensatedEmployeeCount()).isZero();
        assertThat(germany.statistics().average()).isNull();
    }

    @Test
    @DisplayName("filters narrow the cohort before aggregating")
    void appliesFilters() {
        SalaryStatistics engineering = analyticsService
                .summary(filter(
                        EmploymentStatus.ACTIVE,
                        null,
                        "Engineering",
                        "USD"
                ))
                .statistics();

        assertThat(engineering.employeeCount()).isEqualTo(3);
        assertThat(engineering.totalAnnualCompensation())
                .isEqualByComparingTo("300000");

        SalaryStatistics unitedStates = analyticsService
                .summary(filter(EmploymentStatus.ACTIVE, "US", null, "USD"))
                .statistics();

        assertThat(unitedStates.employeeCount()).isEqualTo(2);
    }

    @Test
    @DisplayName("the distribution buckets salaries into equal-width bands")
    void distributesSalariesAcrossBands() {
        List<SalaryBand> bands = analyticsService.distribution(
                filter(EmploymentStatus.ACTIVE, null, null, "USD"),
                new BigDecimal("50000")
        ).bands();

        assertThat(bands).hasSize(3);

        assertThat(bands.get(0).lowerBound()).isEqualByComparingTo("0");
        assertThat(bands.get(0).employeeCount())
                .as("20,000")
                .isEqualTo(1);

        assertThat(bands.get(1).lowerBound()).isEqualByComparingTo("50000");
        assertThat(bands.get(1).employeeCount())
                .as("80,000")
                .isEqualTo(1);

        assertThat(bands.get(2).lowerBound()).isEqualByComparingTo("100000");
        assertThat(bands.get(2).upperBound()).isEqualByComparingTo("150000");
        assertThat(bands.get(2).employeeCount())
                .as("100,000 and 120,000")
                .isEqualTo(2);
    }

    @Test
    @DisplayName("an unconvertible currency is refused, not silently dropped")
    void refusesToReportWithoutARate() {
        Employee employee = testData.employee("JP", "Engineering");
        testData.salary(employee, "9000000", "JPY", STARTED);

        assertThatThrownBy(() -> analyticsService
                .summary(filter(EmploymentStatus.ACTIVE, null, null, "USD")))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("JPY");
    }

    @Test
    @DisplayName("a salary that has not started yet is not counted as current")
    void readsSalariesAsOfTheReportingDate() {
        SalaryStatistics beforeAnyoneJoined = analyticsService
                .summary(new AnalyticsFilter(
                        STARTED.minusDays(1),
                        EmploymentStatus.ACTIVE,
                        null,
                        null,
                        "USD"
                ))
                .statistics();

        assertThat(beforeAnyoneJoined.compensatedEmployeeCount()).isZero();
        assertThat(beforeAnyoneJoined.totalAnnualCompensation())
                .isEqualByComparingTo("0");
    }

    private static AnalyticsFilter filter(
            EmploymentStatus status,
            String countryCode,
            String department,
            String currency
    ) {
        return new AnalyticsFilter(AS_OF, status, countryCode, department, currency);
    }
}

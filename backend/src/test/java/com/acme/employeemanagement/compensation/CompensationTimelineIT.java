package com.acme.employeemanagement.compensation;

import com.acme.employeemanagement.compensation.dto.CompensationResponse;
import com.acme.employeemanagement.compensation.dto.CompensationSummaryResponse;
import com.acme.employeemanagement.compensation.dto.CreateCompensationRequest;
import com.acme.employeemanagement.employee.Employee;
import com.acme.employeemanagement.support.IntegrationTest;
import com.acme.employeemanagement.support.TestData;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Exercises the salary timeline against a real PostgreSQL instance.
 *
 * <p>Two things here cannot be covered by unit tests: the exclusion constraint
 * that stops periods overlapping, and the fact that cancelling a scheduled
 * change must delete the future row before re-opening the previous one — get the
 * order wrong and the database rejects the whole transaction.
 */
@Transactional
class CompensationTimelineIT extends IntegrationTest {

    @Autowired
    private CompensationService compensationService;

    @Autowired
    private CompensationRepository compensationRepository;

    @Autowired
    private TestData testData;

    @Test
    @DisplayName("a salary change closes the current period and opens the new one")
    void schedulesASalaryChange() {
        Employee employee = testData.employee("US", "Engineering");
        LocalDate today = LocalDate.now();
        testData.salary(employee, "100000", "USD", today.minusYears(1));

        LocalDate raiseOn = today.plusDays(30);

        compensationService.create(
                employee.getId(),
                new CreateCompensationRequest(
                        new BigDecimal("120000"),
                        "USD",
                        raiseOn
                )
        );

        CompensationSummaryResponse summary =
                compensationService.getSummary(employee.getId());

        assertThat(summary.current().amount()).isEqualByComparingTo("100000");
        assertThat(summary.current().effectiveTo()).isEqualTo(raiseOn);
        assertThat(summary.scheduled().amount()).isEqualByComparingTo("120000");
        assertThat(summary.scheduled().effectiveTo()).isNull();
        assertThat(summary.history()).hasSize(2);
    }

    @Test
    @DisplayName("cancelling a scheduled change leaves the salary continuous")
    void cancellingRestoresTheOpenEndedPeriod() {
        Employee employee = testData.employee("US", "Engineering");
        LocalDate today = LocalDate.now();
        testData.salary(employee, "100000", "USD", today.minusYears(1));

        CompensationResponse scheduled = compensationService.create(
                employee.getId(),
                new CreateCompensationRequest(
                        new BigDecimal("120000"),
                        "USD",
                        today.plusDays(30)
                )
        );

        compensationService.cancelScheduledCompensation(
                employee.getId(),
                scheduled.id()
        );

        CompensationSummaryResponse summary =
                compensationService.getSummary(employee.getId());

        assertThat(summary.scheduled()).isNull();
        assertThat(summary.current().amount()).isEqualByComparingTo("100000");
        assertThat(summary.current().effectiveTo())
                .as("the employee must not be left without a salary")
                .isNull();
        assertThat(summary.history()).hasSize(1);
    }

    @Test
    @DisplayName("the database refuses two salaries covering the same day")
    void rejectsOverlappingPeriods() {
        Employee employee = testData.employee("US", "Engineering");
        LocalDate today = LocalDate.now();

        testData.salary(
                employee,
                "100000",
                "USD",
                today.minusYears(2),
                today.plusYears(1)
        );

        assertThatThrownBy(() -> {
            testData.salary(employee, "120000", "USD", today.minusMonths(6));
            compensationRepository.flush();
        }).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("periods may touch at a boundary date without overlapping")
    void allowsAdjacentPeriods() {
        Employee employee = testData.employee("US", "Engineering");
        LocalDate boundary = LocalDate.now().minusMonths(6);

        testData.salary(
                employee,
                "100000",
                "USD",
                boundary.minusYears(1),
                boundary
        );
        testData.salary(employee, "120000", "USD", boundary);

        compensationRepository.flush();

        assertThat(compensationRepository.findHistory(employee.getId()))
                .hasSize(2);
    }
}

package com.acme.employeemanagement.compensation;

import com.acme.employeemanagement.common.exception.BusinessRuleViolationException;
import com.acme.employeemanagement.common.exception.ResourceNotFoundException;
import com.acme.employeemanagement.compensation.dto.CompensationSummaryResponse;
import com.acme.employeemanagement.compensation.dto.CreateCompensationRequest;
import com.acme.employeemanagement.employee.Employee;
import com.acme.employeemanagement.employee.EmployeeRepository;
import com.acme.employeemanagement.support.TestIds;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * The salary-timeline rules are the heart of the product, so they are covered
 * with fast unit tests against a fixed clock. "Today" is always 2026-08-24.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CompensationServiceTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 8, 24);
    private static final LocalDate YESTERDAY = TODAY.minusDays(1);
    private static final LocalDate NEXT_MONTH = LocalDate.of(2026, 9, 1);

    @Mock
    private CompensationRepository compensationRepository;

    @Mock
    private EmployeeRepository employeeRepository;

    private CompensationService compensationService;
    private UUID employeeId;
    private Employee employee;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(
                TODAY.atStartOfDay(ZoneOffset.UTC).toInstant(),
                ZoneOffset.UTC
        );

        compensationService = new CompensationService(
                compensationRepository,
                employeeRepository,
                clock,
                new CompensationMapper()
        );

        employeeId = UUID.randomUUID();
        employee = activeEmployee(employeeId);

        when(employeeRepository.findByIdForUpdate(employeeId))
                .thenReturn(Optional.of(employee));
        when(employeeRepository.existsById(employeeId))
                .thenReturn(true);
        when(compensationRepository.save(any(Compensation.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Nested
    @DisplayName("setting the first salary")
    class FirstSalary {

        @Test
        void takesEffectToday() {
            compensationService.create(
                    employeeId,
                    request("100000", "USD", TODAY)
            );

            Compensation saved = capturedSave();

            assertThat(saved.getAmount())
                    .isEqualByComparingTo("100000");
            assertThat(saved.getCurrency()).isEqualTo("USD");
            assertThat(saved.getEffectiveFrom()).isEqualTo(TODAY);
            assertThat(saved.isOpenEnded()).isTrue();
        }

        @Test
        void canBeScheduledForAFutureStartDate() {
            compensationService.create(
                    employeeId,
                    request("100000", "USD", NEXT_MONTH)
            );

            assertThat(capturedSave().getEffectiveFrom())
                    .isEqualTo(NEXT_MONTH);
        }

        @Test
        void cannotBeBackdated() {
            assertThatThrownBy(() -> compensationService.create(
                    employeeId,
                    request("100000", "USD", YESTERDAY)
            ))
                    .isInstanceOf(BusinessRuleViolationException.class)
                    .hasMessageContaining("cannot be backdated");

            verify(compensationRepository, never())
                    .save(any(Compensation.class));
        }
    }

    @Nested
    @DisplayName("changing an existing salary")
    class SalaryChange {

        @Test
        void closesTheCurrentPeriodOnTheDayTheNewOneStarts() {
            Compensation current = currentCompensation("100000", "USD");

            compensationService.create(
                    employeeId,
                    request("120000", "USD", NEXT_MONTH)
            );

            assertThat(current.getEffectiveTo())
                    .as("current period ends when the new one begins")
                    .isEqualTo(NEXT_MONTH);

            Compensation scheduled = capturedSave();
            assertThat(scheduled.getAmount()).isEqualByComparingTo("120000");
            assertThat(scheduled.getEffectiveFrom()).isEqualTo(NEXT_MONTH);
            assertThat(scheduled.isOpenEnded()).isTrue();
        }

        @Test
        void mustTakeEffectInTheFuture() {
            currentCompensation("100000", "USD");

            assertThatThrownBy(() -> compensationService.create(
                    employeeId,
                    request("120000", "USD", TODAY)
            ))
                    .isInstanceOf(BusinessRuleViolationException.class)
                    .hasMessageContaining("future date");
        }

        @Test
        void leavesTheCurrentPeriodUntouchedWhenRejected() {
            Compensation current = currentCompensation("100000", "USD");

            assertThatThrownBy(() -> compensationService.create(
                    employeeId,
                    request("120000", "USD", YESTERDAY)
            ))
                    .isInstanceOf(BusinessRuleViolationException.class);

            assertThat(current.isOpenEnded()).isTrue();
        }

        @Test
        void isRejectedWhenAChangeIsAlreadyScheduled() {
            currentCompensation("100000", "USD");

            when(compensationRepository
                    .existsByEmployeeIdAndEffectiveFromAfter(employeeId, TODAY))
                    .thenReturn(true);

            assertThatThrownBy(() -> compensationService.create(
                    employeeId,
                    request("120000", "USD", NEXT_MONTH)
            ))
                    .isInstanceOf(BusinessRuleViolationException.class)
                    .hasMessageContaining("already has a scheduled");
        }
    }

    @Nested
    @DisplayName("validation")
    class Validation {

        @Test
        void rejectsZeroAmount() {
            assertThatThrownBy(() -> compensationService.create(
                    employeeId,
                    request("0", "USD", NEXT_MONTH)
            ))
                    .isInstanceOf(BusinessRuleViolationException.class)
                    .hasMessageContaining("greater than zero");
        }

        @Test
        void rejectsNegativeAmount() {
            assertThatThrownBy(() -> compensationService.create(
                    employeeId,
                    request("-1", "USD", NEXT_MONTH)
            ))
                    .isInstanceOf(BusinessRuleViolationException.class)
                    .hasMessageContaining("greater than zero");
        }

        @Test
        void rejectsMalformedCurrency() {
            assertThatThrownBy(() -> compensationService.create(
                    employeeId,
                    request("100000", "DOLLAR", NEXT_MONTH)
            ))
                    .isInstanceOf(BusinessRuleViolationException.class)
                    .hasMessageContaining("3-letter ISO currency code");
        }

        @Test
        void normalisesCurrencyToUpperCase() {
            compensationService.create(
                    employeeId,
                    request("100000", " eur ", NEXT_MONTH)
            );

            assertThat(capturedSave().getCurrency()).isEqualTo("EUR");
        }

        @Test
        void rejectsTerminatedEmployee() {
            employee.terminate(YESTERDAY);

            assertThatThrownBy(() -> compensationService.create(
                    employeeId,
                    request("100000", "USD", NEXT_MONTH)
            ))
                    .isInstanceOf(BusinessRuleViolationException.class)
                    .hasMessageContaining("terminated employee");
        }

        @Test
        void rejectsUnknownEmployee() {
            UUID unknownId = UUID.randomUUID();

            when(employeeRepository.findByIdForUpdate(unknownId))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> compensationService.create(
                    unknownId,
                    request("100000", "USD", NEXT_MONTH)
            ))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Employee not found");
        }
    }

    @Nested
    @DisplayName("cancelling a scheduled change")
    class Cancellation {

        @Test
        void deletesTheScheduledPeriodAndRestoresSalaryCoverage() {
            Compensation current = currentCompensation("100000", "USD");
            current.closeAt(NEXT_MONTH);

            Compensation scheduled = compensation("120000", "USD", NEXT_MONTH, null);
            UUID scheduledId = idOf(scheduled);

            when(compensationRepository.findById(scheduledId))
                    .thenReturn(Optional.of(scheduled));
            when(compensationRepository.findPeriodEndingOn(employeeId, NEXT_MONTH))
                    .thenReturn(Optional.of(current));

            compensationService.cancelScheduledCompensation(
                    employeeId,
                    scheduledId
            );

            verify(compensationRepository).delete(scheduled);
            assertThat(current.isOpenEnded())
                    .as("cancelling must not leave the employee without a salary")
                    .isTrue();
        }

        @Test
        void rejectsCancellingTheCurrentSalary() {
            Compensation current = currentCompensation("100000", "USD");
            UUID currentId = idOf(current);

            when(compensationRepository.findById(currentId))
                    .thenReturn(Optional.of(current));

            assertThatThrownBy(() ->
                    compensationService.cancelScheduledCompensation(
                            employeeId,
                            currentId
                    ))
                    .isInstanceOf(BusinessRuleViolationException.class)
                    .hasMessageContaining("immutable");

            verify(compensationRepository, never()).delete(any());
        }

        @Test
        void rejectsCancellingHistoricalCompensation() {
            Compensation historical = compensation(
                    "80000",
                    "USD",
                    LocalDate.of(2024, 1, 1),
                    LocalDate.of(2025, 1, 1)
            );
            UUID historicalId = idOf(historical);

            when(compensationRepository.findById(historicalId))
                    .thenReturn(Optional.of(historical));

            assertThatThrownBy(() ->
                    compensationService.cancelScheduledCompensation(
                            employeeId,
                            historicalId
                    ))
                    .isInstanceOf(BusinessRuleViolationException.class)
                    .hasMessageContaining("immutable");
        }

        @Test
        void rejectsCompensationBelongingToAnotherEmployee() {
            Employee other = activeEmployee(UUID.randomUUID());
            Compensation foreign = new Compensation(
                    other,
                    new BigDecimal("50000"),
                    "USD",
                    NEXT_MONTH,
                    null
            );
            UUID foreignId = idOf(foreign);

            when(compensationRepository.findById(foreignId))
                    .thenReturn(Optional.of(foreign));

            assertThatThrownBy(() ->
                    compensationService.cancelScheduledCompensation(
                            employeeId,
                            foreignId
                    ))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("salary summary")
    class Summary {

        @Test
        void reportsCurrentScheduledAndHistory() {
            Compensation past = compensation(
                    "80000",
                    "USD",
                    LocalDate.of(2024, 1, 1),
                    LocalDate.of(2025, 1, 1)
            );
            Compensation current = compensation(
                    "100000",
                    "USD",
                    LocalDate.of(2025, 1, 1),
                    NEXT_MONTH
            );
            Compensation scheduled = compensation(
                    "120000",
                    "USD",
                    NEXT_MONTH,
                    null
            );

            when(compensationRepository
                    .findEffectiveCompensation(employeeId, TODAY))
                    .thenReturn(Optional.of(current));
            when(compensationRepository
                    .findScheduledCompensations(employeeId, TODAY))
                    .thenReturn(List.of(scheduled));
            when(compensationRepository.findHistory(employeeId))
                    .thenReturn(List.of(scheduled, current, past));

            CompensationSummaryResponse summary =
                    compensationService.getSummary(employeeId);

            assertThat(summary.current().amount())
                    .isEqualByComparingTo("100000");
            assertThat(summary.scheduled().amount())
                    .isEqualByComparingTo("120000");
            assertThat(summary.history()).hasSize(3);
        }

        @Test
        void reportsNullsForAnEmployeeWithoutSalary() {
            when(compensationRepository
                    .findEffectiveCompensation(employeeId, TODAY))
                    .thenReturn(Optional.empty());
            when(compensationRepository
                    .findScheduledCompensations(employeeId, TODAY))
                    .thenReturn(List.of());
            when(compensationRepository.findHistory(employeeId))
                    .thenReturn(List.of());

            CompensationSummaryResponse summary =
                    compensationService.getSummary(employeeId);

            assertThat(summary.current()).isNull();
            assertThat(summary.scheduled()).isNull();
            assertThat(summary.history()).isEmpty();
        }

        @Test
        void rejectsUnknownEmployee() {
            UUID unknownId = UUID.randomUUID();

            when(employeeRepository.existsById(unknownId)).thenReturn(false);

            assertThatThrownBy(() ->
                    compensationService.getSummary(unknownId))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    private CreateCompensationRequest request(
            String amount,
            String currency,
            LocalDate effectiveFrom
    ) {
        return new CreateCompensationRequest(
                new BigDecimal(amount),
                currency,
                effectiveFrom
        );
    }

    private Compensation currentCompensation(String amount, String currency) {
        Compensation current = compensation(
                amount,
                currency,
                LocalDate.of(2025, 1, 1),
                null
        );

        when(compensationRepository
                .findEffectiveCompensation(employeeId, TODAY))
                .thenReturn(Optional.of(current));

        return current;
    }

    private Compensation compensation(
            String amount,
            String currency,
            LocalDate from,
            LocalDate to
    ) {
        return new Compensation(
                employee,
                new BigDecimal(amount),
                currency,
                from,
                to
        );
    }

    private Compensation capturedSave() {
        ArgumentCaptor<Compensation> captor =
                ArgumentCaptor.forClass(Compensation.class);

        verify(compensationRepository).save(captor.capture());

        return captor.getValue();
    }

    private static Employee activeEmployee(UUID id) {
        Employee employee = new Employee(
                "EMP-001",
                "John",
                "Doe",
                "john.doe@acme.com",
                "US",
                "Engineering",
                "Software Engineer"
        );

        return TestIds.assign(employee, id);
    }

    private static UUID idOf(Compensation compensation) {
        return TestIds.assignRandom(compensation);
    }
}

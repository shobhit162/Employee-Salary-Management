package com.acme.employeemanagement.compensation;

import com.acme.employeemanagement.common.exception.BusinessRuleViolationException;
import com.acme.employeemanagement.common.exception.ResourceNotFoundException;
import com.acme.employeemanagement.compensation.dto.CompensationResponse;
import com.acme.employeemanagement.compensation.dto.CompensationSummaryResponse;
import com.acme.employeemanagement.compensation.dto.CreateCompensationRequest;
import com.acme.employeemanagement.employee.Employee;
import com.acme.employeemanagement.employee.EmployeeRepository;
import com.acme.employeemanagement.employee.EmploymentStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

/**
 * Owns the salary timeline of an employee.
 *
 * <p>Compensation periods are half-open ranges {@code [effectiveFrom, effectiveTo)}
 * so consecutive periods share a boundary date without overlapping. The database
 * enforces non-overlap with an exclusion constraint; this service enforces the
 * business rules that sit on top of it.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CompensationService {

    private final CompensationRepository compensationRepository;
    private final EmployeeRepository employeeRepository;
    private final Clock clock;
    private final CompensationMapper compensationMapper;

    /**
     * Sets an employee's salary.
     *
     * <p>An employee who currently has no salary can be given one starting today —
     * that is the normal path right after onboarding. Changing an existing salary
     * must be scheduled for a future date, because the current period has already
     * been paid out and must stay immutable.
     */
    @Transactional
    public CompensationResponse create(
            UUID employeeId,
            CreateCompensationRequest request
    ) {
        LocalDate today = LocalDate.now(clock);
        LocalDate effectiveFrom = request.effectiveFrom();
        BigDecimal amount = validateAmount(request.amount());
        String currency = normalizeCurrency(request.currency());

        // Locking the employee row serialises concurrent salary changes for the
        // same employee, so two requests cannot both read "no scheduled change".
        Employee employee = lockEmployee(employeeId);

        if (employee.getEmploymentStatus() != EmploymentStatus.ACTIVE) {
            throw new BusinessRuleViolationException(
                    "Compensation cannot be added for a terminated employee"
            );
        }

        if (compensationRepository.existsByEmployeeIdAndEffectiveFromAfter(
                employeeId,
                today
        )) {
            throw new BusinessRuleViolationException(
                    "Employee already has a scheduled compensation change. "
                            + "Cancel it before scheduling another one."
            );
        }

        Compensation current = compensationRepository
                .findEffectiveCompensation(employeeId, today)
                .orElse(null);

        if (current == null) {
            if (effectiveFrom.isBefore(today)) {
                throw new BusinessRuleViolationException(
                        "Compensation cannot be backdated"
                );
            }

            return compensationMapper.toResponse(
                    compensationRepository.save(new Compensation(
                            employee,
                            amount,
                            currency,
                            effectiveFrom,
                            null
                    ))
            );
        }

        if (!effectiveFrom.isAfter(today)) {
            throw new BusinessRuleViolationException(
                    "A salary change must take effect on a future date"
            );
        }

        current.closeAt(effectiveFrom);

        Compensation scheduled = compensationRepository.save(new Compensation(
                employee,
                amount,
                currency,
                effectiveFrom,
                null
        ));

        return compensationMapper.toResponse(scheduled);
    }

    /**
     * Cancels a not-yet-effective salary change and restores the previous period
     * to open-ended, so the employee is not left without a salary from the
     * cancelled date onwards.
     */
    @Transactional
    public void cancelScheduledCompensation(
            UUID employeeId,
            UUID compensationId
    ) {
        LocalDate today = LocalDate.now(clock);

        lockEmployee(employeeId);

        Compensation compensation = compensationRepository
                .findById(compensationId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Compensation not found: " + compensationId
                ));

        if (!compensation.getEmployee().getId().equals(employeeId)) {
            throw new ResourceNotFoundException(
                    "Compensation not found for employee: " + employeeId
            );
        }

        if (!compensation.getEffectiveFrom().isAfter(today)) {
            throw new BusinessRuleViolationException(
                    "Only a future scheduled salary change can be cancelled. "
                            + "Current and historical compensation is immutable."
            );
        }

        Optional<Compensation> previous = compensationRepository
                .findPeriodEndingOn(employeeId, compensation.getEffectiveFrom());

        // The delete must reach the database before the previous period is
        // re-opened, otherwise the two rows momentarily overlap and the
        // exclusion constraint rejects the transaction.
        compensationRepository.delete(compensation);
        compensationRepository.flush();

        previous.ifPresent(Compensation::reopen);
    }

    /**
     * Everything the salary tab of an employee needs in a single round trip:
     * what they earn now, what is scheduled, and the full timeline.
     */
    public CompensationSummaryResponse getSummary(UUID employeeId) {
        LocalDate today = LocalDate.now(clock);

        verifyEmployeeExists(employeeId);

        CompensationResponse current = compensationRepository
                .findEffectiveCompensation(employeeId, today)
                .map(compensationMapper::toResponse)
                .orElse(null);

        CompensationResponse scheduled = compensationRepository
                .findScheduledCompensations(employeeId, today)
                .stream()
                .findFirst()
                .map(compensationMapper::toResponse)
                .orElse(null);

        List<CompensationResponse> history = compensationRepository
                .findHistory(employeeId)
                .stream()
                .map(compensationMapper::toResponse)
                .toList();

        return new CompensationSummaryResponse(
                employeeId,
                current,
                scheduled,
                history
        );
    }

    private Employee lockEmployee(UUID employeeId) {
        return employeeRepository.findByIdForUpdate(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Employee not found: " + employeeId
                ));
    }

    private void verifyEmployeeExists(UUID employeeId) {
        if (!employeeRepository.existsById(employeeId)) {
            throw new ResourceNotFoundException(
                    "Employee not found: " + employeeId
            );
        }
    }

    private BigDecimal validateAmount(BigDecimal amount) {
        if (amount == null || amount.signum() <= 0) {
            throw new BusinessRuleViolationException(
                    "Compensation amount must be greater than zero"
            );
        }

        return amount;
    }

    private String normalizeCurrency(String currency) {
        String normalized = currency == null
                ? ""
                : currency.trim().toUpperCase(Locale.ROOT);

        if (!normalized.matches("[A-Z]{3}")) {
            throw new BusinessRuleViolationException(
                    "Currency must be a 3-letter ISO currency code"
            );
        }

        return normalized;
    }
}

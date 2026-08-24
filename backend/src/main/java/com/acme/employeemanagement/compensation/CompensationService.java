package com.acme.employeemanagement.compensation;

import com.acme.employeemanagement.common.exception.BusinessRuleViolationException;
import com.acme.employeemanagement.common.exception.ResourceNotFoundException;
import com.acme.employeemanagement.common.time.ClockConfig;
import com.acme.employeemanagement.compensation.dto.CompensationResponse;
import com.acme.employeemanagement.compensation.dto.CreateCompensationRequest;
import com.acme.employeemanagement.employee.Employee;
import com.acme.employeemanagement.employee.EmploymentStatus;
import com.acme.employeemanagement.employee.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CompensationService {

    private final CompensationRepository compensationRepository;
    private final EmployeeRepository employeeRepository;
    private final Clock clock;
    private final CompensationMapper compensationMapper;

    @Transactional
    public CompensationResponse create(
            UUID employeeId,
            CreateCompensationRequest request
    ) {
        LocalDate today = LocalDate.now(clock);
        LocalDate effectiveFrom = request.effectiveFrom();

        if (!effectiveFrom.isAfter(today)) {
            throw new BusinessRuleViolationException(
                    "Compensation effective date must be in the future"
            );
        }

        Employee employee = employeeRepository.findByIdForUpdate(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Employee not found: " + employeeId
                ));

        if (employee.getEmploymentStatus() != EmploymentStatus.ACTIVE) {
            throw new BusinessRuleViolationException(
                    "Compensation cannot be added for a terminated employee"
            );
        }

        List<Compensation> scheduled =
                compensationRepository.findScheduledCompensations(
                        employeeId,
                        today
                );

        if (!scheduled.isEmpty()) {
            throw new BusinessRuleViolationException(
                    "Employee already has a scheduled compensation change"
            );
        }

        Compensation current =
                compensationRepository.findEffectiveCompensation(
                        employeeId,
                        today
                ).orElse(null);

        if (current == null) {
            Compensation compensation = new Compensation(
                    employee,
                    validateAmount(request.amount()),
                    normalizeCurrency(request.currency()),
                    effectiveFrom,
                    null
            );

            return compensationMapper.toResponse(
                    compensationRepository.save(compensation)
            );
        }

        if (!effectiveFrom.isAfter(current.getEffectiveFrom())) {
            throw new BusinessRuleViolationException(
                    "New compensation must start after current compensation"
            );
        }

        current.closeAt(effectiveFrom);

        Compensation compensation = new Compensation(
                employee,
                validateAmount(request.amount()),
                normalizeCurrency(request.currency()),
                effectiveFrom,
                null
        );

        compensationRepository.save(current);

        Compensation saved =
                compensationRepository.save(compensation);

        return compensationMapper.toResponse(saved);
    }

    @Transactional
    public void cancelScheduledCompensation(
            UUID employeeId,
            UUID compensationId
    ) {
        LocalDate today = LocalDate.now(clock);

        Employee employee = employeeRepository.findByIdForUpdate(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Employee not found: " + employeeId
                ));

        Compensation compensation =
                compensationRepository.findById(compensationId)
                        .orElseThrow(() -> new ResourceNotFoundException(
                                "Compensation not found: " + compensationId
                        ));

        if (!compensation.getEmployee().getId().equals(employeeId)) {
            throw new ResourceNotFoundException(
                    "Compensation does not belong to employee: " + employeeId
            );
        }

        if (!compensation.getEffectiveFrom().isAfter(today)) {
            throw new BusinessRuleViolationException(
                    "Only future scheduled compensation can be cancelled"
            );
        }

        compensationRepository.delete(compensation);
    }

    public CompensationResponse getCurrent(UUID employeeId) {
        LocalDate today = LocalDate.now(clock);

        verifyEmployeeExists(employeeId);

        Compensation compensation =
                compensationRepository.findEffectiveCompensation(
                        employeeId,
                        today
                ).orElseThrow(() -> new ResourceNotFoundException(
                        "No current compensation found for employee: "
                                + employeeId
                ));

        return compensationMapper.toResponse(compensation);
    }

    public List<CompensationResponse> getHistory(UUID employeeId) {
        verifyEmployeeExists(employeeId);

        return compensationRepository
                .findHistory(employeeId)
                .stream()
                .map(compensationMapper::toResponse)
                .toList();
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
        String normalized = currency.trim().toUpperCase(Locale.ROOT);

        if (!normalized.matches("[A-Z]{3}")) {
            throw new BusinessRuleViolationException(
                    "Currency must be a 3-letter ISO currency code"
            );
        }

        return normalized;
    }
}
package com.acme.employeemanagement.compensation;

import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import com.acme.employeemanagement.common.exception.BusinessRuleViolationException;
import com.acme.employeemanagement.compensation.dto.CreateCompensationRequest;
import com.acme.employeemanagement.employee.Employee;

public class CompensationServiceTest {
    @Test
    void shouldRejectPastEffectiveDate() {
        CreateCompensationRequest request =
                new CreateCompensationRequest(
                        new BigDecimal("100000"),
                        "USD",
                        LocalDate.of(2026, 8, 23)
                );

        assertThatThrownBy(() ->
                compensationService.create(employeeId, request)
        )
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining(
                        "effective date must be in the future"
                );
    }

    @Test
    void shouldRejectTodayAsEffectiveDate() {
        CreateCompensationRequest request =
                new CreateCompensationRequest(
                        new BigDecimal("100000"),
                        "USD",
                        LocalDate.of(2026, 8, 24)
                );

        assertThatThrownBy(() ->
                compensationService.create(employeeId, request)
        )
                .isInstanceOf(BusinessRuleViolationException.class);
    }

    @Test
    void shouldRejectZeroAmount() {
        CreateCompensationRequest request =
                new CreateCompensationRequest(
                        BigDecimal.ZERO,
                        "USD",
                        LocalDate.of(2026, 9, 1)
                );

        assertThatThrownBy(() ->
                compensationService.create(employeeId, request)
        )
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("greater than zero");
    }

    @Test
    void shouldRejectCompensationForTerminatedEmployee() {
        Employee employee = new Employee(
                "EMP-001",
                "John",
                "Doe",
                "john@example.com",
                "US",
                "Engineering",
                "Software Engineer"
        );

        employee.terminate(LocalDate.of(2026, 8, 20));

        when(employeeRepository.findByIdForUpdate(employeeId))
                .thenReturn(Optional.of(employee));

        CreateCompensationRequest request =
                new CreateCompensationRequest(
                        new BigDecimal("100000"),
                        "USD",
                        LocalDate.of(2026, 9, 1)
                );

        assertThatThrownBy(() ->
                compensationService.create(employeeId, request)
        )
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("terminated employee");
    }
}

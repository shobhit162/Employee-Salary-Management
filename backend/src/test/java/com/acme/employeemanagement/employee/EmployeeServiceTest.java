package com.acme.employeemanagement.employee;

import com.acme.employeemanagement.common.exception.BusinessRuleViolationException;
import com.acme.employeemanagement.common.exception.DuplicateResourceException;
import com.acme.employeemanagement.common.exception.ResourceNotFoundException;
import com.acme.employeemanagement.compensation.CompensationRepository;
import com.acme.employeemanagement.employee.dto.CreateEmployeeRequest;
import com.acme.employeemanagement.employee.dto.EmployeeResponse;
import com.acme.employeemanagement.employee.dto.UpdateEmployeeRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmployeeServiceTest {

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private EmployeeMapper employeeMapper;

    @Mock
    private CompensationRepository compensationRepository;

    private EmployeeService employeeService;
    private Clock clock;

    @BeforeEach
    void setUp() {
        clock = Clock.fixed(
        Instant.parse("2026-08-24T00:00:00Z"),
        ZoneOffset.UTC
        );

        employeeService = new EmployeeService(
        employeeRepository,
        employeeMapper,
        compensationRepository,
        clock
);
    }

    @Test
    void shouldCreateEmployee() {
        CreateEmployeeRequest request = new CreateEmployeeRequest(
                " emp-001 ",
                "John",
                "Doe",
                "John.Doe@ACME.COM",
                "us",
                "Engineering",
                "Software Engineer"
        );

        when(employeeRepository.existsByEmployeeCodeIgnoreCase("EMP-001"))
                .thenReturn(false);

        when(employeeRepository.existsByEmailIgnoreCase("john.doe@acme.com"))
                .thenReturn(false);

        Employee savedEmployee = new Employee(
                "EMP-001",
                "John",
                "Doe",
                "john.doe@acme.com",
                "US",
                "Engineering",
                "Software Engineer"
        );

        when(employeeRepository.save(any(Employee.class)))
                .thenReturn(savedEmployee);

        EmployeeResponse expectedResponse = new EmployeeResponse(
                UUID.randomUUID(),
                "EMP-001",
                "John",
                "Doe",
                "john.doe@acme.com",
                "US",
                "Engineering",
                "Software Engineer",
                EmploymentStatus.ACTIVE,
                null,
                null,
                null
        );

        when(employeeMapper.toResponse(savedEmployee))
                .thenReturn(expectedResponse);

        EmployeeResponse response = employeeService.create(request);

        assertThat(response).isEqualTo(expectedResponse);

        ArgumentCaptor<Employee> captor =
                ArgumentCaptor.forClass(Employee.class);

        verify(employeeRepository).save(captor.capture());

        Employee employee = captor.getValue();

        assertThat(employee.getEmployeeCode()).isEqualTo("EMP-001");
        assertThat(employee.getEmail()).isEqualTo("john.doe@acme.com");
        assertThat(employee.getCountryCode()).isEqualTo("US");
        assertThat(employee.getEmploymentStatus())
                .isEqualTo(EmploymentStatus.ACTIVE);
    }

    @Test
    void shouldRejectDuplicateEmployeeCode() {
        CreateEmployeeRequest request = new CreateEmployeeRequest(
                "EMP-001",
                "John",
                "Doe",
                "john@example.com",
                "US",
                "Engineering",
                "Software Engineer"
        );

        when(employeeRepository.existsByEmployeeCodeIgnoreCase("EMP-001"))
                .thenReturn(true);

        assertThatThrownBy(() -> employeeService.create(request))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("Employee code already exists");
    }

    @Test
    void shouldRejectDuplicateEmail() {
        CreateEmployeeRequest request = new CreateEmployeeRequest(
                "EMP-001",
                "John",
                "Doe",
                "john@example.com",
                "US",
                "Engineering",
                "Software Engineer"
        );

        when(employeeRepository.existsByEmployeeCodeIgnoreCase("EMP-001"))
                .thenReturn(false);

        when(employeeRepository.existsByEmailIgnoreCase("john@example.com"))
                .thenReturn(true);

        assertThatThrownBy(() -> employeeService.create(request))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("Employee email already exists");
    }

    @Test
    void shouldThrowWhenEmployeeDoesNotExist() {
        UUID employeeId = UUID.randomUUID();

        when(employeeRepository.findById(employeeId))
                .thenReturn(java.util.Optional.empty());

        assertThatThrownBy(() -> employeeService.getById(employeeId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Employee not found");
    }

    @Test
    void shouldUpdateEmployeeDetails() {
        UUID employeeId = UUID.randomUUID();

        Employee employee = new Employee(
                "EMP-001",
                "John",
                "Doe",
                "john@example.com",
                "US",
                "Engineering",
                "Software Engineer"
        );

        UpdateEmployeeRequest request = new UpdateEmployeeRequest(
                "Jane",
                "Doe",
                "jane@example.com",
                "IN",
                "Product",
                "Product Manager"
        );

        when(employeeRepository.findById(employeeId))
                .thenReturn(java.util.Optional.of(employee));

        when(employeeRepository.existsByEmailIgnoreCaseAndIdNot(
                "jane@example.com",
                employeeId
        )).thenReturn(false);

        EmployeeResponse response = new EmployeeResponse(
                employeeId,
                "EMP-001",
                "Jane",
                "Doe",
                "jane@example.com",
                "IN",
                "Product",
                "Product Manager",
                EmploymentStatus.ACTIVE,
                null,
                null,
                null
        );

        when(employeeMapper.toResponse(employee))
                .thenReturn(response);

        EmployeeResponse result =
                employeeService.update(employeeId, request);

        assertThat(result).isEqualTo(response);

        assertThat(employee.getFirstName()).isEqualTo("Jane");
        assertThat(employee.getCountryCode()).isEqualTo("IN");
        assertThat(employee.getDepartment()).isEqualTo("Product");
    }

    @Test
    void shouldTerminateEmployee() {
        UUID employeeId = UUID.randomUUID();

        Employee employee = new Employee(
                "EMP-001",
                "John",
                "Doe",
                "john@example.com",
                "US",
                "Engineering",
                "Software Engineer"
        );

        when(employeeRepository.findById(employeeId))
                .thenReturn(java.util.Optional.of(employee));

        EmployeeResponse response = new EmployeeResponse(
                employeeId,
                "EMP-001",
                "John",
                "Doe",
                "john@example.com",
                "US",
                "Engineering",
                "Software Engineer",
                EmploymentStatus.TERMINATED,
                java.time.LocalDate.now(),
                null,
                null
        );

        when(employeeMapper.toResponse(employee))
                .thenReturn(response);

        employeeService.terminate(employeeId);

        assertThat(employee.getEmploymentStatus())
                .isEqualTo(EmploymentStatus.TERMINATED);

        assertThat(employee.getTerminationDate())
                .isEqualTo(java.time.LocalDate.now());
    }

    @Test
    void shouldNotTerminateEmployeeTwice() {
        Employee employee = new Employee(
                "EMP-001",
                "John",
                "Doe",
                "john@example.com",
                "US",
                "Engineering",
                "Software Engineer"
        );

        employee.terminate(java.time.LocalDate.now());

        assertThatThrownBy(() ->
                employee.terminate(java.time.LocalDate.now())
        )
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("already terminated");
    }

    @Test
    void shouldRejectTerminationWhenFutureCompensationIsScheduled() {
        UUID employeeId = UUID.randomUUID();

        Employee employee = new Employee(
                "EMP-001",
                "John",
                "Doe",
                "john@example.com",
                "US",
                "Engineering",
                "Software Engineer"
        );

        when(employeeRepository.findById(employeeId))
                .thenReturn(Optional.of(employee));

        when(compensationRepository
                .existsByEmployeeIdAndEffectiveFromAfter(
                        employeeId,
                        LocalDate.of(2026, 8, 24)
                ))
                .thenReturn(true);

        assertThatThrownBy(() ->
                employeeService.terminate(employeeId)
        )
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining(
                        "future compensation change is scheduled"
                );
    }

    @Test
    void shouldTerminateWhenNoFutureCompensationIsScheduled() {
        UUID employeeId = UUID.randomUUID();

        Employee employee = new Employee(
                "EMP-001",
                "John",
                "Doe",
                "john@example.com",
                "US",
                "Engineering",
                "Software Engineer"
        );

        when(employeeRepository.findById(employeeId))
                .thenReturn(Optional.of(employee));

        when(compensationRepository
                .existsByEmployeeIdAndEffectiveFromAfter(
                        employeeId,
                        LocalDate.of(2026, 8, 24)
                ))
                .thenReturn(false);

        when(employeeMapper.toResponse(employee))
                .thenReturn(null);

        employeeService.terminate(employeeId);

        assertThat(employee.getEmploymentStatus())
                .isEqualTo(EmploymentStatus.TERMINATED);

        assertThat(employee.getTerminationDate())
                .isEqualTo(LocalDate.of(2026, 8, 24));
    }
}
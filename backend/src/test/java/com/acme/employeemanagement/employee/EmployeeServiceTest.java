package com.acme.employeemanagement.employee;

import com.acme.employeemanagement.common.exception.BusinessRuleViolationException;
import com.acme.employeemanagement.common.exception.DuplicateResourceException;
import com.acme.employeemanagement.common.exception.ResourceNotFoundException;
import com.acme.employeemanagement.compensation.CompensationMapper;
import com.acme.employeemanagement.compensation.CompensationRepository;
import com.acme.employeemanagement.employee.dto.CreateEmployeeRequest;
import com.acme.employeemanagement.employee.dto.EmployeeResponse;
import com.acme.employeemanagement.employee.dto.UpdateEmployeeRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmployeeServiceTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 8, 24);

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private CompensationRepository compensationRepository;

    private EmployeeService employeeService;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(
                TODAY.atStartOfDay(ZoneOffset.UTC).toInstant(),
                ZoneOffset.UTC
        );

        employeeService = new EmployeeService(
                employeeRepository,
                new EmployeeMapper(),
                compensationRepository,
                new CompensationMapper(),
                clock
        );
    }

    @Nested
    @DisplayName("creating an employee")
    class Create {

        @Test
        void normalisesCodeEmailAndCountryBeforeStoring() {
            when(employeeRepository.save(any(Employee.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            EmployeeResponse response = employeeService.create(
                    new CreateEmployeeRequest(
                            " emp-001 ",
                            "  John ",
                            "Doe ",
                            "John.Doe@ACME.COM",
                            "us",
                            "Engineering",
                            "Software Engineer"
                    )
            );

            ArgumentCaptor<Employee> captor =
                    ArgumentCaptor.forClass(Employee.class);
            verify(employeeRepository).save(captor.capture());
            Employee saved = captor.getValue();

            assertThat(saved.getEmployeeCode()).isEqualTo("EMP-001");
            assertThat(saved.getFirstName()).isEqualTo("John");
            assertThat(saved.getLastName()).isEqualTo("Doe");
            assertThat(saved.getEmail()).isEqualTo("john.doe@acme.com");
            assertThat(saved.getCountryCode()).isEqualTo("US");
            assertThat(saved.getEmploymentStatus())
                    .isEqualTo(EmploymentStatus.ACTIVE);
            assertThat(saved.getTerminationDate()).isNull();

            assertThat(response.employeeCode()).isEqualTo("EMP-001");
        }

        @Test
        void rejectsDuplicateEmployeeCode() {
            when(employeeRepository.existsByEmployeeCodeIgnoreCase("EMP-001"))
                    .thenReturn(true);

            assertThatThrownBy(() ->
                    employeeService.create(createRequest("EMP-001")))
                    .isInstanceOf(DuplicateResourceException.class)
                    .hasMessageContaining("Employee code already exists");

            verify(employeeRepository, never()).save(any());
        }

        @Test
        void rejectsDuplicateEmail() {
            when(employeeRepository.existsByEmployeeCodeIgnoreCase("EMP-001"))
                    .thenReturn(false);
            when(employeeRepository.existsByEmailIgnoreCase("john.doe@acme.com"))
                    .thenReturn(true);

            assertThatThrownBy(() ->
                    employeeService.create(createRequest("EMP-001")))
                    .isInstanceOf(DuplicateResourceException.class)
                    .hasMessageContaining("Employee email already exists");
        }
    }

    @Nested
    @DisplayName("updating an employee")
    class Update {

        @Test
        void appliesTheNewDetails() {
            UUID employeeId = UUID.randomUUID();
            Employee employee = activeEmployee();

            when(employeeRepository.findById(employeeId))
                    .thenReturn(Optional.of(employee));
            when(employeeRepository.existsByEmailIgnoreCaseAndIdNot(
                    "jane@acme.com",
                    employeeId
            )).thenReturn(false);

            employeeService.update(employeeId, new UpdateEmployeeRequest(
                    "Jane",
                    "Roe",
                    "Jane@ACME.com",
                    "in",
                    "Product",
                    "Product Manager"
            ));

            assertThat(employee.getFirstName()).isEqualTo("Jane");
            assertThat(employee.getLastName()).isEqualTo("Roe");
            assertThat(employee.getEmail()).isEqualTo("jane@acme.com");
            assertThat(employee.getCountryCode()).isEqualTo("IN");
            assertThat(employee.getDepartment()).isEqualTo("Product");
        }

        @Test
        void allowsKeepingTheSameEmail() {
            UUID employeeId = UUID.randomUUID();
            Employee employee = activeEmployee();

            when(employeeRepository.findById(employeeId))
                    .thenReturn(Optional.of(employee));

            employeeService.update(employeeId, new UpdateEmployeeRequest(
                    "John",
                    "Doe",
                    "John.Doe@acme.com",
                    "US",
                    "Engineering",
                    "Staff Engineer"
            ));

            assertThat(employee.getJobTitle()).isEqualTo("Staff Engineer");
            verify(employeeRepository, never())
                    .existsByEmailIgnoreCaseAndIdNot(any(), any());
        }

        @Test
        void rejectsAnEmailUsedByAnotherEmployee() {
            UUID employeeId = UUID.randomUUID();

            when(employeeRepository.findById(employeeId))
                    .thenReturn(Optional.of(activeEmployee()));
            when(employeeRepository.existsByEmailIgnoreCaseAndIdNot(
                    "taken@acme.com",
                    employeeId
            )).thenReturn(true);

            assertThatThrownBy(() ->
                    employeeService.update(employeeId, new UpdateEmployeeRequest(
                            "John",
                            "Doe",
                            "taken@acme.com",
                            "US",
                            "Engineering",
                            "Software Engineer"
                    )))
                    .isInstanceOf(DuplicateResourceException.class);
        }

        @Test
        void rejectsUnknownEmployee() {
            UUID employeeId = UUID.randomUUID();

            when(employeeRepository.findById(employeeId))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> employeeService.getById(employeeId))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Employee not found");
        }
    }

    @Nested
    @DisplayName("terminating an employee")
    class Terminate {

        @Test
        void setsStatusAndTerminationDateToToday() {
            UUID employeeId = UUID.randomUUID();
            Employee employee = activeEmployee();

            when(employeeRepository.findByIdForUpdate(employeeId))
                    .thenReturn(Optional.of(employee));
            when(compensationRepository
                    .existsByEmployeeIdAndEffectiveFromAfter(employeeId, TODAY))
                    .thenReturn(false);

            employeeService.terminate(employeeId);

            assertThat(employee.getEmploymentStatus())
                    .isEqualTo(EmploymentStatus.TERMINATED);
            assertThat(employee.getTerminationDate()).isEqualTo(TODAY);
        }

        @Test
        void isRejectedWhileASalaryChangeIsScheduled() {
            UUID employeeId = UUID.randomUUID();
            Employee employee = activeEmployee();

            when(employeeRepository.findByIdForUpdate(employeeId))
                    .thenReturn(Optional.of(employee));
            when(compensationRepository
                    .existsByEmployeeIdAndEffectiveFromAfter(employeeId, TODAY))
                    .thenReturn(true);

            assertThatThrownBy(() -> employeeService.terminate(employeeId))
                    .isInstanceOf(BusinessRuleViolationException.class)
                    .hasMessageContaining("future compensation change is scheduled");

            assertThat(employee.getEmploymentStatus())
                    .isEqualTo(EmploymentStatus.ACTIVE);
        }

        @Test
        void isRejectedForAnAlreadyTerminatedEmployee() {
            UUID employeeId = UUID.randomUUID();
            Employee employee = activeEmployee();
            employee.terminate(TODAY.minusDays(10));

            when(employeeRepository.findByIdForUpdate(employeeId))
                    .thenReturn(Optional.of(employee));

            assertThatThrownBy(() -> employeeService.terminate(employeeId))
                    .isInstanceOf(BusinessRuleViolationException.class)
                    .hasMessageContaining("already terminated");

            assertThat(employee.getTerminationDate())
                    .as("the original termination date must not be overwritten")
                    .isEqualTo(TODAY.minusDays(10));
        }
    }

    private static CreateEmployeeRequest createRequest(String employeeCode) {
        return new CreateEmployeeRequest(
                employeeCode,
                "John",
                "Doe",
                "john.doe@acme.com",
                "US",
                "Engineering",
                "Software Engineer"
        );
    }

    private static Employee activeEmployee() {
        return new Employee(
                "EMP-001",
                "John",
                "Doe",
                "john.doe@acme.com",
                "US",
                "Engineering",
                "Software Engineer"
        );
    }
}

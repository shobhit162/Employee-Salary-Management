package com.acme.employeemanagement.employee;

import com.acme.employeemanagement.common.exception.DuplicateResourceException;
import com.acme.employeemanagement.common.exception.ResourceNotFoundException;
import com.acme.employeemanagement.employee.dto.CreateEmployeeRequest;
import com.acme.employeemanagement.employee.dto.EmployeeResponse;
import com.acme.employeemanagement.employee.dto.UpdateEmployeeRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EmployeeService {

    private final EmployeeRepository employeeRepository;
    private final EmployeeMapper employeeMapper;

    @Transactional
    public EmployeeResponse create(CreateEmployeeRequest request) {
        String employeeCode = normalizeEmployeeCode(request.employeeCode());
        String email = normalizeEmail(request.email());

        validateUniqueEmployeeCode(employeeCode);
        validateUniqueEmail(email);

        Employee employee = new Employee(
                employeeCode,
                normalizeText(request.firstName()),
                normalizeText(request.lastName()),
                email,
                normalizeCountryCode(request.countryCode()),
                normalizeText(request.department()),
                normalizeText(request.jobTitle())
        );

        Employee savedEmployee = employeeRepository.save(employee);

        return employeeMapper.toResponse(savedEmployee);
    }

    public EmployeeResponse getById(UUID employeeId) {
        Employee employee = findEmployee(employeeId);
        return employeeMapper.toResponse(employee);
    }

    public Page<EmployeeResponse> search(
            String search,
            String countryCode,
            String department,
            EmploymentStatus status,
            Pageable pageable
    ) {
        Specification<Employee> specification = Specification.allOf(
                EmployeeSpecifications.search(search),
                EmployeeSpecifications.hasCountry(countryCode),
                EmployeeSpecifications.hasDepartment(department),
                EmployeeSpecifications.hasStatus(status)
        );

        return employeeRepository
                .findAll(specification, pageable)
                .map(employeeMapper::toResponse);
    }

    @Transactional
    public EmployeeResponse update(
            UUID employeeId,
            UpdateEmployeeRequest request
    ) {
        Employee employee = findEmployee(employeeId);

        String email = normalizeEmail(request.email());

        if (!employee.getEmail().equalsIgnoreCase(email)
                && employeeRepository.existsByEmailIgnoreCaseAndIdNot(
                        email,
                        employeeId
                )) {
            throw new DuplicateResourceException(
                    "Employee email already exists: " + email
            );
        }

        employee.updateDetails(
                normalizeText(request.firstName()),
                normalizeText(request.lastName()),
                email,
                normalizeCountryCode(request.countryCode()),
                normalizeText(request.department()),
                normalizeText(request.jobTitle())
        );

        return employeeMapper.toResponse(employee);
    }

    @Transactional
    public EmployeeResponse terminate(UUID employeeId) {
        Employee employee = findEmployee(employeeId);

        employee.terminate(LocalDate.now());

        return employeeMapper.toResponse(employee);
    }

    private Employee findEmployee(UUID employeeId) {
        return employeeRepository.findById(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Employee not found: " + employeeId
                ));
    }

    private void validateUniqueEmployeeCode(String employeeCode) {
        if (employeeRepository.existsByEmployeeCodeIgnoreCase(employeeCode)) {
            throw new DuplicateResourceException(
                    "Employee code already exists: " + employeeCode
            );
        }
    }

    private void validateUniqueEmail(String email) {
        if (employeeRepository.existsByEmailIgnoreCase(email)) {
            throw new DuplicateResourceException(
                    "Employee email already exists: " + email
            );
        }
    }

    private String normalizeEmployeeCode(String employeeCode) {
        return employeeCode.trim().toUpperCase(Locale.ROOT);
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizeCountryCode(String countryCode) {
        return countryCode.trim().toUpperCase(Locale.ROOT);
    }

    private String normalizeText(String value) {
        return value.trim();
    }
}
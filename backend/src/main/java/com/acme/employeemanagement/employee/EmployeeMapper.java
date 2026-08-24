package com.acme.employeemanagement.employee;

import com.acme.employeemanagement.employee.dto.EmployeeResponse;
import org.springframework.stereotype.Component;

@Component
public class EmployeeMapper {

    public EmployeeResponse toResponse(Employee employee) {
        return new EmployeeResponse(
                employee.getId(),
                employee.getEmployeeCode(),
                employee.getFirstName(),
                employee.getLastName(),
                employee.getEmail(),
                employee.getCountryCode(),
                employee.getDepartment(),
                employee.getJobTitle(),
                employee.getEmploymentStatus(),
                employee.getTerminationDate(),
                employee.getCreatedAt(),
                employee.getUpdatedAt()
        );
    }
}
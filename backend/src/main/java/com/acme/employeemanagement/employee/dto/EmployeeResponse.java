package com.acme.employeemanagement.employee.dto;

import com.acme.employeemanagement.employee.EmploymentStatus;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record EmployeeResponse(
        UUID id,
        String employeeCode,
        String firstName,
        String lastName,
        String email,
        String countryCode,
        String department,
        String jobTitle,
        EmploymentStatus employmentStatus,
        LocalDate terminationDate,
        Instant createdAt,
        Instant updatedAt
) {
}
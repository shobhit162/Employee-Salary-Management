package com.acme.employeemanagement.employee.dto;

import com.acme.employeemanagement.compensation.dto.CompensationResponse;

/**
 * A row of the employee list.
 *
 * <p>Salary is included because the list is where HR actually works: without it
 * every "what does this person earn?" question costs a page load.
 *
 * @param currentCompensation {@code null} when the employee has no salary
 *                            effective today
 */
public record EmployeeListItemResponse(
        EmployeeResponse employee,
        CompensationResponse currentCompensation
) {
}

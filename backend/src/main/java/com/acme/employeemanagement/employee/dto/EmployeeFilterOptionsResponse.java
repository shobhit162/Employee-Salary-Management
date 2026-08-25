package com.acme.employeemanagement.employee.dto;

import java.util.List;

/**
 * The values that actually appear in the data, so the UI's filters offer real
 * choices instead of a hard-coded list that drifts from the organisation.
 */
public record EmployeeFilterOptionsResponse(
        List<String> countryCodes,
        List<String> departments
) {
}

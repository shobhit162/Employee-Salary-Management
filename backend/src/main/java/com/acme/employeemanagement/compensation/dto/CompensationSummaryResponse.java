package com.acme.employeemanagement.compensation.dto;

import java.util.List;
import java.util.UUID;

/**
 * The complete salary picture for one employee.
 *
 * @param current   effective today, or {@code null} if the employee has no salary yet
 * @param scheduled the pending future change, or {@code null} if none is scheduled
 * @param history   every period, newest first
 */
public record CompensationSummaryResponse(
        UUID employeeId,
        CompensationResponse current,
        CompensationResponse scheduled,
        List<CompensationResponse> history
) {
}

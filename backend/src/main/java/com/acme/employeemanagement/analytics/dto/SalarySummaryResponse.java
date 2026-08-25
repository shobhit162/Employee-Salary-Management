package com.acme.employeemanagement.analytics.dto;

import java.time.LocalDate;

/**
 * The headline numbers of the dashboard.
 *
 * @param asOf     the date the salaries were read at
 * @param currency the reporting currency every amount is expressed in
 */
public record SalarySummaryResponse(
        LocalDate asOf,
        String currency,
        SalaryStatistics statistics
) {
}

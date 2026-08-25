package com.acme.employeemanagement.analytics;

import com.acme.employeemanagement.employee.EmploymentStatus;

import java.time.LocalDate;

/**
 * The scope of an analytics question: which employees, on which date, reported in
 * which currency.
 *
 * @param asOf         the date salaries are read at — "current pay" means the
 *                     period covering this date
 * @param status       {@code null} means every employee regardless of status
 * @param countryCode  {@code null} means every country
 * @param department   {@code null} means every department
 * @param currency     the reporting currency all amounts are converted into
 */
public record AnalyticsFilter(
        LocalDate asOf,
        EmploymentStatus status,
        String countryCode,
        String department,
        String currency
) {
}

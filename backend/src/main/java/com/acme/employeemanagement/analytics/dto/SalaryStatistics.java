package com.acme.employeemanagement.analytics.dto;

import java.math.BigDecimal;

/**
 * The pay statistics for one cohort, all amounts in the reporting currency.
 *
 * @param employeeCount            employees in the cohort
 * @param compensatedEmployeeCount those of them that actually have a salary on
 *                                 the reporting date — a gap between the two is a
 *                                 data-quality signal HR needs to see
 * @param totalAnnualCompensation  annualised payroll for the cohort
 * @param average                  {@code null} when nobody in the cohort has a salary
 * @param median                   {@code null} when nobody in the cohort has a salary
 * @param minimum                  {@code null} when nobody in the cohort has a salary
 * @param maximum                  {@code null} when nobody in the cohort has a salary
 */
public record SalaryStatistics(
        long employeeCount,
        long compensatedEmployeeCount,
        BigDecimal totalAnnualCompensation,
        BigDecimal average,
        BigDecimal median,
        BigDecimal minimum,
        BigDecimal maximum
) {
}

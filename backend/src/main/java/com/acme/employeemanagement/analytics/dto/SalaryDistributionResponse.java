package com.acme.employeemanagement.analytics.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * A histogram of salaries in equal-width bands.
 *
 * @param bandSize width of each band in the reporting currency
 */
public record SalaryDistributionResponse(
        LocalDate asOf,
        String currency,
        BigDecimal bandSize,
        List<SalaryBand> bands
) {

    /**
     * @param upperBound exclusive, or {@code null} for the final open-ended band
     *                   that collects every salary above the last boundary
     */
    public record SalaryBand(
            BigDecimal lowerBound,
            BigDecimal upperBound,
            long employeeCount
    ) {
    }
}

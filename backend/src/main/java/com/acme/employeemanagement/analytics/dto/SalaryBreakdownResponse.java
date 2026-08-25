package com.acme.employeemanagement.analytics.dto;

import com.acme.employeemanagement.analytics.BreakdownDimension;

import java.time.LocalDate;
import java.util.List;

public record SalaryBreakdownResponse(
        LocalDate asOf,
        String currency,
        BreakdownDimension dimension,
        List<SalaryBreakdownRow> rows
) {

    /**
     * @param key the country code or department name the row aggregates
     */
    public record SalaryBreakdownRow(
            String key,
            SalaryStatistics statistics
    ) {
    }
}

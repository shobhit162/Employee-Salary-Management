package com.acme.employeemanagement.analytics;

/**
 * The organisational dimensions compensation can be grouped by.
 *
 * <p>The column name is carried here rather than being interpolated from request
 * input, so the grouping column of the aggregation query can never come from an
 * untrusted string.
 */
public enum BreakdownDimension {

    COUNTRY("country_code"),
    DEPARTMENT("department");

    private final String column;

    BreakdownDimension(String column) {
        this.column = column;
    }

    String column() {
        return column;
    }
}

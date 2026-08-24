package com.acme.employeemanagement.compensation.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record CompensationResponse(
        UUID id,
        UUID employeeId,
        BigDecimal amount,
        String currency,
        LocalDate effectiveFrom,
        LocalDate effectiveTo
) {
}
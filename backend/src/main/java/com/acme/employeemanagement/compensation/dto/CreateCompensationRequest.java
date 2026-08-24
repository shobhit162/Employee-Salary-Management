package com.acme.employeemanagement.compensation.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CreateCompensationRequest(

        @NotNull
        @DecimalMin(value = "0.01")
        BigDecimal amount,

        @NotBlank
        @Pattern(regexp = "^[A-Za-z]{3}$")
        String currency,

        @NotNull
        LocalDate effectiveFrom
) {
}
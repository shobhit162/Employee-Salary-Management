package com.acme.employeemanagement.analytics;

import com.acme.employeemanagement.analytics.dto.SalaryBreakdownResponse;
import com.acme.employeemanagement.analytics.dto.SalaryDistributionResponse;
import com.acme.employeemanagement.analytics.dto.SalarySummaryResponse;
import com.acme.employeemanagement.common.currency.ExchangeRateProperties;
import com.acme.employeemanagement.employee.EmploymentStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;

@RestController
@RequestMapping("/api/v1/analytics")
@RequiredArgsConstructor
public class AnalyticsController {

    private static final BigDecimal DEFAULT_BAND_SIZE = new BigDecimal("25000");

    private final AnalyticsService analyticsService;
    private final ExchangeRateProperties exchangeRateProperties;
    private final Clock clock;

    /**
     * Headline KPIs. Defaults to active employees today, because "what do we pay
     * right now?" is the question the dashboard opens on.
     */
    @GetMapping("/summary")
    public SalarySummaryResponse summary(
            @RequestParam(required = false) String countryCode,
            @RequestParam(required = false) String department,
            @RequestParam(required = false, defaultValue = "ACTIVE")
            String status,
            @RequestParam(required = false) String currency,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate asOf
    ) {
        return analyticsService.summary(
                filter(countryCode, department, status, currency, asOf)
        );
    }

    @GetMapping("/breakdown")
    public SalaryBreakdownResponse breakdown(
            @RequestParam BreakdownDimension dimension,
            @RequestParam(required = false) String countryCode,
            @RequestParam(required = false) String department,
            @RequestParam(required = false, defaultValue = "ACTIVE")
            String status,
            @RequestParam(required = false) String currency,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate asOf
    ) {
        return analyticsService.breakdown(
                filter(countryCode, department, status, currency, asOf),
                dimension
        );
    }

    @GetMapping("/distribution")
    public SalaryDistributionResponse distribution(
            @RequestParam(required = false) BigDecimal bandSize,
            @RequestParam(required = false) String countryCode,
            @RequestParam(required = false) String department,
            @RequestParam(required = false, defaultValue = "ACTIVE")
            String status,
            @RequestParam(required = false) String currency,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate asOf
    ) {
        return analyticsService.distribution(
                filter(countryCode, department, status, currency, asOf),
                bandSize == null ? DEFAULT_BAND_SIZE : bandSize
        );
    }

    @GetMapping("/currencies")
    public List<String> reportingCurrencies() {
        return analyticsService.reportingCurrencies();
    }

    private AnalyticsFilter filter(
            String countryCode,
            String department,
            String status,
            String currency,
            LocalDate asOf
    ) {
        return new AnalyticsFilter(
                asOf == null ? LocalDate.now(clock) : asOf,
                parseStatus(status),
                blankToNull(upperCase(countryCode)),
                blankToNull(department),
                currency == null || currency.isBlank()
                        ? upperCase(exchangeRateProperties.getReporting())
                        : upperCase(currency)
        );
    }

    /**
     * {@code status=ALL} is the explicit way to include terminated employees, so
     * that "no filter" can keep its safer meaning of active employees only.
     */
    private static EmploymentStatus parseStatus(String status) {
        if (status == null || status.isBlank() || "ALL".equalsIgnoreCase(status)) {
            return null;
        }

        return EmploymentStatus.valueOf(status.toUpperCase(Locale.ROOT));
    }

    private static String upperCase(String value) {
        return value == null ? null : value.trim().toUpperCase(Locale.ROOT);
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}

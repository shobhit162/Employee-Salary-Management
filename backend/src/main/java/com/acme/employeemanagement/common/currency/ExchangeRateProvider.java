package com.acme.employeemanagement.common.currency;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Supplies the rates used to express salaries held in different currencies in a
 * single reporting currency.
 *
 * <p>The MVP is backed by rates from configuration. Keeping this behind an
 * interface means a live FX feed (with its own caching, retries and rate-as-of
 * semantics) can replace it without touching the compensation or analytics
 * domain.
 */
public interface ExchangeRateProvider {

    /**
     * The factor that converts an amount in {@code from} into {@code to}.
     *
     * @return empty when either currency has no configured rate — callers must
     *         surface that rather than quietly dropping the amount
     */
    Optional<BigDecimal> rate(String from, String to);

    /**
     * Every currency this provider can convert. Used to fail fast when the
     * database holds a currency the provider does not know about.
     */
    Set<String> supportedCurrencies();

    /**
     * The full rate table for one reporting currency, so aggregation can be
     * pushed into SQL in a single pass instead of converting row by row in Java.
     */
    default Map<String, BigDecimal> ratesTo(String reportingCurrency) {
        return supportedCurrencies().stream()
                .collect(Collectors.toMap(
                        Function.identity(),
                        currency -> rate(currency, reportingCurrency)
                                .orElseThrow(() -> new IllegalArgumentException(
                                        "No exchange rate from " + currency
                                                + " to " + reportingCurrency
                                ))
                ));
    }
}

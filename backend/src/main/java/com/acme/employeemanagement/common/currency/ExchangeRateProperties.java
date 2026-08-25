package com.acme.employeemanagement.common.currency;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Exchange rates expressed as "how many units of this currency equal one unit of
 * {@link #getBase()}". Configured in {@code application.properties} under
 * {@code app.exchange-rates}.
 */
@ConfigurationProperties(prefix = "app.exchange-rates")
public class ExchangeRateProperties {

    /** Currency the configured rates are quoted against. */
    private String base = "USD";

    /** Default reporting currency for analytics when the caller does not pick one. */
    private String reporting = "USD";

    private Map<String, BigDecimal> rates = new LinkedHashMap<>();

    public String getBase() {
        return base;
    }

    public void setBase(String base) {
        this.base = base;
    }

    public String getReporting() {
        return reporting;
    }

    public void setReporting(String reporting) {
        this.reporting = reporting;
    }

    public Map<String, BigDecimal> getRates() {
        return rates;
    }

    public void setRates(Map<String, BigDecimal> rates) {
        this.rates = rates;
    }
}

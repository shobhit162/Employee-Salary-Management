package com.acme.employeemanagement.common.currency;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Reads a fixed rate table from configuration.
 *
 * <p>Rates are deliberately static: analytics must be reproducible and tests must
 * not depend on a third-party service being reachable. The trade-off is that
 * historical analytics use today's rates rather than the rate that applied on the
 * date being reported — acceptable while the product answers "how do we pay
 * people now?" rather than "what did last year cost at last year's rates?".
 */
@Component
public class ConfiguredExchangeRateProvider implements ExchangeRateProvider {

    private static final MathContext RATE_PRECISION = new MathContext(12);

    private final String baseCurrency;
    private final Map<String, BigDecimal> unitsPerBase;

    public ConfiguredExchangeRateProvider(ExchangeRateProperties properties) {
        this.baseCurrency = normalize(properties.getBase());
        this.unitsPerBase = properties.getRates().entrySet().stream()
                .collect(Collectors.toUnmodifiableMap(
                        entry -> normalize(entry.getKey()),
                        Map.Entry::getValue
                ));

        if (!unitsPerBase.containsKey(baseCurrency)) {
            throw new IllegalStateException(
                    "Exchange rate configuration must include the base currency: "
                            + baseCurrency
            );
        }

        unitsPerBase.forEach((currency, rate) -> {
            if (rate == null || rate.signum() <= 0) {
                throw new IllegalStateException(
                        "Exchange rate for " + currency + " must be positive"
                );
            }
        });
    }

    @Override
    public Optional<BigDecimal> rate(String from, String to) {
        String source = normalize(from);
        String target = normalize(to);

        if (source.equals(target)) {
            return Optional.of(BigDecimal.ONE);
        }

        BigDecimal sourcePerBase = unitsPerBase.get(source);
        BigDecimal targetPerBase = unitsPerBase.get(target);

        if (sourcePerBase == null || targetPerBase == null) {
            return Optional.empty();
        }

        return Optional.of(
                targetPerBase.divide(sourcePerBase, RATE_PRECISION)
        );
    }

    @Override
    public Set<String> supportedCurrencies() {
        return unitsPerBase.keySet();
    }

    private static String normalize(String currency) {
        return Optional.ofNullable(currency)
                .map(String::trim)
                .map(value -> value.toUpperCase(Locale.ROOT))
                .filter(value -> value.matches("[A-Z]{3}"))
                .orElseThrow(() -> new IllegalArgumentException(
                        "Not a 3-letter ISO currency code: " + currency
                ));
    }
}

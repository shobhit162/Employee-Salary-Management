package com.acme.employeemanagement.analytics;

import com.acme.employeemanagement.analytics.dto.SalaryBreakdownResponse;
import com.acme.employeemanagement.analytics.dto.SalaryDistributionResponse;
import com.acme.employeemanagement.analytics.dto.SalarySummaryResponse;
import com.acme.employeemanagement.common.currency.ExchangeRateProvider;
import com.acme.employeemanagement.common.exception.BusinessRuleViolationException;
import com.acme.employeemanagement.compensation.CompensationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

/**
 * Answers "how does the organisation pay people?".
 *
 * <p>Every figure is reported in a single currency. Before aggregating, the
 * service checks that a rate exists for every currency actually present in the
 * data: without that check a missing rate would drop those salaries from the
 * totals and the dashboard would quietly under-report payroll.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AnalyticsService {

    /**
     * Salaries at or above {@code MAX_BAND_INDEX × bandSize} are reported as one
     * open-ended top band, so a single outlier cannot stretch the histogram into
     * thousands of empty buckets.
     */
    private static final int MAX_BAND_INDEX = 40;

    private final AnalyticsRepository analyticsRepository;
    private final CompensationRepository compensationRepository;
    private final ExchangeRateProvider exchangeRateProvider;

    public SalarySummaryResponse summary(AnalyticsFilter filter) {
        return new SalarySummaryResponse(
                filter.asOf(),
                filter.currency(),
                analyticsRepository.summarise(filter, rates(filter))
        );
    }

    public SalaryBreakdownResponse breakdown(
            AnalyticsFilter filter,
            BreakdownDimension dimension
    ) {
        return new SalaryBreakdownResponse(
                filter.asOf(),
                filter.currency(),
                dimension,
                analyticsRepository.breakdown(filter, dimension, rates(filter))
        );
    }

    public SalaryDistributionResponse distribution(
            AnalyticsFilter filter,
            BigDecimal bandSize
    ) {
        if (bandSize == null || bandSize.signum() <= 0) {
            throw new BusinessRuleViolationException(
                    "Salary band size must be greater than zero"
            );
        }

        return new SalaryDistributionResponse(
                filter.asOf(),
                filter.currency(),
                bandSize,
                analyticsRepository.distribution(
                        filter,
                        rates(filter),
                        bandSize,
                        MAX_BAND_INDEX
                )
        );
    }

    /**
     * @throws BusinessRuleViolationException if any currency in use cannot be
     *         converted into the reporting currency
     */
    private Map<String, BigDecimal> rates(AnalyticsFilter filter) {
        String reportingCurrency = filter.currency();

        if (!exchangeRateProvider.supportedCurrencies()
                .contains(reportingCurrency)) {
            throw new BusinessRuleViolationException(
                    "No exchange rates are configured for reporting currency "
                            + reportingCurrency
            );
        }

        List<String> unsupported = compensationRepository
                .findDistinctCurrencies()
                .stream()
                .filter(currency -> exchangeRateProvider
                        .rate(currency, reportingCurrency)
                        .isEmpty())
                .sorted()
                .toList();

        if (!unsupported.isEmpty()) {
            throw new BusinessRuleViolationException(
                    "Cannot report in " + reportingCurrency
                            + ": no exchange rate configured for "
                            + String.join(", ", unsupported)
            );
        }

        return exchangeRateProvider.ratesTo(reportingCurrency);
    }

    /** The currencies analytics can report in, for the dashboard's currency picker. */
    public List<String> reportingCurrencies() {
        return List.copyOf(new TreeSet<>(
                exchangeRateProvider.supportedCurrencies()
        ));
    }
}

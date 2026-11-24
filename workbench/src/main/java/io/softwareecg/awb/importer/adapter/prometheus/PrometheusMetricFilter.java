package io.softwareecg.awb.importer.adapter.prometheus;

import io.softwareecg.awb.importer.model.RawSeriesData;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.regex.Pattern;

/**
 * A filter that compares the Prometheus series names with the configured
 * whitelist expressions. The filter will only let pass a series than at least
 * on of the defined filters matches to the series name.
 *
 * @since 31.07.2018
 */
public class PrometheusMetricFilter implements Predicate<String> {

    private final List<Pattern> regexFilterPatterns = new ArrayList<>();

    private final List<String> stdFilterExpressions;

    /**
     * Constructs a new instance of the PrometheusMetricFilter class
     * with pre-initialized filter expressions.
     */
    public PrometheusMetricFilter() {
        List<String> regexFilter = PrometheusModuleConfig.getRegexSeriesFilter();
        regexFilter.forEach(expression -> regexFilterPatterns.add(Pattern.compile(expression)));

        stdFilterExpressions = PrometheusModuleConfig.getStandardSeriesFilter();
    }

    /**
     * Checks if the given RawSeriesData matches to one of the configured
     * filters. If not false will returned, otherwise true.
     *
     * @param rawSeriesData a {@link RawSeriesData} instance that will checked using the property metricName.
     * @return true if the metric name of the RawSeriesData matches to one of the whitelist expression otherwise false
     */
    public boolean test(RawSeriesData rawSeriesData) {
        return test(rawSeriesData.getMetricName());
    }

    /**
     * Checks if the given metric name matches to one of the configured
     * filters. If not false will returned, otherwise true.
     *
     * @param metricName the metric name that will checked using the filter patterns/expressions
     * @return true if the metric name matches to one of the whitelist expression otherwise false
     */
    @Override
    public boolean test(String metricName) {

        if (regexFilterPatterns.isEmpty() && stdFilterExpressions.isEmpty()) {
            return true;
        }

        for (String expression : stdFilterExpressions) {
            if (expression.equals(metricName)) {
                return true;
            }
        }

        for (Pattern filterPattern : regexFilterPatterns) {
            if (filterPattern.matcher(metricName).matches()) {
                return true;
            }
        }

        return false;
    }
}

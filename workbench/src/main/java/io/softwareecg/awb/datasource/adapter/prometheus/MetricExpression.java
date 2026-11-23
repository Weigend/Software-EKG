package io.softwareecg.awb.datasource.adapter.prometheus;

/**
 * One thing to collect from Prometheus.
 *
 * @param name       the metric name the resulting series get in the project. Needed for
 *                   calculations, whose results carry no name of their own; a plain selector
 *                   keeps the Prometheus metric names.
 * @param expression the PromQL expression
 */
public record MetricExpression(String name, String expression) {

    /**
     * @return the metric name the series of this expression get: the given name, or for a plain
     *         selector the metric it selects (empty if it selects several)
     */
    public String seriesName() {
        if (name != null && !name.isBlank()) {
            return name;
        }
        int brace = expression.indexOf('{');
        return brace < 0 ? expression : expression.substring(0, brace);
    }
}

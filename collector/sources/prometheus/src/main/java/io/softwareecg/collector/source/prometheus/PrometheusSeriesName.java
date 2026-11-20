package io.softwareecg.collector.source.prometheus;

import java.util.Map;
import java.util.TreeMap;

/**
 * The name under which the Prometheus source hands one time series to the collector output,
 * in Prometheus' own notation: {@code node_load1{instance="server-01",job="node"}}.
 * <p>
 * A Prometheus job collects the series of many systems through one server. The collector
 * knows only that server as its node, so the identity of each series - which system and
 * process it belongs to - travels in this name. An output that understands Prometheus
 * (the AWB) reads the labels back with {@link #parse(String)}; any other output simply
 * uses the text as the metric label.
 *
 * @param metric the metric name
 * @param labels the labels of the series without {@code __name__}, sorted by name
 */
public record PrometheusSeriesName(String metric, Map<String, String> labels) {

    /**
     * The label Prometheus keeps the metric name in.
     */
    public static final String NAME_LABEL = "__name__";

    public PrometheusSeriesName {
        labels = java.util.Collections.unmodifiableMap(new TreeMap<>(labels));
    }

    /**
     * Builds the name of a result series.
     *
     * @param fallbackMetric the name to use if the series carries none, which is the case for
     *                       the result of most expressions (rates, aggregations)
     * @param seriesLabels   the labels of the series as Prometheus returned them
     */
    public static PrometheusSeriesName of(String fallbackMetric, Map<String, String> seriesLabels) {
        Map<String, String> labels = new TreeMap<>(seriesLabels);
        String metric = labels.remove(NAME_LABEL);
        if (metric == null || metric.isBlank()) {
            metric = fallbackMetric == null || fallbackMetric.isBlank() ? "value" : sanitize(fallbackMetric.trim());
        }
        return new PrometheusSeriesName(metric, labels);
    }

    /**
     * A metric definition without label falls back to its expression text. That may contain
     * braces and quotes, which would make the name unreadable for {@link #parse(String)}.
     */
    private static String sanitize(String metric) {
        return metric.replaceAll("[^A-Za-z0-9_:.\\-]+", "_");
    }

    /**
     * Reads a name written by {@link #toString()}.
     *
     * @return the parsed name; a text without labels yields a name with an empty label map
     */
    public static PrometheusSeriesName parse(String text) {
        int brace = text.indexOf('{');
        if (brace < 0 || !text.endsWith("}")) {
            return new PrometheusSeriesName(text, Map.of());
        }
        Map<String, String> labels = new TreeMap<>();
        int pos = brace + 1;
        int end = text.length() - 1;
        while (pos < end) {
            int equals = text.indexOf("=\"", pos);
            if (equals < 0) {
                break;
            }
            String name = text.substring(pos, equals).trim();
            StringBuilder value = new StringBuilder();
            pos = equals + 2;
            while (pos < end) {
                char c = text.charAt(pos++);
                if (c == '\\' && pos < end) {
                    char escaped = text.charAt(pos++);
                    value.append(escaped == 'n' ? '\n' : escaped);
                } else if (c == '"') {
                    break;
                } else {
                    value.append(c);
                }
            }
            labels.put(name, value.toString());
            if (pos < end && text.charAt(pos) == ',') {
                pos++;
            }
        }
        return new PrometheusSeriesName(text.substring(0, brace), labels);
    }

    @Override
    public String toString() {
        if (labels.isEmpty()) {
            return metric;
        }
        StringBuilder sb = new StringBuilder(metric).append('{');
        boolean first = true;
        for (Map.Entry<String, String> label : labels.entrySet()) {
            if (!first) {
                sb.append(',');
            }
            first = false;
            sb.append(label.getKey()).append("=\"")
                    .append(label.getValue().replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n"))
                    .append('"');
        }
        return sb.append('}').toString();
    }
}

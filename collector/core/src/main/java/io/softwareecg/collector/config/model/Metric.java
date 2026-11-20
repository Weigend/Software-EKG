package io.softwareecg.collector.config.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * One metric, or - with wildcards - all metrics of the source that match.
 * <p>
 * In the file a metric is usually just its text: {@code '\Load\one'}. With a label or one of the
 * other settings it is written as a map: <code>{metric: 'up{job="node"}', label: up}</code>.
 *
 * @param metric          the metric in the notation of its source: a counter path, a JMX name,
 *                        a PromQL expression
 * @param label           the name shown instead of the metric, or {@code null}. Not useful
 *                        together with wildcards, as all matches would get the same name.
 * @param regexPostFilter a regular expression the names of the matches are filtered with; a
 *                        group of it becomes the label. Or {@code null}.
 * @param wildcard        {@code false} to take the metric literally; {@code null} for the default
 */
public record Metric(String metric, String label, String regexPostFilter, Boolean wildcard) {

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public Metric(@JsonProperty("metric") String metric,
                  @JsonProperty("label") String label,
                  @JsonProperty("regexPostFilter") String regexPostFilter,
                  @JsonProperty("wildcard") Boolean wildcard) {
        this.metric = metric == null ? null : metric.strip();
        this.label = label;
        this.regexPostFilter = regexPostFilter;
        this.wildcard = wildcard;
    }

    /**
     * A metric given by its text alone.
     */
    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static Metric of(String metric) {
        return new Metric(metric, null, null, null);
    }

    /**
     * @return what is written to the file: the text alone, or a map if there is more to say
     */
    @JsonValue
    Object written() {
        if (label == null && regexPostFilter == null && wildcard == null) {
            return metric;
        }
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("metric", metric);
        if (label != null) {
            map.put("label", label);
        }
        if (regexPostFilter != null) {
            map.put("regexPostFilter", regexPostFilter);
        }
        if (wildcard != null) {
            map.put("wildcard", wildcard);
        }
        return map;
    }
}

package io.softwareecg.collector.config.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.util.ArrayList;
import java.util.List;

/**
 * A set of metrics that is collected as a unit.
 *
 * @param type    the source the metrics are read with: {@code unix}, {@code unixio},
 *                {@code unixdf}, {@code prometheus}, {@code jolokia}, {@code jmx}, {@code win} ...
 * @param metrics what is read, in the notation of that source
 */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
@JsonPropertyOrder({"type", "metrics"})
public record MetricGroup(String type, List<Metric> metrics) {

    public MetricGroup {
        metrics = metrics == null ? List.of() : new ArrayList<>(metrics);
    }
}

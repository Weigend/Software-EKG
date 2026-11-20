package io.softwareecg.awb.datasource.bl.prometheus;

import io.softwareecg.awb.datasource.bl.SeriesDimensions;
import io.softwareecg.collector.source.prometheus.PrometheusSeriesName;

import java.util.Map;
import java.util.TreeMap;

/**
 * Places a Prometheus series in the dimensions of the EKG, so that the series of similar
 * systems line up for comparison:
 * <ul>
 *   <li>host group and process group = the Prometheus job (the group of similar systems)</li>
 *   <li>host = the instance, without port</li>
 *   <li>process = the job: the same name on every host, which is what makes
 *       "this process on all hosts" one wildcard</li>
 *   <li>metric group = the first part of the metric name (cpu, memory, node, jvm ...)</li>
 *   <li>metric = the metric name, followed by the remaining labels if there are any</li>
 * </ul>
 * This is the generic model for any Prometheus environment; models for particular platforms
 * (Kubernetes with namespace and pod, for example) can be added as further mappings.
 */
public final class PrometheusDimensions {

    private static final String UNKNOWN_GROUP = "prometheus";

    private PrometheusDimensions() {
    }

    public static SeriesDimensions of(PrometheusSeriesName series) {
        Map<String, String> rest = new TreeMap<>(series.labels());
        String job = rest.remove("job");
        String instance = rest.remove("instance");

        String group = job == null || job.isBlank() ? UNKNOWN_GROUP : job;
        String host = instance == null || instance.isBlank() ? group : withoutPort(instance);
        String metric = rest.isEmpty() ? series.metric() : new PrometheusSeriesName(series.metric(), rest).toString();

        return new SeriesDimensions(group, host, group, group, SeriesDimensions.metricGroupOf(series.metric()), metric);
    }

    static String withoutPort(String instance) {
        int colon = instance.lastIndexOf(':');
        if (colon > 0 && colon < instance.length() - 1 && instance.substring(colon + 1).chars().allMatch(Character::isDigit)
                && instance.indexOf(':') == colon) {
            return instance.substring(0, colon);
        }
        return instance;
    }
}

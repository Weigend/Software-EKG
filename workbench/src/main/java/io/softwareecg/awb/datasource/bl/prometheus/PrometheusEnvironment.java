package io.softwareecg.awb.datasource.bl.prometheus;

import java.util.List;
import java.util.Map;

/**
 * What a Prometheus server offers, as shown to the user right after connecting: the groups
 * of similar systems it monitors and how much data each of them has.
 *
 * @param url    the address of the server
 * @param groups the groups found, largest first
 */
public record PrometheusEnvironment(String url, List<Group> groups) {

    /**
     * A group of similar systems: all targets of one Prometheus job.
     *
     * @param name          the job name
     * @param systems       the monitored systems (the {@code instance} labels), sorted
     * @param seriesCount   the number of time series of the whole group
     * @param seriesByMetric the number of series of each metric of the group, by metric name
     */
    public record Group(String name, List<String> systems, int seriesCount, Map<String, Integer> seriesByMetric) {

        public int metricCount() {
            return seriesByMetric.size();
        }
    }

    public int systemCount() {
        return groups.stream().mapToInt(group -> group.systems().size()).sum();
    }
}

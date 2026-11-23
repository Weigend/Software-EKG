package io.softwareecg.awb.datasource.adapter.prometheus;

import io.softwareecg.awb.datasource.adapter.prometheus.PrometheusEnvironment.Group;
import io.softwareecg.collector.source.prometheus.PrometheusApi;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * Finds out what a Prometheus server monitors. Two aggregating queries are enough, so the
 * picture of the environment is there right after the address was entered - no series or
 * values are transferred.
 */
public final class PrometheusDiscovery {

    private static final String JOB = "job";
    private static final String INSTANCE = "instance";
    private static final String NAME = "__name__";

    private PrometheusDiscovery() {
    }

    /**
     * @param url      the address of the Prometheus server
     * @param username the user for basic authentication, or blank
     * @param password the password, or a bearer token if no user is given, or blank
     * @throws io.softwareecg.collector.source.prometheus.PrometheusException if the server cannot
     *         be reached or rejects the credentials; the message is meant for the user
     */
    public static PrometheusEnvironment discover(String url, String username, String password) {
        PrometheusApi api = new PrometheusApi(url, username, password);

        Map<String, TreeSet<String>> systemsByGroup = new TreeMap<>();
        Map<String, Integer> seriesByGroup = new TreeMap<>();
        for (PrometheusApi.Series series : api.query("count by (job, instance) ({__name__=~\".+\"})")) {
            String group = series.labels().getOrDefault(JOB, "");
            systemsByGroup.computeIfAbsent(group, g -> new TreeSet<>()).add(series.labels().getOrDefault(INSTANCE, ""));
            seriesByGroup.merge(group, count(series), Integer::sum);
        }

        Map<String, Map<String, Integer>> metricsByGroup = new TreeMap<>();
        for (PrometheusApi.Series series : api.query("count by (job, __name__) ({__name__=~\".+\"})")) {
            metricsByGroup.computeIfAbsent(series.labels().getOrDefault(JOB, ""), g -> new TreeMap<>())
                    .put(series.labels().getOrDefault(NAME, ""), count(series));
        }

        List<Group> groups = new ArrayList<>();
        systemsByGroup.forEach((name, systems) -> groups.add(new Group(name, List.copyOf(systems),
                seriesByGroup.getOrDefault(name, 0), Map.copyOf(metricsByGroup.getOrDefault(name, Map.of())))));
        groups.sort(Comparator.comparingInt((Group group) -> group.systems().size()).reversed()
                .thenComparing(Group::name));

        return new PrometheusEnvironment(api.getBaseUrl(), List.copyOf(groups));
    }

    private static int count(PrometheusApi.Series series) {
        if (series.samples().isEmpty() || series.samples().get(0).value() == null) {
            return 0;
        }
        return (int) Math.round(series.samples().get(0).value());
    }
}

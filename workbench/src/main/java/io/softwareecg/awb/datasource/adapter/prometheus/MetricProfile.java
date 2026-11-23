package io.softwareecg.awb.datasource.adapter.prometheus;

import io.softwareecg.awb.datasource.spi.AnalysisTopic;
import io.softwareecg.awb.datasource.adapter.prometheus.PrometheusEnvironment.Group;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Decides what to collect for a group of systems. The point of a profile is that the user
 * does not have to pick from hundreds of metric names: "the essentials" are a handful of
 * comparable series per system, ready for the first chart.
 * <p>
 * Known kinds of systems get a curated list (a template); everything else falls back to a rule.
 */
public final class MetricProfile {

    /**
     * How many metrics the rule-based essentials of an unknown kind of system contain at most.
     */
    private static final int GENERIC_ESSENTIALS = 12;

    private MetricProfile() {
    }

    /**
     * The essentials of a group: few, comparable series per system.
     *
     * @param group the group as discovered
     * @param step  the interval at which values will be collected; rates are calculated over
     *              a window that fits it
     */
    public static List<MetricExpression> essentials(Group group, Duration step) {
        return essentials(group, step, WildcardPattern.ALL);
    }

    /**
     * The essentials of a group for some of its systems.
     *
     * @param group the group as discovered
     * @param step  the interval at which values will be collected
     * @param hosts which systems of the group to collect; it stays a pattern, so systems that
     *              match later are collected as well
     */
    public static List<MetricExpression> essentials(Group group, Duration step, WildcardPattern hosts) {
        // called "job" for what it mostly is: the selector every expression is restricted to
        String job = selector(group, hosts);
        String window = "[" + rateWindowSeconds(step) + "s]";
        Map<String, Integer> metrics = group.seriesByMetric();
        List<MetricExpression> result = new ArrayList<>();

        if (metrics.containsKey("node_cpu_seconds_total")) {
            // Linux/Unix hosts monitored by the node exporter
            result.add(new MetricExpression("cpu_usage_percent",
                    "100 * (1 - avg by (job, instance) (rate(node_cpu_seconds_total{" + job + ",mode=\"idle\"}" + window + ")))"));
            // under the name the load has from every source, not the one of the exporter
            addIfPresent(result, metrics, "node_load1", "node_load1{" + job + "}", "load_1m");
            if (metrics.containsKey("node_memory_MemAvailable_bytes") && metrics.containsKey("node_memory_MemTotal_bytes")) {
                result.add(new MetricExpression("memory_used_percent",
                        "100 * (1 - node_memory_MemAvailable_bytes{" + job + "} / node_memory_MemTotal_bytes{" + job + "})"));
            }
            addIfPresent(result, metrics, "node_disk_read_bytes_total",
                    "sum by (job, instance) (rate(node_disk_read_bytes_total{" + job + "}" + window + "))", "disk_read_bytes_per_second");
            addIfPresent(result, metrics, "node_disk_written_bytes_total",
                    "sum by (job, instance) (rate(node_disk_written_bytes_total{" + job + "}" + window + "))", "disk_written_bytes_per_second");
            addIfPresent(result, metrics, "node_network_receive_bytes_total",
                    "sum by (job, instance) (rate(node_network_receive_bytes_total{" + job + ",device!=\"lo\"}" + window + "))", "network_receive_bytes_per_second");
            addIfPresent(result, metrics, "node_network_transmit_bytes_total",
                    "sum by (job, instance) (rate(node_network_transmit_bytes_total{" + job + ",device!=\"lo\"}" + window + "))", "network_transmit_bytes_per_second");
            if (metrics.containsKey("node_filesystem_avail_bytes") && metrics.containsKey("node_filesystem_size_bytes")) {
                result.add(new MetricExpression("filesystem_used_percent",
                        "100 * (1 - node_filesystem_avail_bytes{" + job + ",mountpoint=\"/\"} / node_filesystem_size_bytes{" + job + ",mountpoint=\"/\"})"));
            }
        }

        String heap = jvmHeapMetric(metrics);
        if (heap != null) {
            // Java processes instrumented with Micrometer, the Prometheus Java client or the
            // JMX exporter; each of them names the same things a little differently
            result.add(new MetricExpression("jvm_heap_used_bytes",
                    "sum by (job, instance) (" + heap + "{" + job + ",area=\"heap\"})"));
            if (metrics.containsKey("jvm_threads_live_threads")) {
                result.add(new MetricExpression(null, "jvm_threads_live_threads{" + job + "}"));
            } else {
                addIfPresent(result, metrics, "jvm_threads_current", "jvm_threads_current{" + job + "}", null);
            }
            if (metrics.containsKey("jvm_gc_pause_seconds_sum")) {
                result.add(new MetricExpression("jvm_gc_pause_seconds_per_second",
                        "sum by (job, instance) (rate(jvm_gc_pause_seconds_sum{" + job + "}" + window + "))"));
            } else {
                addIfPresent(result, metrics, "jvm_gc_collection_seconds_sum",
                        "sum by (job, instance) (rate(jvm_gc_collection_seconds_sum{" + job + "}" + window + "))",
                        "jvm_gc_seconds_per_second");
            }
            addIfPresent(result, metrics, "process_cpu_usage", "process_cpu_usage{" + job + "}", null);
        }

        boolean hostGroup = metrics.containsKey("node_cpu_seconds_total");
        if (!hostGroup && metrics.containsKey("process_cpu_seconds_total")) {
            // any process instrumented with a Prometheus client library (Go, Python, Java ...).
            // Not for a group of hosts: there these metrics describe the exporter, not the host.
            result.add(new MetricExpression("process_cpu_usage_percent",
                    "100 * sum by (job, instance) (rate(process_cpu_seconds_total{" + job + "}" + window + "))"));
            addIfPresent(result, metrics, "process_resident_memory_bytes", "process_resident_memory_bytes{" + job + "}", null);
            addIfPresent(result, metrics, "process_open_fds", "process_open_fds{" + job + "}", null);
            addIfPresent(result, metrics, "go_goroutines", "go_goroutines{" + job + "}", null);
        }

        if (result.isEmpty()) {
            // unknown kind of system: the metrics with the fewest series per system are the
            // ones that describe the system as a whole rather than its parts
            int systems = Math.max(1, group.systems().size());
            metrics.entrySet().stream()
                    .filter(metric -> !metric.getKey().endsWith("_info") && !metric.getKey().endsWith("_bucket"))
                    .filter(metric -> metric.getValue() <= systems)
                    .sorted(Map.Entry.<String, Integer>comparingByValue().thenComparing(Map.Entry.comparingByKey()))
                    .limit(GENERIC_ESSENTIALS)
                    .sorted(Comparator.comparing(Map.Entry::getKey))
                    .forEach(metric -> result.add(new MetricExpression(null, metric.getKey() + "{" + job + "}")));
        }
        // whether each system was reachable at all - the first thing to know about a gap or an outlier
        if (!result.isEmpty() && metrics.containsKey("up")) {
            result.add(0, new MetricExpression(null, "up{" + job + "}"));
        }
        return result;
    }

    /**
     * The topics a group belongs to, recognised by the metrics it has. Only topics that have a
     * template can be recognised; a group none of them fits yields an empty set.
     */
    public static Set<AnalysisTopic> topicsOf(Group group) {
        Set<AnalysisTopic> topics = EnumSet.noneOf(AnalysisTopic.class);
        if (group.seriesByMetric().containsKey("node_cpu_seconds_total")) {
            topics.add(AnalysisTopic.LINUX);
        }
        if (jvmHeapMetric(group.seriesByMetric()) != null) {
            topics.add(AnalysisTopic.JVM);
        }
        return topics;
    }

    /**
     * @return the metric that holds the memory used by a JVM, {@code null} if the group has none
     */
    private static String jvmHeapMetric(Map<String, Integer> metrics) {
        if (metrics.containsKey("jvm_memory_used_bytes")) {
            return "jvm_memory_used_bytes";     // Micrometer, Prometheus Java client 1.x
        }
        if (metrics.containsKey("jvm_memory_bytes_used")) {
            return "jvm_memory_bytes_used";     // JMX exporter and Java client before 1.0
        }
        return null;
    }

    /**
     * Everything the group has, as it is in Prometheus.
     */
    public static List<MetricExpression> everything(Group group) {
        return everything(group, WildcardPattern.ALL);
    }

    /**
     * Everything some systems of the group have.
     */
    public static List<MetricExpression> everything(Group group, WildcardPattern hosts) {
        return List.of(new MetricExpression(null, "{" + selector(group, hosts) + "}"));
    }

    /**
     * The given metrics of the group as they are in Prometheus, in one expression.
     *
     * @param metricNames the names of the metrics; must not be empty
     */
    public static MetricExpression metrics(Group group, WildcardPattern hosts, Collection<String> metricNames) {
        if (metricNames.isEmpty()) {
            throw new IllegalArgumentException("No metrics given.");
        }
        String names = metricNames.stream().sorted().distinct()
                .map(name -> name.replaceAll("[^A-Za-z0-9_:]", "."))
                .collect(Collectors.joining("|"));
        return new MetricExpression(null, "{" + selector(group, hosts) + ",__name__=~\"" + names + "\"}");
    }

    /**
     * The systems of the group a pattern selects. A system is selected if the pattern fits
     * its name with or without the port ({@code db*} and {@code db-0?} both find
     * {@code db-01:9100}).
     */
    public static List<String> systemsMatching(Group group, WildcardPattern hosts) {
        return group.systems().stream()
                .filter(system -> hosts.matches(system) || hosts.matches(PrometheusDimensions.withoutPort(system)))
                .collect(Collectors.toList());
    }

    /**
     * The metrics of the group a pattern selects, sorted by name.
     */
    public static List<String> metricsMatching(Group group, WildcardPattern metricNames) {
        return group.seriesByMetric().keySet().stream().filter(metricNames::matches).sorted()
                .collect(Collectors.toList());
    }

    /**
     * An estimate of how many series the essentials yield, for the hint next to the choice.
     */
    public static int estimatedSeries(Group group, List<MetricExpression> expressions) {
        return estimatedSeries(group, expressions, group.systems().size());
    }

    /**
     * An estimate of how many series the expressions yield for some systems of the group.
     *
     * @param systems the number of systems collected
     */
    public static int estimatedSeries(Group group, List<MetricExpression> expressions, int systems) {
        // a calculation of a template yields one series per system, a plain metric as many as it has
        int total = 0;
        for (MetricExpression expression : expressions) {
            String plain = expression.name() == null
                    ? expression.expression().substring(0, Math.max(0, expression.expression().indexOf('{')))
                    : "";
            total += group.seriesByMetric().containsKey(plain)
                    ? estimatedSeriesOf(group, List.of(plain), systems)
                    : Math.max(1, systems);
        }
        return total;
    }

    /**
     * An estimate of how many series the named metrics have for some systems of the group.
     * The group only knows the number per metric, so the systems are taken to have equal shares.
     */
    public static int estimatedSeriesOf(Group group, Collection<String> metricNames, int systems) {
        int all = Math.max(1, group.systems().size());
        long series = metricNames.stream().mapToLong(name -> group.seriesByMetric().getOrDefault(name, 0)).sum();
        return (int) Math.ceil((double) series * Math.min(all, Math.max(0, systems)) / all);
    }

    /**
     * What restricts an expression to the group and, if not all are wanted, to some of its
     * systems. The pattern of the systems is handed on as a pattern; it may be followed by
     * a port, which a host name typed by the user does not have.
     */
    private static String selector(Group group, WildcardPattern hosts) {
        String selector = "job=\"" + escape(group.name()) + "\"";
        if (!hosts.isAll()) {
            selector += ",instance=~\"" + escape("(?i:(?:" + hosts.regex() + ")(?::[0-9]+)?)") + "\"";
        }
        return selector;
    }

    /**
     * A rate needs at least two samples in its window. Four collection intervals, but not less
     * than two minutes, cover the usual scrape intervals of up to a minute.
     */
    static long rateWindowSeconds(Duration step) {
        return Math.max(120, 4 * step.toSeconds());
    }

    private static void addIfPresent(List<MetricExpression> result, Map<String, Integer> metrics, String metric,
                                     String expression, String name) {
        if (metrics.containsKey(metric)) {
            result.add(new MetricExpression(name, expression));
        }
    }

    private static String escape(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}

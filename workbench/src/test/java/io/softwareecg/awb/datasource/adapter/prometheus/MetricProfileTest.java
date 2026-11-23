package io.softwareecg.awb.datasource.adapter.prometheus;

import io.softwareecg.awb.datasource.spi.AnalysisTopic;
import io.softwareecg.awb.datasource.adapter.prometheus.PrometheusEnvironment.Group;
import org.junit.Test;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import io.softwareecg.awb.datasource.adapter.prometheus.MetricExpression;
import io.softwareecg.awb.datasource.adapter.prometheus.MetricProfile;
import io.softwareecg.awb.datasource.adapter.prometheus.PrometheusEnvironment;
import io.softwareecg.awb.datasource.adapter.prometheus.WildcardPattern;
public class MetricProfileTest {

    @Test
    public void nodeExporterGroupGetsTheCuratedEssentials() {
        Group node = new Group("node", List.of("server-01", "server-02"), 5000, Map.of(
                "node_cpu_seconds_total", 2000, "node_load1", 2,
                "node_memory_MemAvailable_bytes", 2, "node_memory_MemTotal_bytes", 2,
                "node_disk_read_bytes_total", 10, "node_network_receive_bytes_total", 8,
                "process_cpu_seconds_total", 2));   // the exporter's own process: not part of the host essentials

        List<MetricExpression> essentials = MetricProfile.essentials(node, Duration.ofSeconds(60));

        assertEquals(List.of("cpu_usage_percent", "load_1m", "memory_used_percent", "disk_read_bytes_per_second",
                        "network_receive_bytes_per_second"),
                essentials.stream().map(MetricExpression::name).filter(name -> name != null).collect(Collectors.toList()));
        assertTrue("what every source of a host has is named alike, not after the exporter",
                essentials.stream().anyMatch(e -> "load_1m".equals(e.name()) && e.expression().equals("node_load1{job=\"node\"}")));
        assertEquals(List.of("cpu_usage_percent", "load_1m", "memory_used_percent", "disk_read_bytes_per_second",
                        "network_receive_bytes_per_second"),
                essentials.stream().map(MetricExpression::seriesName).collect(Collectors.toList()));
        assertTrue("every expression is restricted to the group",
                essentials.stream().allMatch(e -> e.expression().contains("job=\"node\"")));
        assertTrue("rates use a window that fits the collection interval",
                essentials.get(0).expression().contains("[240s]"));

        // one series per system for each of the five expressions, instead of 5000
        assertEquals(10, MetricProfile.estimatedSeries(node, essentials));
    }

    @Test
    public void unknownKindOfSystemFallsBackToItsSystemWideMetrics() {
        Group custom = new Group("billing", List.of("a", "b", "c"), 400, Map.of(
                "billing_open_invoices", 3, "billing_queue_length", 3, "billing_requests_total", 240,
                "billing_build_info", 3, "billing_latency_bucket", 90));

        List<MetricExpression> essentials = MetricProfile.essentials(custom, Duration.ofSeconds(60));

        assertEquals(List.of("billing_open_invoices{job=\"billing\"}", "billing_queue_length{job=\"billing\"}"),
                essentials.stream().map(MetricExpression::expression).collect(Collectors.toList()));
        assertEquals(6, MetricProfile.estimatedSeries(custom, essentials));
    }

    @Test
    public void instrumentedProcessGetsProcessEssentialsAndItsAvailability() {
        Group service = new Group("orderservice", List.of("a", "b"), 900, Map.of(
                "up", 2, "process_cpu_seconds_total", 2, "process_resident_memory_bytes", 2,
                "go_goroutines", 2, "go_gc_cycles_total", 6, "http_requests_total", 400));

        List<MetricExpression> essentials = MetricProfile.essentials(service, Duration.ofSeconds(60));

        assertEquals(List.of("up{job=\"orderservice\"}",
                        "100 * sum by (job, instance) (rate(process_cpu_seconds_total{job=\"orderservice\"}[240s]))",
                        "process_resident_memory_bytes{job=\"orderservice\"}",
                        "go_goroutines{job=\"orderservice\"}"),
                essentials.stream().map(MetricExpression::expression).collect(Collectors.toList()));
        assertEquals(8, MetricProfile.estimatedSeries(service, essentials));
    }

    @Test
    public void javaProcessesAreRecognisedWhateverInstrumentsThem() {
        Group micrometer = new Group("shop", List.of("a", "b"), 700, Map.of(
                "up", 2, "jvm_memory_used_bytes", 16, "jvm_threads_live_threads", 2,
                "jvm_gc_pause_seconds_sum", 6, "process_cpu_usage", 2));
        Group jmxExporter = new Group("batch", List.of("a"), 300, Map.of(
                "up", 1, "jvm_memory_bytes_used", 2, "jvm_threads_current", 1,
                "jvm_gc_collection_seconds_sum", 2, "process_cpu_seconds_total", 1));

        assertEquals(List.of("up{job=\"shop\"}",
                        "sum by (job, instance) (jvm_memory_used_bytes{job=\"shop\",area=\"heap\"})",
                        "jvm_threads_live_threads{job=\"shop\"}",
                        "sum by (job, instance) (rate(jvm_gc_pause_seconds_sum{job=\"shop\"}[240s]))",
                        "process_cpu_usage{job=\"shop\"}"),
                MetricProfile.essentials(micrometer, Duration.ofSeconds(60)).stream()
                        .map(MetricExpression::expression).collect(Collectors.toList()));

        assertEquals(List.of("up{job=\"batch\"}",
                        "sum by (job, instance) (jvm_memory_bytes_used{job=\"batch\",area=\"heap\"})",
                        "jvm_threads_current{job=\"batch\"}",
                        "sum by (job, instance) (rate(jvm_gc_collection_seconds_sum{job=\"batch\"}[240s]))",
                        "100 * sum by (job, instance) (rate(process_cpu_seconds_total{job=\"batch\"}[240s]))"),
                MetricProfile.essentials(jmxExporter, Duration.ofSeconds(60)).stream()
                        .map(MetricExpression::expression).collect(Collectors.toList()));
        assertEquals("the heap has the same name in both, so that the processes compare",
                "jvm_heap_used_bytes", MetricProfile.essentials(jmxExporter, Duration.ofSeconds(60)).get(1).name());
    }

    @Test
    public void groupsAreAssignedToTheTopicsTheirMetricsShow() {
        Group node = new Group("node", List.of("a"), 10, Map.of("node_cpu_seconds_total", 8, "process_cpu_seconds_total", 1));
        Group shop = new Group("shop", List.of("a"), 10, Map.of("jvm_memory_used_bytes", 8));
        Group batch = new Group("batch", List.of("a"), 10, Map.of("jvm_memory_bytes_used", 2));
        Group custom = new Group("billing", List.of("a"), 10, Map.of("billing_open_invoices", 1));

        assertEquals(Set.of(AnalysisTopic.LINUX), MetricProfile.topicsOf(node));
        assertEquals(Set.of(AnalysisTopic.JVM), MetricProfile.topicsOf(shop));
        assertEquals(Set.of(AnalysisTopic.JVM), MetricProfile.topicsOf(batch));
        assertEquals("what no template fits belongs to no topic", Set.of(), MetricProfile.topicsOf(custom));
    }

    @Test
    public void aHostPatternRestrictsEveryExpressionAndStaysAPattern() {
        Group node = new Group("node", List.of("db-01:9100", "db-02:9100", "web-01:9100"), 900, Map.of(
                "up", 3, "node_cpu_seconds_total", 240, "node_load1", 3));
        WildcardPattern databases = WildcardPattern.parse("db-0?");

        assertEquals("the pattern fits the name with or without the port",
                List.of("db-01:9100", "db-02:9100"), MetricProfile.systemsMatching(node, databases));
        assertEquals(List.of("db-01:9100"), MetricProfile.systemsMatching(node, WildcardPattern.parse("db-01:9100")));

        List<MetricExpression> essentials = MetricProfile.essentials(node, Duration.ofSeconds(60), databases);
        String selector = "job=\"node\",instance=~\"(?i:(?:db-0.)(?::[0-9]+)?)\"";
        assertEquals("up{" + selector + "}", essentials.get(0).expression());
        assertTrue(essentials.stream().allMatch(e -> e.expression().contains(selector)));
        assertEquals(List.of(new MetricExpression(null, "{" + selector + "}")), MetricProfile.everything(node, databases));

        assertEquals("without a pattern nothing is added",
                MetricProfile.essentials(node, Duration.ofSeconds(60)),
                MetricProfile.essentials(node, Duration.ofSeconds(60), WildcardPattern.ALL));
        // up, cpu and load for two of three systems
        assertEquals(6, MetricProfile.estimatedSeries(node, essentials, 2));
    }

    @Test
    public void aRegularExpressionCharacterInAHostNameIsEscapedTwice() {
        Group node = new Group("node", List.of("10.0.0.1:9100"), 10, Map.of("up", 1, "node_cpu_seconds_total", 8));

        // once for the regular expression, once for the string it stands in
        assertEquals("{job=\"node\",instance=~\"(?i:(?:10\\\\.0\\\\.0\\\\.1)(?::[0-9]+)?)\"}",
                MetricProfile.everything(node, WildcardPattern.parse("10.0.0.1")).get(0).expression());
    }

    @Test
    public void furtherMetricsAreSelectedByPatternAndCollectedInOneExpression() {
        Group node = new Group("node", List.of("a", "b"), 500, Map.of(
                "up", 2, "node_filesystem_avail_bytes", 20, "node_filesystem_size_bytes", 20, "node_load1", 2));

        List<String> names = MetricProfile.metricsMatching(node, WildcardPattern.parse("node_filesystem_*"));
        assertEquals(List.of("node_filesystem_avail_bytes", "node_filesystem_size_bytes"), names);
        assertEquals(new MetricExpression(null,
                        "{job=\"node\",__name__=~\"node_filesystem_avail_bytes|node_filesystem_size_bytes\"}"),
                MetricProfile.metrics(node, WildcardPattern.ALL, names));
        assertEquals(40, MetricProfile.estimatedSeriesOf(node, names, 2));
        assertEquals("half the systems, about half the series", 20, MetricProfile.estimatedSeriesOf(node, names, 1));
    }

    @Test
    public void everythingIsOneSelectorForTheWholeGroup() {
        Group node = new Group("no\"de", List.of("x"), 1, Map.of("up", 1));

        assertEquals(List.of(new MetricExpression(null, "{job=\"no\\\"de\"}")), MetricProfile.everything(node));
    }

    @Test
    public void rateWindowNeverGetsShorterThanTwoMinutes() {
        assertEquals(120, MetricProfile.rateWindowSeconds(Duration.ofSeconds(15)));
        assertEquals(1200, MetricProfile.rateWindowSeconds(Duration.ofMinutes(5)));
    }
}

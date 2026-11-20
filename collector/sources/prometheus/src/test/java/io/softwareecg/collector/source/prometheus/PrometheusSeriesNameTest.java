package io.softwareecg.collector.source.prometheus;

import org.junit.Test;

import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class PrometheusSeriesNameTest {

    @Test
    public void usesTheMetricNameOfTheSeriesAndSortsTheLabels() {
        PrometheusSeriesName name = PrometheusSeriesName.of("ignored",
                Map.of("__name__", "node_load1", "job", "node", "instance", "server-01"));

        assertEquals("node_load1{instance=\"server-01\",job=\"node\"}", name.toString());
    }

    @Test
    public void fallsBackToTheGivenNameForExpressionResults() {
        PrometheusSeriesName name = PrometheusSeriesName.of("cpu_usage", Map.of("instance", "server-01"));

        assertEquals("cpu_usage{instance=\"server-01\"}", name.toString());
    }

    @Test
    public void fallbackTakenFromAnExpressionStaysParseable() {
        PrometheusSeriesName name = PrometheusSeriesName.of("sum(rate(http_requests{code=\"200\"}[1m]))", Map.of("job", "api"));

        PrometheusSeriesName parsed = PrometheusSeriesName.parse(name.toString());

        assertEquals(name, parsed);
        assertEquals(Map.of("job", "api"), parsed.labels());
    }

    @Test
    public void parseReadsBackWhatToStringWrote() {
        PrometheusSeriesName name = new PrometheusSeriesName("disk_free",
                Map.of("mountpoint", "/data", "note", "a \"quoted\" value, with comma and \\ backslash", "instance", "db=1"));

        assertEquals(name, PrometheusSeriesName.parse(name.toString()));
    }

    @Test
    public void nameWithoutLabelsIsJustTheMetric() {
        PrometheusSeriesName name = PrometheusSeriesName.of("up", Map.of());

        assertEquals("up", name.toString());
        assertTrue(PrometheusSeriesName.parse("up").labels().isEmpty());
    }
}

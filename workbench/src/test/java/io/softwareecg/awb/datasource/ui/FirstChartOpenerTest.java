package io.softwareecg.awb.datasource.ui;

import io.softwareecg.awb.datasource.adapter.prometheus.MetricExpression;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * Which metric the first chart of a new analysis shows.
 */
public class FirstChartOpenerTest {

    @Test
    public void cpuUsageIsTheFirstPictureOfAGroupOfHosts() {
        assertEquals("cpu_usage_percent", FirstChartOpener.headline(names(
                new MetricExpression(null, "up{job=\"node\"}"),
                new MetricExpression(null, "node_load1{job=\"node\"}"),
                new MetricExpression("cpu_usage_percent", "100 * (1 - avg by (job, instance) (rate(x[2m])))"))));
    }

    @Test
    public void plainMetricIsNamedByItsSelector() {
        assertEquals("node_load1", FirstChartOpener.headline(names(
                new MetricExpression(null, "up{job=\"node\"}"),
                new MetricExpression(null, "node_load1{job=\"node\"}"))));
    }

    @Test
    public void anythingCollectedBeatsTheAvailabilitySeries() {
        assertEquals("billing_queue_length", FirstChartOpener.headline(names(
                new MetricExpression(null, "up{job=\"billing\"}"),
                new MetricExpression(null, "billing_queue_length{job=\"billing\"}"))));
    }

    @Test
    public void everythingSelectionFallsBackToAvailability() {
        assertEquals("up", FirstChartOpener.headline(names(new MetricExpression(null, "{job=\"node\"}"))));
    }

    /** The names the series of the expressions get, which is what the first chart is chosen from. */
    private static List<String> names(MetricExpression... expressions) {
        return java.util.Arrays.stream(expressions).map(MetricExpression::seriesName).toList();
    }
}

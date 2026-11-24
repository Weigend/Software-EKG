package io.softwareecg.awb.datasource.adapter.prometheus;

import io.softwareecg.awb.datasource.model.SeriesDimensions;
import io.softwareecg.collector.source.prometheus.PrometheusSeriesName;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class PrometheusDimensionsTest {

    @Test
    public void jobAndInstanceBecomeGroupAndHost() {
        SeriesDimensions dimensions = PrometheusDimensions.of(
                PrometheusSeriesName.parse("node_load1{instance=\"server-01:9100\",job=\"node\"}"));

        assertEquals("node", dimensions.hostGroup());
        assertEquals("server-01", dimensions.host());
        assertEquals("node", dimensions.processGroup());
        assertEquals("the same process name on every host makes the hosts comparable", "node", dimensions.process());
        assertEquals("node", dimensions.metricGroup());
        assertEquals("node_load1", dimensions.metric());
    }

    @Test
    public void remainingLabelsStayPartOfTheMetric() {
        SeriesDimensions dimensions = PrometheusDimensions.of(PrometheusSeriesName.parse(
                "filesystem_used_percent{device=\"/dev/sda1\",instance=\"server-01\",job=\"node\",mountpoint=\"/\"}"));

        assertEquals("filesystem", dimensions.metricGroup());
        assertEquals("filesystem_used_percent{device=\"/dev/sda1\",mountpoint=\"/\"}", dimensions.metric());
    }

    @Test
    public void seriesWithoutJobAndInstanceStillGetsAPlace() {
        SeriesDimensions dimensions = PrometheusDimensions.of(PrometheusSeriesName.parse("up"));

        assertEquals("prometheus", dimensions.hostGroup());
        assertEquals("prometheus", dimensions.host());
        assertEquals("up", dimensions.metricGroup());
        assertEquals("up", dimensions.metric());
    }

    @Test
    public void ipv6InstanceIsNotCutAtAColon() {
        SeriesDimensions dimensions = PrometheusDimensions.of(
                new PrometheusSeriesName("up", java.util.Map.of("instance", "[::1]:9100", "job", "node")));

        assertEquals("[::1]:9100", dimensions.host());
    }
}

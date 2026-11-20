package io.softwareecg.collector.source.prometheus;

import io.softwareecg.collector.config.CounterDescriptor;
import io.softwareecg.collector.dt.MetricDefinition;
import io.softwareecg.collector.source.CounterSeries;
import io.softwareecg.collector.source.ExpandWildcardResponse;
import io.softwareecg.collector.source.ExpandWildcardResultType;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class PrometheusCounterSourceTest {

    private static final String LOAD = "node_load1{job=\"node\"}";
    private static final String CPU = "100 * (1 - avg by (instance) (rate(node_cpu_seconds_total{mode=\"idle\"}[1m])))";

    /** 2026-11-19 12:00:00.500 UTC - deliberately not on the minute grid. */
    private static final long NOW = 1_795_089_600_500L;
    private static final long MINUTE = 60_000;

    private FakePrometheus prometheus;

    private final PrometheusCounterSource source = new PrometheusCounterSource(() -> NOW);

    @Before
    public void startPrometheus() throws Exception {
        prometheus = new FakePrometheus()
                .expression(LOAD, List.of(
                        Map.of("__name__", "node_load1", "job", "node", "instance", "server-02"),
                        Map.of("__name__", "node_load1", "job", "node", "instance", "server-01")))
                .expression(CPU, List.of(
                        Map.of("instance", "server-01"),
                        Map.of("instance", "server-02")));
    }

    @After
    public void stopPrometheus() {
        prometheus.close();
    }

    @Test
    public void expressionsAreResolvedToOneMetricPerSeries() {
        ExpandWildcardResponse response = source.replaceWildcardCounter(job(null));

        assertEquals(ExpandWildcardResultType.SUCCESSFUL_EXPANDED, response.getResultType());
        assertEquals(List.of(
                        "node_load1{instance=\"server-01\",job=\"node\"}",
                        "node_load1{instance=\"server-02\",job=\"node\"}",
                        "cpu_usage{instance=\"server-01\"}",
                        "cpu_usage{instance=\"server-02\"}"),
                labels(response.getExpandedDefinitions()));
        assertEquals("a resolved metric keeps the expression it came from",
                CPU, response.getExpandedDefinitions()[2].getDefinition());
    }

    @Test
    public void expressionWithoutSeriesIsReportedAsNotFound() {
        CounterDescriptor job = descriptor(null, new MetricDefinition("no_such_metric", "nothing"));

        assertEquals(ExpandWildcardResultType.METRIC_NOT_FOUND, source.replaceWildcardCounter(job).getResultType());
    }

    @Test
    public void firstFillReachesBackByTheConfiguredLookbackOnTheStepGrid() {
        CounterDescriptor job = resolved(job("lookback=10m"));
        List<CounterSeries> rows = new ArrayList<>();

        assertTrue(source.queryCounterSince(job, null, rows::add));

        long newest = (NOW / MINUTE) * MINUTE;              // 12:00:00
        long oldest = newest - 9 * MINUTE;                  // first grid point within the last 10 minutes
        assertEquals(10, rows.size());
        assertEquals(new Date(oldest), rows.get(0).getDate());
        assertEquals(new Date(newest), rows.get(9).getDate());

        // one value per resolved metric, in their order; the fake encodes series index and time in the value
        assertEquals(List.of(
                        FakePrometheus.valueOf(1, oldest),   // node_load1 server-01 is the second series of LOAD
                        FakePrometheus.valueOf(0, oldest),   // node_load1 server-02 the first
                        FakePrometheus.valueOf(0, oldest),   // cpu_usage server-01
                        FakePrometheus.valueOf(1, oldest)),  // cpu_usage server-02
                rows.get(0).getValues());

        FakePrometheus.Request range = prometheus.requests.stream()
                .filter(r -> r.path().endsWith("query_range")).findFirst().orElseThrow();
        assertEquals("60.000", range.form().get("step"));
    }

    @Test
    public void laterCyclesDeliverOnlyWhatIsNewer() {
        CounterDescriptor job = resolved(job("lookback=10m"));
        long newest = (NOW / MINUTE) * MINUTE;
        List<CounterSeries> rows = new ArrayList<>();

        assertTrue(source.queryCounterSince(job, new Date(newest - 3 * MINUTE), rows::add));

        assertEquals(3, rows.size());
        assertEquals(new Date(newest - 2 * MINUTE), rows.get(0).getDate());
        assertEquals(new Date(newest), rows.get(2).getDate());
    }

    @Test
    public void nothingNewIsNotAnError() {
        CounterDescriptor job = resolved(job(null));
        long newest = (NOW / MINUTE) * MINUTE;
        List<CounterSeries> rows = new ArrayList<>();

        assertTrue(source.queryCounterSince(job, new Date(newest), rows::add));

        assertTrue(rows.isEmpty());
    }

    @Test
    public void withoutLookbackCollectingStartsNow() {
        CounterDescriptor job = resolved(job(null));
        List<CounterSeries> rows = new ArrayList<>();

        assertTrue(source.queryCounterSince(job, null, rows::add));

        assertTrue("at most the current grid point", rows.size() <= 1);
    }

    @Test
    public void seriesThatStartsLaterHasNoValuesBefore() {
        long newest = (NOW / MINUTE) * MINUTE;
        prometheus.startsAt(CPU, 1, newest - 2 * MINUTE);   // cpu_usage of server-02 appears late
        CounterDescriptor job = resolved(job("lookback=5m"));
        List<CounterSeries> rows = new ArrayList<>();

        assertTrue(source.queryCounterSince(job, null, rows::add));

        assertEquals(5, rows.size());
        assertNull(rows.get(0).getValues().get(3));
        assertEquals(FakePrometheus.valueOf(1, newest), rows.get(4).getValues().get(3), 0d);
    }

    @Test
    public void unreachableServerIsReportedAsFailure() {
        CounterDescriptor job = resolved(job("lookback=5m"));
        prometheus.statusCode = 503;

        assertFalse(source.queryCounterSince(job, null, row -> { }));
        assertTrue(source.queryCounter(job).getValues().isEmpty());
        ExpandWildcardResponse expansion = source.replaceWildcardCounter(job(null));
        assertEquals("a server that does not answer is not 'no such metric'",
                ExpandWildcardResultType.METRIC_SOURCE_NOT_AVAILABLE, expansion.getResultType());
        assertTrue(expansion.getMessage(), expansion.getMessage().contains("did not answer the query"));
    }

    @Test
    public void currentValuesComeAsOneValueSet() {
        CounterDescriptor job = resolved(job(null));

        CounterSeries current = source.queryCounter(job);

        assertEquals(4, current.getValues().size());
        assertEquals(new Date(NOW), current.getDate());
    }

    @Test
    public void credentialsAreSentAsBasicAuthentication() {
        CounterDescriptor job = job(null);
        when(job.getUsername()).thenReturn("analyst");
        when(job.getPassword()).thenReturn("secret");

        source.replaceWildcardCounter(job);

        String expected = "Basic " + Base64.getEncoder().encodeToString("analyst:secret".getBytes());
        assertEquals(expected, prometheus.requests.get(0).authorization());
    }

    @Test
    public void lookbackArgumentIsParsed() {
        assertEquals(0, PrometheusCounterSource.lookbackMillis(job(null)));
        assertEquals(24 * 3_600_000L, PrometheusCounterSource.lookbackMillis(job("lookback=24h")));
        assertEquals(90_000L, PrometheusCounterSource.lookbackMillis(job("other=1 lookback=90s")));
        assertEquals(7 * 86_400_000L, PrometheusCounterSource.lookbackMillis(job("lookback=7d,foo=bar")));
        assertEquals(0, PrometheusCounterSource.lookbackMillis(job("nolookback=5m")));
    }

    //-----------------------------------------------------------------------------------------------------------

    /** The job as configured: two expressions. */
    private CounterDescriptor job(String arguments) {
        return descriptor(arguments, new MetricDefinition(LOAD, null), new MetricDefinition(CPU, "cpu_usage"));
    }

    /** The job as the collector passes it on after resolving the expressions. */
    private CounterDescriptor resolved(CounterDescriptor configured) {
        MetricDefinition[] expanded = source.replaceWildcardCounter(configured).getExpandedDefinitions();
        when(configured.getMetricDefinitions()).thenReturn(expanded);
        prometheus.requests.clear();
        return configured;
    }

    private CounterDescriptor descriptor(String arguments, MetricDefinition... definitions) {
        CounterDescriptor descriptor = mock(CounterDescriptor.class);
        when(descriptor.getNodeAddress()).thenReturn(prometheus.address());
        when(descriptor.getMetricGroupId()).thenReturn("node-basics");
        when(descriptor.getMetricFetchInterval()).thenReturn(MINUTE);
        when(descriptor.getAdditionalArguments()).thenReturn(arguments);
        when(descriptor.getMetricDefinitions()).thenReturn(definitions);
        return descriptor;
    }

    private static List<String> labels(MetricDefinition[] definitions) {
        List<String> labels = new ArrayList<>();
        for (MetricDefinition definition : definitions) {
            labels.add(definition.getLabel());
        }
        return labels;
    }
}

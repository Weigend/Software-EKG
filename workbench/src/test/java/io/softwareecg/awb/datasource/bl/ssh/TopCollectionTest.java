package io.softwareecg.awb.datasource.bl.ssh;

import io.softwareecg.awb.datasource.bl.ProjectSeriesOutput;
import io.softwareecg.awb.sdk.awbapi.repository.Repository;
import io.softwareecg.awb.sdk.awbapi.repository.SeriesImportService;
import io.softwareecg.awb.sdk.datamodel.TimeSeries;
import io.softwareecg.collector.config.CollectorConfigurations;
import io.softwareecg.collector.control.CollectorEngine;
import org.junit.After;
import org.junit.Assume;
import org.junit.Test;

import java.io.File;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * The whole way of a host read with top: plan, collector configuration, a real collector
 * engine reading this machine, and the series as they arrive in the project.
 */
public class TopCollectionTest {

    private CollectorEngine engine;

    @After
    public void stop() {
        if (engine != null) {
            engine.stop();
        }
    }

    @Test
    public void thisMachineIsCollectedUnderTheNamesOfTheAnalysis() throws Exception {
        Assume.assumeTrue("needs the Unix command top", new File("/usr/bin/top").exists() || new File("/bin/top").exists());

        SshPlan plan = new SshPlan("Zoo", "linux", SshPlan.parseHosts("localhost"), null, null,
                List.of("java"),     // this test runs in one
                List.of(TopMetric.CPU_USAGE, TopMetric.LOAD_1M, TopMetric.MEMORY_USED_PERCENT, TopMetric.SWAP_USED_PERCENT),
                List.of(TopMetric.PROCESS_CPU, TopMetric.PROCESS_MEMORY), Duration.ofSeconds(1));

        Written written = new Written();
        ProjectSeriesOutput output = new ProjectSeriesOutput("Zoo", "live", written, (project, measurement, series) -> 0,
                newest -> { });
        engine = new CollectorEngine(output, true);
        engine.start(CollectorConfigurations.fromString(plan.toCollectorConfig("Zoo")));

        long deadline = System.currentTimeMillis() + 40_000;
        while (written.series().size() < 6 && System.currentTimeMillis() < deadline) {
            Thread.sleep(200);
        }
        if (written.series().size() < 6) {
            fail("only " + written.series() + " arrived; status: " + engine.getStatus());
        }

        // the process says whose values they are; the metric is the same for the host and a process
        assertEquals(Set.of("system/cpu_usage_percent", "system/load_1m", "system/memory_used_percent",
                "system/swap_used_percent", "java/cpu_usage_percent", "java/memory_used_percent"), written.series());

        TimeSeries cpu = written.first("system", "cpu_usage_percent");
        assertEquals("Zoo", cpu.getProject());
        assertEquals("live", cpu.getMeasurement());
        assertEquals("linux", cpu.getHostGroup());
        assertEquals("localhost", cpu.getHost());
        assertEquals("the host as a whole", "system", cpu.getProcess());
        assertEquals("cpu", cpu.getMetricGroup());

        TimeSeries process = written.first("java", "cpu_usage_percent");
        assertEquals("localhost", process.getHost());
        assertEquals("linux", process.getHostGroup());
        assertEquals("it stands beside the host's own value, in the same group of metrics", "cpu", process.getMetricGroup());

        Map<String, Double> values = written.lastValues("system");
        assertTrue("a share of the CPU: " + values, values.get("cpu_usage_percent") >= 0 && values.get("cpu_usage_percent") <= 100);
        assertTrue("memory is in use: " + values, values.get("memory_used_percent") > 0 && values.get("memory_used_percent") <= 100);
        assertTrue(values.toString(), values.get("load_1m") >= 0);
    }

    /** Records what is written to the project. */
    private static final class Written implements SeriesImportService {

        private final List<TimeSeries> chunks = new CopyOnWriteArrayList<>();

        /** The series written, as process and metric. */
        Set<String> series() {
            return chunks.stream().map(chunk -> chunk.getProcess() + "/" + chunk.getMetricName())
                    .collect(Collectors.toCollection(TreeSet::new));
        }

        TimeSeries first(String process, String metric) {
            return chunks.stream()
                    .filter(chunk -> process.equals(chunk.getProcess()) && metric.equals(chunk.getMetricName()))
                    .findFirst().orElseThrow();
        }

        Map<String, Double> lastValues(String process) {
            Map<String, Double> values = new TreeMap<>();
            for (TimeSeries chunk : chunks) {
                if (process.equals(chunk.getProcess())) {
                    values.put(chunk.getMetricName(), chunk.getValues().get(chunk.getValues().size() - 1).getValue());
                }
            }
            return values;
        }

        @Override
        public Repository getConfiguredRepository() {
            return null;
        }

        @Override
        public long resolveLastUpdate(TimeSeries seriesWithoutValues) {
            return -1;
        }

        @Override
        public void add(TimeSeries series) {
            chunks.add(series);
        }

        @Override
        public void add(Stream<TimeSeries> series) {
            series.forEach(chunks::add);
        }

        @Override
        public void add(Iterable<TimeSeries> series) {
            series.forEach(chunks::add);
        }

        @Override
        public void commit() {
        }
    }
}

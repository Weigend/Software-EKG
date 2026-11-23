package io.softwareecg.awb.datasource.adapter.windows;

import io.softwareecg.awb.datasource.adapter.collector.ProjectSeriesOutput;
import io.softwareecg.awb.datasource.adapter.ssh.SshPlan;
import io.softwareecg.awb.repository.api.Repository;
import io.softwareecg.awb.timeseries.port.TimeSeriesWriteService;
import io.softwareecg.awb.timeseries.TimeSeries;
import io.softwareecg.collector.config.CollectorConfigurations;
import io.softwareecg.collector.control.CollectorEngine;
import org.junit.After;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Test;

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
 * The whole way of a host read through its Windows performance counters: looking at the host,
 * plan, collector configuration, a real collector engine reading this machine, and the series
 * as they arrive in the project.
 * <p>
 * Runs on Windows with the native library of the counters (profile {@code wpc}); with the
 * system property {@code wpc.required} a library that is missing is a failure instead of a
 * reason to skip.
 */
public class WindowsCollectionTest {

    private CollectorEngine engine;

    @After
    public void stop() {
        if (engine != null) {
            engine.stop();
        }
    }

    private static void assumeCounters() {
        Assume.assumeTrue("not on Windows", System.getProperty("os.name", "").startsWith("Win"));
        String problem = WindowsDiscovery.problem();
        if (Boolean.getBoolean("wpc.required")) {
            Assert.assertNull("java.library.path=" + System.getProperty("java.library.path"), problem);
        }
        Assume.assumeTrue(problem, problem == null);
    }

    @Test
    public void thisMachineIsLookedAt() {
        assumeCounters();

        List<WindowsDiscovery.HostCheck> checks = WindowsDiscovery.inspect(SshPlan.parseHosts("localhost"));

        assertEquals(1, checks.size());
        assertTrue(checks.get(0).problem(), checks.get(0).reachable());
        List<String> programs = checks.get(0).processes().stream()
                .map(WindowsDiscovery.ProcessCandidate::name).collect(Collectors.toList());
        assertTrue("nothing runs here? " + programs, !programs.isEmpty() && !programs.contains("_Total"));
        assertTrue("the biggest first: " + checks.get(0).processes(),
                checks.get(0).processes().get(0).memoryBytes() > 0);
    }

    @Test
    public void thisMachineIsCollectedUnderTheNamesOfTheAnalysis() throws Exception {
        assumeCounters();

        WindowsPlan plan = new WindowsPlan("Zoo", "windows", SshPlan.parseHosts("localhost"),
                List.of("java"),     // this test runs in one
                List.of(WindowsMetric.CPU_USAGE, WindowsMetric.MEMORY_AVAILABLE, WindowsMetric.MEMORY_COMMITTED,
                        WindowsMetric.DISK_FREE, WindowsMetric.TASKS_TOTAL),
                List.of(WindowsMetric.PROCESS_CPU, WindowsMetric.PROCESS_MEMORY),
                List.of("\\System\\Threads", "\\Processor(*)\\% Idle Time"), Duration.ofSeconds(1));

        Written written = new Written();
        ProjectSeriesOutput output = new ProjectSeriesOutput("Zoo", "live", written, (project, measurement, series) -> 0,
                newest -> { });
        engine = new CollectorEngine(output, true);
        engine.start(CollectorConfigurations.fromString(plan.toCollectorConfig("Zoo")));

        Set<String> expected = Set.of("system/cpu_usage_percent", "system/memory_available_bytes",
                "system/memory_committed_percent", "system/disk_free_percent", "system/tasks_total",
                "system/\\System\\Threads", "java/cpu_usage_percent", "java/memory_used_bytes");
        long deadline = System.currentTimeMillis() + 60_000;
        while (!written.series().containsAll(expected) && System.currentTimeMillis() < deadline) {
            Thread.sleep(200);
        }
        if (!written.series().containsAll(expected)) {
            fail("only " + written.series() + " arrived; status: " + engine.getStatus());
        }
        // a rate has its first value at the second look
        Thread.sleep(3_000);

        TimeSeries cpu = written.first("system", "cpu_usage_percent");
        assertEquals("Zoo", cpu.getProject());
        assertEquals("live", cpu.getMeasurement());
        assertEquals("windows", cpu.getHostGroup());
        assertEquals("localhost", cpu.getHost());
        assertEquals("the host as a whole", "system", cpu.getProcess());
        assertEquals("cpu", cpu.getMetricGroup());

        TimeSeries process = written.first("java", "cpu_usage_percent");
        assertEquals("localhost", process.getHost());
        assertEquals("it stands beside the host's own value, in the same group of metrics", "cpu", process.getMetricGroup());

        Map<String, Double> values = written.lastValues("system");
        assertTrue("a share of the CPU: " + values, values.get("cpu_usage_percent") >= 0 && values.get("cpu_usage_percent") <= 100);
        assertTrue("memory is available: " + values, values.get("memory_available_bytes") > 0);
        assertTrue("memory is in use: " + values,
                values.get("memory_committed_percent") > 0 && values.get("memory_committed_percent") <= 100);
        assertTrue("a share of the disks: " + values, values.get("disk_free_percent") >= 0 && values.get("disk_free_percent") <= 100);
        assertTrue("processes run: " + values, values.get("tasks_total") > 1);
        assertTrue("a counter named by the user: " + values, values.get("\\System\\Threads") > 1);
        assertTrue("this JVM uses memory: " + written.lastValues("java"), written.lastValues("java").get("memory_used_bytes") > 0);

        // the wildcard of the user is resolved into the processors of this machine
        Set<String> idle = written.series().stream().filter(name -> name.contains("(") && name.startsWith("system/\\"))
                .collect(Collectors.toCollection(TreeSet::new));
        assertTrue("the idle time of each processor: " + written.series(), idle.size() >= 2);
    }

    /** Records what is written to the project. */
    private static final class Written implements TimeSeriesWriteService {

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
        public String getRepositoryId() {
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

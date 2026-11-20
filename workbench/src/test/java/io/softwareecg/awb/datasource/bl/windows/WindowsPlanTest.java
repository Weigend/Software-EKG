package io.softwareecg.awb.datasource.bl.windows;

import io.softwareecg.awb.datasource.bl.ssh.SshPlan;
import io.softwareecg.collector.config.CollectorConfiguration;
import io.softwareecg.collector.config.CollectorConfigurations;
import io.softwareecg.collector.dt.ClassicCollectorJobConfiguration;
import io.softwareecg.collector.dt.MetricDefinition;
import org.junit.Test;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class WindowsPlanTest {

    @Test
    public void countersAreReadAsTheUserTypesThem() {
        assertEquals(List.of("\\Memory\\Cache Bytes", "\\LogicalDisk(*)\\% Free Space", "\\Process(java#1)\\Thread Count"),
                WindowsPlan.parseCounters("\\Memory\\Cache Bytes, \\LogicalDisk(*)\\% Free Space\n"
                        + " \\Process(java#1)\\Thread Count ; \\Memory\\Cache Bytes ;; "));
        assertTrue(WindowsPlan.parseCounters(null).isEmpty());
        assertTrue(WindowsPlan.parseCounters("   ").isEmpty());

        assertTrue(WindowsPlan.isCounter("\\Memory\\Available Bytes"));
        assertTrue(WindowsPlan.isCounter("\\Processor(_Total)\\% Processor Time"));
        assertFalse("a name is no path", WindowsPlan.isCounter("Memory"));
        assertFalse("an object has no value", WindowsPlan.isCounter("\\Memory"));
        assertFalse(WindowsPlan.isCounter("\\Memory\\"));
        assertFalse("the machine is that of the host", WindowsPlan.isCounter("\\\\server\\Memory\\Available Bytes"));
        assertEquals("what is no counter is left out",
                List.of("\\Memory\\Available Bytes"), WindowsPlan.parseCounters("cpu, \\Memory\\Available Bytes"));
    }

    @Test
    public void thePlanIsMultipliedOutByTheCollector() throws Exception {
        WindowsPlan plan = new WindowsPlan("Zoo & <Co>", "windows-servers", SshPlan.parseHosts("srv-[01-03]"),
                List.of("java", "sqlservr"),
                List.of(WindowsMetric.CPU_USAGE, WindowsMetric.MEMORY_AVAILABLE, WindowsMetric.NETWORK_RECEIVE),
                List.of(WindowsMetric.PROCESS_CPU, WindowsMetric.PROCESS_MEMORY),
                List.of("\\Memory\\Cache Bytes", "\\LogicalDisk(*)\\% Free Space"), Duration.ofSeconds(15));

        assertEquals("three hosts, each once as a whole and once per process", 9, plan.jobCount());
        assertEquals(3 * (3 + 2 + 2 * 2), plan.seriesCount());
        assertTrue("how many network interfaces and disks there are is not known yet", plan.isOpenEnded());
        assertEquals("srv-01, srv-02, srv-03", plan.endpoint());
        assertEquals("win", plan.sourceType());
        assertNull("the counters are read with the login of the user", plan.secret());
        assertEquals("windows-servers", plan.firstHostGroup());
        assertEquals("a series for each interface has no name that is known beforehand",
                List.of("cpu_usage_percent", "memory_available_bytes"), plan.firstMetrics());

        CollectorConfiguration config = CollectorConfigurations.fromString(plan.toCollectorConfig("Zoo & <Co>"));
        List<ClassicCollectorJobConfiguration> jobs = config.getClassicCollectorJobConfiguration();
        assertEquals(9, jobs.size());

        ClassicCollectorJobConfiguration system = jobs.stream()
                .filter(job -> "srv-02".equals(job.getNodeName()) && "system".equals(job.getProcessLabel()))
                .findFirst().orElseThrow();
        assertEquals("win", system.getCounterGroupType());
        assertEquals("windows-servers", system.getNodeGroupId());
        assertEquals("srv-02", system.getNodeAddress());
        assertEquals(15_000, system.getMetricFetchInterval());
        assertEquals("Zoo & <Co>", system.getTimeSeriesHeader().getEkgProjectName());

        MetricDefinition[] metrics = system.getMetricDefinitions();
        assertEquals("the series are named by what they are; a counter the user named is called as it is",
                List.of("cpu_usage_percent", "memory_available_bytes", "network_receive_bytes_per_second ${FILTER:1}",
                        "\\Memory\\Cache Bytes", "\\LogicalDisk(*)\\% Free Space"),
                Arrays.stream(metrics).map(MetricDefinition::getLabel).collect(Collectors.toList()));
        assertEquals("the counters are written in English; the collector reads them on every Windows",
                "\\Processor(_Total)\\% Processor Time", metrics[0].getDefinition());
        assertEquals("only what stands for several counters is resolved",
                List.of(false, false, true, false, true),
                Arrays.stream(metrics).map(MetricDefinition::useWildcard).collect(Collectors.toList()));

        ClassicCollectorJobConfiguration process = jobs.stream()
                .filter(job -> "srv-03".equals(job.getNodeName()) && "sqlservr".equals(job.getProcessLabel()))
                .findFirst().orElseThrow();
        assertEquals("the metric has the name it has for the host: whose it is says the process",
                "cpu_usage_percent", process.getMetricDefinitions()[0].getLabel());
        assertEquals("the process of the job stands in the counter",
                "\\Process(sqlservr)\\% Processor Time", process.getMetricDefinitions()[0].getDefinition());
        assertEquals("memory_used_bytes", process.getMetricDefinitions()[1].getLabel());
    }

    @Test
    public void theInstanceOfACounterNamesItsSeriesInEveryLanguage() throws Exception {
        WindowsPlan plan = new WindowsPlan("Zoo", "windows", SshPlan.parseHosts("localhost"), List.of(),
                List.of(WindowsMetric.NETWORK_TRANSMIT), List.of(), List.of(), Duration.ofSeconds(15));
        MetricDefinition metric = CollectorConfigurations.fromString(plan.toCollectorConfig("Zoo"))
                .getClassicCollectorJobConfiguration().get(0).getMetricDefinitions()[0];

        Pattern filter = Pattern.compile(metric.getRegexPostFilterExp());
        Matcher english = filter.matcher("\\\\SRV-01\\Network Interface(Intel[R] 82574L Gigabit)\\Bytes Sent/sec");
        assertTrue(english.matches());
        assertEquals("Intel[R] 82574L Gigabit", english.group(1));
        Matcher german = filter.matcher("\\\\SRV-01\\Netzwerkschnittstelle(Realtek PCIe GbE)\\Bytes gesendet/s");
        assertTrue(german.matches());
        assertEquals("Realtek PCIe GbE", german.group(1));
        assertFalse("a counter without an instance is none of them", filter.matcher("\\\\SRV-01\\Memory\\Available Bytes").matches());
    }

    @Test
    public void withoutProcessesOnlyTheHostsAreRead() throws Exception {
        WindowsPlan plan = new WindowsPlan("Zoo", "windows", SshPlan.parseHosts("localhost"), List.of(),
                List.of(WindowsMetric.CPU_USAGE), List.of(WindowsMetric.PROCESS_CPU), List.of(), Duration.ofSeconds(5));

        assertEquals(1, plan.jobCount());
        assertEquals(1, plan.seriesCount());
        assertFalse(plan.isOpenEnded());
        assertTrue(plan.isComplete());
        String yaml = plan.toCollectorConfig("Zoo");
        assertFalse(yaml.contains("username"));
        assertFalse(yaml.contains("password"));
        assertEquals(1, CollectorConfigurations.fromString(yaml).getClassicCollectorJobConfiguration().size());

        assertFalse("no host, nothing to collect", new WindowsPlan("Zoo", "windows", List.of(), List.of(),
                List.of(WindowsMetric.CPU_USAGE), List.of(), List.of(), Duration.ofSeconds(5)).isComplete());
        assertFalse("nothing selected, nothing to collect", new WindowsPlan("Zoo", "windows", SshPlan.parseHosts("localhost"),
                List.of("java"), List.of(), List.of(), List.of(), Duration.ofSeconds(5)).isComplete());
    }

    @Test
    public void countersAloneAreAPlan() throws Exception {
        WindowsPlan plan = new WindowsPlan("Zoo", "windows", SshPlan.parseHosts("localhost"), List.of(),
                List.of(), List.of(), List.of("\\Memory\\Cache Bytes"), Duration.ofSeconds(5));

        assertTrue(plan.isComplete());
        assertEquals(1, plan.jobCount());
        assertEquals("the first chart shows the counter", List.of("\\Memory\\Cache Bytes"), plan.firstMetrics());
        assertEquals(1, CollectorConfigurations.fromString(plan.toCollectorConfig("Zoo"))
                .getClassicCollectorJobConfiguration().get(0).getMetricDefinitions().length);
    }
}

package io.softwareecg.awb.datasource.adapter.ssh;

import io.softwareecg.awb.datasource.model.HostAddress;
import io.softwareecg.awb.datasource.adapter.collector.CollectorConfigCompiler;
import io.softwareecg.collector.config.CollectorConfiguration;
import io.softwareecg.collector.config.CollectorConfigurations;
import io.softwareecg.collector.config.SidecarCollectorJobConfiguration;
import org.junit.Test;

import java.time.Duration;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class SshPlanTest {

    @Test
    public void thePlanIsMultipliedOutByTheCollector() throws Exception {
        SshPlan plan = new SshPlan("Zoo & <Co>", "db-servers", HostAddress.parseAll("db-[01-03]"), "monitor", "s3cr&t",
                List.of("java", "postgres"),
                List.of(TopMetric.CPU_USAGE, TopMetric.LOAD_1M, TopMetric.MEMORY_USED_PERCENT),
                List.of(TopMetric.PROCESS_CPU, TopMetric.PROCESS_MEMORY), Duration.ofSeconds(15));

        assertEquals("three hosts, each once as a whole and once per process", 9, plan.jobCount());
        assertEquals(3 * (3 + 2 * 2), plan.seriesCount());
        assertEquals("monitor@db-01, db-02, db-03", plan.endpoint());
        assertEquals("unix", plan.sourceType());
        assertEquals("db-servers", plan.firstHostGroup());
        assertEquals(List.of("cpu_usage_percent", "load_1m", "memory_used_percent"), plan.firstMetrics());

        String yaml = plan.toCollectorConfig("Zoo & <Co>");
        assertFalse("the password never goes into the configuration", yaml.contains("s3cr"));

        CollectorConfiguration config = CollectorConfigurations.fromString(CollectorConfigCompiler.withSecret(yaml, "s3cr&t"));
        List<SidecarCollectorJobConfiguration> jobs = config.getSidecarCollectorConfiguration();
        assertEquals(9, jobs.size());

        SidecarCollectorJobConfiguration system = jobs.stream()
                .filter(job -> "db-02".equals(job.getNodeName()) && "system".equals(job.getProcessLabel()))
                .findFirst().orElseThrow();
        assertEquals("unix", system.getCounterGroupType());
        assertEquals("db-servers", system.getNodeGroupId());
        assertEquals("monitor", system.getUsername());
        assertEquals("s3cr&t", system.getPassword());
        assertEquals(15_000, system.getMetricFetchInterval());
        assertEquals("Zoo & <Co>", system.getTimeSeriesHeader().getEkgProjectName());
        assertEquals("the series are named by what they are, not by where they come from",
                List.of("cpu_usage_percent", "load_1m", "memory_used_percent"),
                java.util.Arrays.stream(system.getMetricDefinitions()).map(d -> d.getLabel()).collect(Collectors.toList()));
        assertEquals("\\CPU\\used", system.getMetricDefinitions()[0].getDefinition());

        SidecarCollectorJobConfiguration process = jobs.stream()
                .filter(job -> "db-03".equals(job.getNodeName()) && "postgres".equals(job.getProcessLabel()))
                .findFirst().orElseThrow();
        assertEquals("the metric has the name it has for the host: whose it is says the process",
                "cpu_usage_percent", process.getMetricDefinitions()[0].getLabel());
        assertEquals("memory_used_percent", process.getMetricDefinitions()[1].getLabel());
        assertEquals("the process of the job stands in the counter",
                "\\Process(postgres)\\CPU", process.getMetricDefinitions()[0].getDefinition());
    }

    @Test
    public void withoutProcessesOnlyTheHostsAreRead() throws Exception {
        SshPlan plan = new SshPlan("Zoo", "linux", HostAddress.parseAll("localhost"), null, null, List.of(),
                List.of(TopMetric.CPU_USAGE), List.of(TopMetric.PROCESS_CPU), Duration.ofSeconds(5));

        assertEquals(1, plan.jobCount());
        assertTrue(plan.isComplete());
        String yaml = plan.toCollectorConfig("Zoo");
        assertFalse(yaml.contains("username"));
        assertFalse(yaml.contains("password"));
        assertEquals(1, CollectorConfigurations.fromString(yaml).getSidecarCollectorConfiguration().size());

        assertFalse("no host, nothing to collect", new SshPlan("Zoo", "linux", List.of(), null, null, List.of(),
                List.of(TopMetric.CPU_USAGE), List.of(), Duration.ofSeconds(5)).isComplete());
    }
}

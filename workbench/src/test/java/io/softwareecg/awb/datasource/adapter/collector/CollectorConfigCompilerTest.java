package io.softwareecg.awb.datasource.adapter.collector;

import io.softwareecg.awb.datasource.adapter.prometheus.MetricExpression;
import io.softwareecg.collector.config.CollectorConfiguration;
import io.softwareecg.collector.config.CollectorConfigurations;
import io.softwareecg.collector.dt.ClassicCollectorJobConfiguration;
import org.junit.Test;

import java.time.Duration;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import io.softwareecg.awb.datasource.model.AnalysisPlan;
/**
 * The compiled configuration must be one the collector accepts and understands as intended.
 */
public class CollectorConfigCompilerTest {

    @Test
    public void planBecomesOneCollectorJobPerSelectedGroup() throws Exception {
        AnalysisPlan plan = new AnalysisPlan("Shop <Prod> & \"Test\"", "http://prometheus.example:9090", "analyst", "s3cr&t",
                List.of(
                        new AnalysisPlan.GroupSelection("node", List.of(
                                new MetricExpression("cpu_usage_percent",
                                        "100 * (1 - avg by (job, instance) (rate(node_cpu_seconds_total{job=\"node\",mode=\"idle\"}[120s])))"),
                                new MetricExpression(null, "node_load1{job=\"node\"}"))),
                        new AnalysisPlan.GroupSelection("orderservice", List.of(
                                new MetricExpression(null, "process_cpu_usage{job=\"orderservice\"}")))),
                Duration.ofHours(24), Duration.ofSeconds(60));

        String yaml = CollectorConfigCompiler.toYaml(plan);
        assertFalse("the password never goes into the configuration", yaml.contains("s3cr"));
        assertTrue(yaml.contains(CollectorConfigCompiler.SECRET_PLACEHOLDER));

        // the embedded collector gets the secret filled in, in memory
        CollectorConfiguration config = CollectorConfigurations.fromString(CollectorConfigCompiler.withSecret(yaml, "s3cr&t"));

        List<ClassicCollectorJobConfiguration> jobs = config.getClassicCollectorJobConfiguration();
        assertEquals(2, jobs.size());

        ClassicCollectorJobConfiguration node = jobs.stream()
                .filter(job -> "node".equals(job.getMetricGroupId())).findFirst().orElseThrow();
        assertEquals("prometheus", node.getCounterGroupType());
        assertEquals("http://prometheus.example:9090", node.getNodeAddress());
        assertEquals("analyst", node.getUsername());
        assertEquals("s3cr&t", node.getPassword());
        assertEquals(60_000, node.getMetricFetchInterval());
        assertEquals("lookback=86400s", node.getAdditionalArguments());
        assertEquals("Shop <Prod> & \"Test\"", node.getTimeSeriesHeader().getEkgProjectName());

        assertEquals(2, node.getMetricDefinitions().length);
        assertEquals("cpu_usage_percent", node.getMetricDefinitions()[0].getLabel());
        assertEquals("100 * (1 - avg by (job, instance) (rate(node_cpu_seconds_total{job=\"node\",mode=\"idle\"}[120s])))",
                node.getMetricDefinitions()[0].getDefinition());
        assertEquals("node_load1{job=\"node\"}", node.getMetricDefinitions()[1].getDefinition());
    }

    @Test
    public void planWithoutPasswordHasNoPlaceholder() {
        AnalysisPlan plan = new AnalysisPlan("Open", "http://prometheus.example:9090", null, null,
                List.of(new AnalysisPlan.GroupSelection("node", List.of(new MetricExpression(null, "up{job=\"node\"}")))),
                Duration.ofHours(1), Duration.ofSeconds(60));

        String yaml = CollectorConfigCompiler.toYaml(plan);

        assertFalse(yaml.contains("password"));
        assertNull(CollectorConfigurations.fromString(yaml).getClassicCollectorJobConfiguration().get(0).getPassword());
    }

    @Test
    public void connectionCanBeChangedWithoutTouchingWhatIsCollected() {
        AnalysisPlan plan = new AnalysisPlan("Shop", "http://old.example:9090", "olduser", "oldsecret",
                List.of(new AnalysisPlan.GroupSelection("node", List.of(new MetricExpression(null, "up{job=\"node\"}")))),
                Duration.ofHours(1), Duration.ofSeconds(60));
        String yaml = CollectorConfigCompiler.toYaml(plan);

        String moved = CollectorConfigCompiler.withConnection(yaml, "https://new.example/prom?a=1&b=2", "ana \"lyst\"", true);
        ClassicCollectorJobConfiguration job = CollectorConfigurations.fromString(CollectorConfigCompiler.withSecret(moved, "new"))
                .getClassicCollectorJobConfiguration().get(0);
        assertEquals("https://new.example/prom?a=1&b=2", job.getNodeAddress());
        assertEquals("ana \"lyst\"", job.getUsername());
        assertEquals("new", job.getPassword());
        assertEquals("up{job=\"node\"}", job.getMetricDefinitions()[0].getDefinition());

        String open = CollectorConfigCompiler.withConnection(yaml, "http://new.example:9090", null, false);
        assertFalse(open.contains("password"));
        assertFalse(open.contains("username"));
    }

    /**
     * Analyses that were set up with an earlier version have their configuration stored as XML.
     */
    @Test
    public void aConfigurationStoredAsXmlStillRunsAndBecomesYamlWhenTouched() {
        String stored = """
                <?xml version="1.0" encoding="UTF-8"?>
                <CollectorConfiguration>
                    <ekgProjectName>Shop</ekgProjectName>
                    <Counter>
                        <MetricGroup id="node" type="prometheus">
                            <MetricDefinition label="up">up{job=&quot;node&quot;}</MetricDefinition>
                        </MetricGroup>
                    </Counter>
                    <ProcessGroups>
                        <ProcessGroup id="prometheus">
                            <Process name="prometheus" args="lookback=3600s"/>
                        </ProcessGroup>
                    </ProcessGroups>
                    <PhysicalEnv>
                        <HostGroups id="prometheus">
                            <Host name="prometheus" address="http://old.example:9090" username="analyst" password="${ENV:EKG_SOURCE_SECRET}"/>
                        </HostGroups>
                    </PhysicalEnv>
                    <LogicalEnv>
                    </LogicalEnv>
                    <CollectorJobs>
                        <ClassicRemoteCollector metricGroupId="node" hostGroupName="prometheus" processGroupId="prometheus" metricFetchInterval="60000" metricWildcardExpandInterval="600000"/>
                    </CollectorJobs>
                </CollectorConfiguration>
                """;

        ClassicCollectorJobConfiguration job = CollectorConfigurations.fromString(CollectorConfigCompiler.withSecret(stored, "s3cret"))
                .getClassicCollectorJobConfiguration().get(0);
        assertEquals("http://old.example:9090", job.getNodeAddress());
        assertEquals("s3cret", job.getPassword());
        assertEquals("up{job=\"node\"}", job.getMetricDefinitions()[0].getDefinition());
        assertEquals(60_000, job.getMetricFetchInterval());

        String exported = CollectorConfigCompiler.asYaml(stored);
        assertFalse(CollectorConfigurations.isLegacyXml(exported));
        assertTrue(exported.contains(CollectorConfigCompiler.SECRET_PLACEHOLDER));
        assertEquals(CollectorConfigurations.read(stored), CollectorConfigurations.read(exported));

        String moved = CollectorConfigCompiler.withConnection(stored, "http://new.example:9090", null, false);
        assertFalse(CollectorConfigurations.isLegacyXml(moved));
        assertEquals("http://new.example:9090",
                CollectorConfigurations.fromString(moved).getClassicCollectorJobConfiguration().get(0).getNodeAddress());
    }

    @Test
    public void theConfigurationIsWrittenAsOneWouldWriteItByHand() {
        AnalysisPlan plan = new AnalysisPlan("Shop", "http://prometheus.example:9090", "analyst", "s3cret",
                List.of(new AnalysisPlan.GroupSelection("node", List.of(new MetricExpression("load_1m", "node_load1{job=\"node\"}")))),
                Duration.ofHours(1), Duration.ofSeconds(15));

        assertEquals("""
                project: Shop

                metricGroups:
                  node:
                    type: prometheus
                    metrics:
                      - metric: "node_load1{job=\\"node\\"}"
                        label: load_1m

                processGroups:
                  prometheus:
                    - name: prometheus
                      args: lookback=3600s

                hostGroups:
                  prometheus:
                    hosts:
                      - name: prometheus
                        address: http://prometheus.example:9090
                        username: analyst
                        password: "${ENV:EKG_SOURCE_SECRET}"

                collectors:
                  - type: remote
                    metricGroup: node
                    processGroup: prometheus
                    hostGroup: prometheus
                    interval: 15s
                    expandInterval: 150s
                """, CollectorConfigCompiler.toYaml(plan));
    }
}

package io.softwareecg.collector.config;

import io.softwareecg.collector.config.model.CollectorConfig;
import io.softwareecg.collector.config.model.CollectorDef;
import io.softwareecg.collector.config.model.Host;
import io.softwareecg.collector.config.model.Metric;
import org.junit.Test;

import java.time.Duration;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class CollectorConfigurationsTest {

    private static final String SERVERS = """
            # what a configuration looks like
            project: Shop

            metricGroups:
              host:
                type: unix
                metrics:
                  - '\\CPU\\us'
                  - metric: '\\Memory\\used'
                    label: memory
              jvm:
                type: unix
                metrics:
                  - '\\Process(${REF:procName})\\CPU'

            processGroups:
              system:
                - name: global
                  source: '${REF:hostAddress}:22'
              java:
                - name: java
                  label: shop
                  source: '${REF:hostAddress}:22'
                - name: 'java#1'
                  source: '${REF:hostAddress}:22'

            hostGroups:
              db:
                username: ekg
                password: secret
                hosts:
                  - db-01
                  - name: db-02
                    address: 10.0.0.2
                    username: other
                    password: own

            collectors:
              - metricGroup: host
                processGroup: system
                hostGroup: db
                interval: 15s
                expandInterval: 5m
              - type: remote
                metricGroup: jvm
                processGroup: java
                hostGroup: db
                interval: 60000
            """;

    // -----------------------------------------------------------------------------------------
    // reading
    // -----------------------------------------------------------------------------------------

    @Test
    public void aCollectorYieldsOneJobForEveryHostAndProcess() {
        CollectorConfiguration configuration = CollectorConfigurations.fromString(SERVERS);

        List<ClassicCollectorJobConfiguration> jobs = configuration.getClassicCollectorJobConfiguration();
        assertEquals("2 hosts x 1 process + 2 hosts x 2 processes", 6, jobs.size());
        assertTrue(configuration.getSidecarCollectorConfiguration().isEmpty());
        assertTrue(configuration.getCloudCollectorJobConfiguration().isEmpty());

        ClassicCollectorJobConfiguration host = job(jobs, "db-01", "global");
        assertEquals("Shop", host.getEkgProjectName());
        assertEquals("a host given by its address alone is named like it", "db-01", host.getHostAddress());
        assertEquals("host", host.getMetricGroupId());
        assertEquals("unix", host.getCounterGroupType());
        assertEquals(15_000, host.getMetricFetchInterval());
        assertEquals(Long.valueOf(300_000), host.getWildcardExpandRefreshInterval());
        assertEquals("references are resolved for the job", "db-01:22", host.getProcessMetricsSource());
        assertEquals(2, host.getMetricDefinitions().length);
        assertEquals("\\CPU\\us", host.getMetricDefinitions()[0].getDefinition());
        assertEquals("memory", host.getMetricDefinitions()[1].getLabel());

        ClassicCollectorJobConfiguration process = job(jobs, "db-02", "java#1");
        assertEquals("a number alone counts milliseconds", 60_000, process.getMetricFetchInterval());
        assertNull("without an interval for it wildcards are resolved once", process.getWildcardExpandRefreshInterval());
        assertEquals("\\Process(java#1)\\CPU", process.getMetricDefinitions()[0].getDefinition());
        assertEquals("10.0.0.2:22", process.getProcessMetricsSource());
    }

    @Test
    public void aJobKnowsTheGroupsOfItsHostItsProcessAndItsMetricsAndSoDoItsSeries() {
        List<ClassicCollectorJobConfiguration> jobs =
                CollectorConfigurations.fromString(SERVERS).getClassicCollectorJobConfiguration();

        ClassicCollectorJobConfiguration system = job(jobs, "db-01", "global");
        assertEquals("system", system.getProcessGroupId());
        assertEquals("db", system.getTimeSeriesHeader().getHostGroupId());
        assertEquals("system", system.getTimeSeriesHeader().getProcessGroupId());
        assertEquals("host", system.getTimeSeriesHeader().getMetricGroupId());

        ClassicCollectorJobConfiguration process = job(jobs, "db-02", "java#1");
        assertEquals("java", process.getTimeSeriesHeader().getProcessGroupId());
        assertEquals("the group survives a copy of the job, as a job makes one when its series change",
                "java", ((ClassicCollectorJobConfiguration) process.copy()).getTimeSeriesHeader().getProcessGroupId());
    }

    @Test
    public void aHostTakesTheLoginOfItsGroupUnlessItHasItsOwn() {
        List<ClassicCollectorJobConfiguration> jobs = CollectorConfigurations.fromString(SERVERS).getClassicCollectorJobConfiguration();

        assertEquals("ekg", job(jobs, "db-01", "global").getUsername());
        assertEquals("secret", job(jobs, "db-01", "global").getPassword());
        assertEquals("other", job(jobs, "db-02", "global").getUsername());
        assertEquals("own", job(jobs, "db-02", "global").getPassword());
    }

    @Test
    public void aSidecarCollectorWithoutNodesReadsItsOwnMachine() {
        CollectorConfiguration configuration = CollectorConfigurations.fromString("""
                metricGroups:
                  host: {type: unix, metrics: ['\\Load\\one']}
                processGroups:
                  system: [{name: global}]
                collectors:
                  - {type: sidecar, metricGroup: host, processGroup: system, interval: 1s}
                """);

        assertTrue(configuration.getClassicCollectorJobConfiguration().isEmpty());
        List<SidecarCollectorJobConfiguration> jobs = configuration.getSidecarCollectorConfiguration();
        assertEquals(1, jobs.size());
        assertEquals("localhost", jobs.get(0).getNodeName());
        assertEquals("127.0.0.1", jobs.get(0).getNodeAddress());
    }

    @Test
    public void aSidecarCollectorReadsTheHostsOfAHostGroupOrThePodsOfAPodGroup() {
        CollectorConfiguration configuration = CollectorConfigurations.fromString("""
                metricGroups:
                  jvm: {type: jolokia, metrics: ['java.lang:type=Memory,*']}
                processGroups:
                  app: [{name: app, source: '${REF:podAddress}/jolokia'}]
                hostGroups:
                  nodes:
                    username: ekg
                    hosts: [node-1, node-2]
                platform:
                  name: shop
                  refreshInterval: 30s
                  podGroups:
                    web:
                      service: web
                      username: pod-user
                      pods:
                        - {id: web-1, name: web-1, address: 'http://10.1.0.1'}
                        - {id: web-2, name: web-2, address: 'http://10.1.0.2', username: own}
                collectors:
                  - {type: sidecar, metricGroup: jvm, processGroup: app, nodeGroup: nodes, interval: 5s}
                  - {type: sidecar, metricGroup: jvm, processGroup: app, nodeGroup: web, interval: 5s}
                """);

        List<SidecarCollectorJobConfiguration> jobs = configuration.getSidecarCollectorConfiguration();
        assertEquals(4, jobs.size());

        SidecarCollectorJobConfiguration host = sidecarJob(jobs, "node-1");
        assertEquals("ekg", host.getUsername());

        SidecarCollectorJobConfiguration pod = sidecarJob(jobs, "web-2");
        assertEquals("http://10.1.0.2/jolokia", pod.getProcessMetricsSource());
        assertEquals("own", pod.getUsername());
        assertEquals("pod-user", sidecarJob(jobs, "web-1").getUsername());
        assertEquals("shop", pod.getTimeSeriesHeader().getNamespace());
    }

    // -----------------------------------------------------------------------------------------
    // what is wrong is said, with its place
    // -----------------------------------------------------------------------------------------

    @Test
    public void aKeyThatIsNotKnownIsAnErrorThatNamesItsLine() {
        String message = failure(SERVERS.replace("    expandInterval: 5m", "    expandIntervall: 5m"));

        assertTrue(message, message.contains("unknown key 'expandIntervall'"));
        assertTrue(message, message.contains("collectors[0]"));
        int line = SERVERS.substring(0, SERVERS.indexOf("expandInterval")).split("\n").length;
        assertTrue(message, message.contains("line " + line + ":"));
        assertTrue("the keys that are known are listed: " + message,
                message.contains("Known here: [at, expandInterval, hostGroup, interval, metricGroup, nodeGroup, processGroup, type]"));
    }

    @Test
    public void aCollectorMustReferToGroupsThatExist() {
        String message = failure(SERVERS.replace("hostGroup: db\n    interval: 15s", "hostGroup: web\n    interval: 15s"));

        assertTrue(message, message.contains("collectors[0]: there is no host group 'web'. Known: [db]"));

        assertTrue(failure(SERVERS.replace("metricGroup: host", "metricGroup: hosts")).contains("no metric group 'hosts'"));
        assertTrue(failure(SERVERS.replace("processGroup: system", "processGroup: sys")).contains("no process group 'sys'"));
    }

    @Test
    public void whatIsMissingIsNamed() {
        assertTrue(failure("").contains("empty"));
        assertTrue(failure("project: X").contains("'metricGroups' is missing"));
        assertTrue(failure(SERVERS.replace("    interval: 15s\n", "")).contains("collectors[0]: 'interval' is missing"));
        assertTrue(failure(SERVERS.replace("    hostGroup: db\n    interval: 15s", "    interval: 15s"))
                .contains("collectors[0]: 'hostGroup' is missing"));
        assertTrue(failure(SERVERS.replace("    type: unix\n    metrics:\n      - '\\CPU\\us'", "    metrics:\n      - '\\CPU\\us'"))
                .contains("metricGroups.host: 'type' is missing"));
    }

    @Test
    public void aDurationNeedsAUnitThatIsKnown() {
        String message = failure(SERVERS.replace("interval: 15s", "interval: 15 sec"));

        assertTrue(message, message.contains("'15 sec' is not a duration"));
        assertTrue(message, message.contains("collectors[0].interval"));
    }

    @Test
    public void aKeyGivenTwiceIsAnError() {
        String message = failure(SERVERS.replace("project: Shop", "project: Shop\nproject: Other"));

        assertTrue(message, message.toLowerCase().contains("duplicate"));
    }

    @Test
    public void nodeGroupAndHostGroupBelongToTheirKindOfCollector() {
        assertTrue(failure(SERVERS.replace("  - type: remote", "  - type: sidecar")).contains("names its machines as 'nodeGroup'"));
        assertTrue(failure(SERVERS.replace("    hostGroup: db\n    interval: 15s", "    nodeGroup: db\n    interval: 15s"))
                .contains("'nodeGroup' belongs to a collector of 'type: sidecar'"));
    }

    // -----------------------------------------------------------------------------------------
    // writing
    // -----------------------------------------------------------------------------------------

    @Test
    public void whatIsWrittenReadsAsTheSameConfiguration() {
        CollectorConfig config = CollectorConfigurations.read(SERVERS);

        String yaml = CollectorConfigurations.toYaml(config);

        assertEquals(config, CollectorConfigurations.read(yaml));
        assertTrue("a simple metric is its text:\n" + yaml, yaml.contains("- \\CPU\\us\n"));
        assertTrue("a host named like its address is its text:\n" + yaml, yaml.contains("- db-01\n"));
        assertTrue("durations keep their unit:\n" + yaml, yaml.contains("interval: 15s") && yaml.contains("expandInterval: 5m")
                && yaml.contains("interval: 1m"));
        assertFalse(yaml.startsWith("---"));
    }

    @Test
    public void textsThatLookLikeSomethingElseSurviveWriting() {
        CollectorConfig config = new CollectorConfig("123",
                java.util.Map.of("m", new io.softwareecg.collector.config.model.MetricGroup("prometheus",
                        List.of(Metric.of("rate(node_cpu_seconds_total{mode=\"idle\"}[1m]) # not a comment"),
                                new Metric("*:type=*,*", "true", null, false)))),
                java.util.Map.of("p", List.of(new io.softwareecg.collector.config.model.ProcessDef("%1", null, "${REF:hostAddress}:22", "lookback=1h"))),
                java.util.Map.of("h", new io.softwareecg.collector.config.model.HostGroup("user: name", "007 'x' \"y\" \\ ${ENV:P}",
                        List.of(new Host("a", "http://host:9090", null, null)))),
                null,
                List.of(CollectorDef.remote("m", "p", "h", Duration.ofMillis(1500), null)));

        assertEquals(config, CollectorConfigurations.read(CollectorConfigurations.toYaml(config)));
    }

    // -----------------------------------------------------------------------------------------
    // configurations of earlier versions
    // -----------------------------------------------------------------------------------------

    private static final String SERVERS_AS_XML = """
            <?xml version="1.0" encoding="UTF-8"?>
            <CollectorConfiguration>
                <ekgProjectName>Shop</ekgProjectName>
                <Counter>
                    <MetricGroup id="host" type="unix">
                        <MetricDefinition>\\CPU\\us</MetricDefinition>
                        <MetricDefinition label="memory">\\Memory\\used</MetricDefinition>
                    </MetricGroup>
                    <MetricGroup id="jvm" type="unix">
                        <MetricDefinition>\\Process(${REF:procName})\\CPU</MetricDefinition>
                    </MetricGroup>
                </Counter>
                <ProcessGroups>
                    <ProcessGroup id="system">
                        <Process name="global" source="${REF:hostAddress}:22"/>
                    </ProcessGroup>
                    <ProcessGroup id="java">
                        <Process name="java" label="shop" source="${REF:hostAddress}:22"/>
                        <Process name="java#1" source="${REF:hostAddress}:22"/>
                    </ProcessGroup>
                </ProcessGroups>
                <PhysicalEnv>
                    <HostGroups id="db" username="ekg" password="secret">
                        <Host name="db-01" address="db-01"/>
                        <Host name="db-02" address="10.0.0.2" username="other" password="own"/>
                    </HostGroups>
                </PhysicalEnv>
                <LogicalEnv>
                </LogicalEnv>
                <CollectorJobs>
                    <ClassicRemoteCollector metricGroupId="host" hostGroupName="db" processGroupId="system"
                                            metricFetchInterval="15000" metricWildcardExpandInterval="300000"/>
                    <ClassicRemoteCollector metricGroupId="jvm" hostGroupName="db" processGroupId="java" metricFetchInterval="60000"/>
                </CollectorJobs>
            </CollectorConfiguration>
            """;

    @Test
    public void aConfigurationInXmlIsStillRead() {
        assertTrue(CollectorConfigurations.isLegacyXml(SERVERS_AS_XML));
        assertFalse(CollectorConfigurations.isLegacyXml(SERVERS));

        assertEquals(CollectorConfigurations.read(SERVERS), CollectorConfigurations.read(SERVERS_AS_XML));
        assertEquals(6, CollectorConfigurations.fromString(SERVERS_AS_XML).getClassicCollectorJobConfiguration().size());
    }

    @Test
    public void aConfigurationInXmlIsConvertedToYaml() {
        String yaml = ConvertConfiguration.convert(SERVERS_AS_XML, "config.xml");

        assertTrue(yaml.startsWith("# Software-EKG collector configuration, converted from config.xml\n"));
        assertFalse(CollectorConfigurations.isLegacyXml(yaml));
        assertEquals(CollectorConfigurations.read(SERVERS), CollectorConfigurations.read(yaml));
    }

    @Test
    public void xmlOfACollectorBefore61IsRejectedWithItsReason() {
        String message = failure("<CollectorConfiguration><CounterGroups/><Collectors/></CollectorConfiguration>");

        assertTrue(message, message.contains("before version 6.1"));
    }

    @Test
    public void xmlMustNotPullInOtherFiles() {
        String message = failure("""
                <?xml version="1.0"?>
                <!DOCTYPE CollectorConfiguration [<!ENTITY secret SYSTEM "file:///etc/passwd">]>
                <CollectorConfiguration><ekgProjectName>&secret;</ekgProjectName></CollectorConfiguration>
                """);

        assertTrue(message, message.contains("not readable"));
    }

    // -----------------------------------------------------------------------------------------

    private static String failure(String text) {
        try {
            CollectorConfigurations.fromString(text);
        } catch (InvalidConfigurationException e) {
            return e.getDetail();
        }
        fail("the configuration should have been rejected");
        return null;
    }

    private static ClassicCollectorJobConfiguration job(List<ClassicCollectorJobConfiguration> jobs, String host, String process) {
        return jobs.stream()
                .filter(job -> host.equals(job.getHostName()) && process.equals(job.getProcessName()))
                .findFirst().orElseThrow(() -> new AssertionError("no job for " + process + " on " + host));
    }

    private static SidecarCollectorJobConfiguration sidecarJob(List<SidecarCollectorJobConfiguration> jobs, String node) {
        return jobs.stream().filter(job -> node.equals(job.getNodeName()))
                .findFirst().orElseThrow(() -> new AssertionError("no job on " + node));
    }
}

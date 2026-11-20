package io.softwareecg.collector.config.impl;

import io.softwareecg.collector.config.InvalidConfigurationException;
import io.softwareecg.collector.config.model.CollectorConfig;
import io.softwareecg.collector.config.model.CollectorDef;
import io.softwareecg.collector.config.model.Host;
import io.softwareecg.collector.config.model.HostGroup;
import io.softwareecg.collector.config.model.Metric;
import io.softwareecg.collector.config.model.MetricGroup;
import io.softwareecg.collector.config.model.Platform;
import io.softwareecg.collector.config.model.ProcessDef;
import io.softwareecg.collector.dt.ClassicCollectorJobConfiguration;
import io.softwareecg.collector.dt.ContainerInfo;
import io.softwareecg.collector.dt.HostInfo;
import io.softwareecg.collector.dt.MetricDefinition;
import io.softwareecg.collector.dt.MetricGroupInfo;
import io.softwareecg.collector.dt.NodeInfo;
import io.softwareecg.collector.dt.PlatformProjectInfo;
import io.softwareecg.collector.dt.PodInfo;
import io.softwareecg.collector.dt.ProcessInfo;
import io.softwareecg.collector.dt.SidecarCollectorJobConfiguration;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * Turns a configuration into the jobs of the collector: every collector of the configuration
 * yields one job for every machine and every process of its groups.
 */
final class JobConfigurationFactory {

    /**
     * The machine a sidecar collector reads if it names none: the one it runs on.
     */
    private static final NodeInfo OWN_MACHINE = new NodeInfo("localhost", "127.0.0.1", "Default", null, null);

    private JobConfigurationFactory() {
    }

    // ---------------------------------------------------------------------------------------------
    // remote collectors
    // ---------------------------------------------------------------------------------------------

    static List<ClassicCollectorJobConfiguration> remoteJobs(CollectorConfig config) {
        List<ClassicCollectorJobConfiguration> jobs = new ArrayList<>();

        for (CollectorDef collector : config.collectors()) {
            if (collector.type() != CollectorDef.Type.REMOTE) {
                continue;
            }
            HostGroup hostGroup = config.hostGroups().get(collector.hostGroup());
            MetricGroup metricGroup = config.metricGroups().get(collector.metricGroup());

            for (Host host : hostGroup.hosts()) {
                for (ProcessDef process : config.processGroups().get(collector.processGroup())) {
                    jobs.add(new ClassicCollectorJobConfiguration(
                            config.project(),
                            new HostInfo(host.name(), host.address(), collector.hostGroup(),
                                    hostGroup.usernameOf(host), hostGroup.passwordOf(host)),
                            processInfo(process),
                            metricGroupInfo(collector, metricGroup),
                            metricDefinitions(metricGroup)));
                }
            }
        }

        jobs.sort(Comparator.comparing(ClassicCollectorJobConfiguration::getHostName));
        return jobs;
    }

    // ---------------------------------------------------------------------------------------------
    // sidecar collectors
    // ---------------------------------------------------------------------------------------------

    static List<SidecarCollectorJobConfiguration> sidecarJobs(CollectorConfig config) {
        List<SidecarCollectorJobConfiguration> jobs = new ArrayList<>();

        for (CollectorDef collector : config.collectors()) {
            if (collector.type() != CollectorDef.Type.SIDECAR) {
                continue;
            }
            MetricGroup metricGroup = config.metricGroups().get(collector.metricGroup());

            for (Node node : nodesOf(config, collector.nodeGroup())) {
                for (ProcessDef process : config.processGroups().get(collector.processGroup())) {
                    jobs.add(sidecarJob(config, collector, node, process, metricGroup));
                }
            }
        }

        jobs.sort(Comparator.comparing(SidecarCollectorJobConfiguration::getNodeName));
        return jobs;
    }

    private static SidecarCollectorJobConfiguration sidecarJob(CollectorConfig config, CollectorDef collector,
                                                               Node node, ProcessDef process, MetricGroup metricGroup) {
        if (node.podGroup != null) {
            // a pod of a pod group: the job knows its place in the cluster
            Platform platform = config.platform();
            return new SidecarCollectorJobConfiguration(
                    config.project(),
                    platformProjectInfo(platform),
                    podInfo(platform, node.podGroupId, node.podGroup, node.pod),
                    (ContainerInfo) null,
                    processInfo(process),
                    metricGroupInfo(collector, metricGroup),
                    metricDefinitions(metricGroup));
        }
        return new SidecarCollectorJobConfiguration(
                config.project(),
                node.info,
                processInfo(process),
                metricGroupInfo(collector, metricGroup),
                metricDefinitions(metricGroup));
    }

    /**
     * The machines a sidecar collector reads: the hosts of a host group, the pods of a pod
     * group, the containers of a single pod - or, without a node group, the own machine.
     */
    private static List<Node> nodesOf(CollectorConfig config, String nodeGroup) {
        if (nodeGroup == null || nodeGroup.isBlank()) {
            return List.of(new Node(OWN_MACHINE));
        }

        HostGroup hostGroup = config.hostGroups().get(nodeGroup);
        if (hostGroup != null) {
            List<Node> nodes = new ArrayList<>();
            for (Host host : hostGroup.hosts()) {
                nodes.add(new Node(new HostInfo(host.name(), host.address(), nodeGroup,
                        hostGroup.usernameOf(host), hostGroup.passwordOf(host))));
            }
            return nodes;
        }

        Platform platform = config.platform();
        if (platform != null) {
            Platform.PodGroup podGroup = platform.podGroups().get(nodeGroup);
            if (podGroup != null) {
                return podsOf(platform, nodeGroup, podGroup);
            }
            for (Platform.PodGroup group : platform.podGroups().values()) {
                for (Platform.Pod pod : group.pods()) {
                    if (nodeGroup.equals(pod.id())) {
                        return containersOf(pod);
                    }
                }
            }
        }

        throw new InvalidConfigurationException("There is no host group, pod group or pod '" + nodeGroup + "'.");
    }

    private static List<Node> podsOf(Platform platform, String podGroupId, Platform.PodGroup podGroup) {
        List<Node> nodes = new ArrayList<>();
        for (Platform.Pod pod : podGroup.pods()) {
            Node node = new Node(new NodeInfo(
                    first(pod.name(), podGroupId),
                    first(pod.address(), ""),
                    platform.name(),
                    first(pod.username(), podGroup.username()),
                    first(pod.password(), podGroup.password())));
            node.pod = pod;
            node.podGroup = podGroup;
            node.podGroupId = podGroupId;
            nodes.add(node);
        }
        return nodes;
    }

    private static List<Node> containersOf(Platform.Pod pod) {
        List<Node> nodes = new ArrayList<>();
        for (Platform.Container container : pod.containers()) {
            Node node = new Node(new NodeInfo(
                    first(container.name(), first(container.id(), first(pod.name(), pod.id()))),
                    first(container.address(), first(pod.address(), "")),
                    pod.name(),
                    first(container.username(), pod.username()),
                    first(container.password(), pod.password())));
            node.pod = pod;
            nodes.add(node);
        }
        return nodes;
    }

    private static PodInfo podInfo(Platform platform, String podGroupId, Platform.PodGroup podGroup, Platform.Pod pod) {
        return new PodInfo(
                pod.name(),
                pod.address(),
                podGroupId,
                first(podGroup.namespace(), platform.name()),
                podGroup.service(),
                first(pod.username(), podGroup.username()),
                first(pod.password(), podGroup.password()));
    }

    private static PlatformProjectInfo platformProjectInfo(Platform platform) {
        return new PlatformProjectInfo(
                platform.name(),
                platform.serviceAddress(),
                platform.serviceUser(),
                platform.servicePassword(),
                platform.refreshInterval() == null ? -1L : platform.refreshInterval().toMillis());
    }

    // ---------------------------------------------------------------------------------------------
    // common parts of a job
    // ---------------------------------------------------------------------------------------------

    private static ProcessInfo processInfo(ProcessDef process) {
        return new ProcessInfo(process.name(), process.label(), process.source(), process.args());
    }

    private static MetricGroupInfo metricGroupInfo(CollectorDef collector, MetricGroup metricGroup) {
        return new MetricGroupInfo(
                collector.metricGroup(),
                metricGroup.type(),
                collector.expandInterval() == null ? null : collector.expandInterval().toMillis(),
                collector.interval() == null ? 0L : collector.interval().toMillis(),
                collector.at());
    }

    /**
     * @return the metrics of the group, as objects of their own for every job: a job changes them
     */
    private static MetricDefinition[] metricDefinitions(MetricGroup metricGroup) {
        List<Metric> metrics = metricGroup.metrics();
        MetricDefinition[] definitions = new MetricDefinition[metrics.size()];
        for (int i = 0; i < definitions.length; i++) {
            Metric metric = metrics.get(i);
            definitions[i] = new MetricDefinition(metric.metric(), metric.label(), metric.regexPostFilter(), metric.wildcard());
        }
        return definitions;
    }

    private static String first(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    /**
     * A machine a sidecar collector reads and, for a pod, where it was found.
     */
    private static final class Node {

        private final NodeInfo info;

        private Platform.Pod pod;

        private Platform.PodGroup podGroup;

        private String podGroupId;

        private Node(NodeInfo info) {
            this.info = info;
        }
    }
}

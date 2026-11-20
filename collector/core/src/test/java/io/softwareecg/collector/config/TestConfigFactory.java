package io.softwareecg.collector.config;

import io.softwareecg.collector.dt.*;
import org.apache.commons.lang3.StringUtils;

/**
 * todo insert comment here
 *
 * @since 12.08.2018
 */
public class TestConfigFactory {

    public static CollectorJobConfiguration createTestConfig() {
        return createTestConfig("TestHostname");
    }

    public static CollectorJobConfiguration createTestConfig(String hostName) {
        return createTestConfig(hostName, "127.0.0.1", "TestProcessName");
    }

    public static CollectorJobConfiguration createTestConfig(String hostName, String processName) {
        return createTestConfig(hostName, "127.0.0.1", processName, "/testsource", "jmx");
    }

    public static CollectorJobConfiguration createTestConfig(String hostName, String processName, String metricGroupType) {
        return createTestConfig(hostName, "127.0.0.1", processName, "/testsource", metricGroupType);
    }

    public static CollectorJobConfiguration createTestConfig(String nodeName, String nodeAddress, String processName,
                                                             String processSource, String metricGroupType) {

        return createTestConfig(nodeName, nodeAddress, processName, "TestProcessLabel", processSource,
                metricGroupType, 3000);
    }

    public static CollectorJobConfiguration createTestConfig(String nodeName, String nodeAddress, String processName,
                          String processLabel, String processSource, String metricGroupType, long metricRefreshInterval) {

        return new SidecarCollectorJobConfiguration(
                "ekgProject",
                createNodeInfo(nodeName, nodeAddress),
                createProcessInfo(processName, processLabel, processSource),
                createMetricGroupInfo("TestMetricGroupId", metricGroupType, 6000, metricRefreshInterval),
                new MetricDefinition[0]
        );
    }

    public static CollectorJobConfiguration createTestConfig(NodeInfo nodeInfo, ProcessInfo processInfo,
                                                             MetricGroupInfo metricGroupInfo, MetricDefinition[] metrics) {

        return new SidecarCollectorJobConfiguration(
                "ekgProject",
                nodeInfo,
                processInfo,
                metricGroupInfo,
                metrics
        );
    }


    public static NodeInfo createNodeInfo(String nodeName, String nodeAddress) {

        String nodeGroupName = "TestNodeGroup";
        String username = "";
        String password = "";

        return new NodeInfo(nodeName, nodeAddress, nodeGroupName, username, password);
    }


    public static ProcessInfo createProcessInfo(String processName, String processLabel, String processSource) {

        if (StringUtils.isBlank(processLabel) && StringUtils.isNotBlank(processName)) {
            processLabel = processName;
        }

        if (StringUtils.isBlank(processName) && StringUtils.isNotBlank(processLabel)) {
            processName = processLabel;
        }

        return new ProcessInfo(processName, processLabel, processSource, null);
    }


    public static MetricGroupInfo createMetricGroupInfo(String metricGroupType) {
        return createMetricGroupInfo("TestMetricGroupId", metricGroupType);
    }

    public static MetricGroupInfo createMetricGroupInfo(String metricGroupId, String metricGroupType) {
        return createMetricGroupInfo(metricGroupId, metricGroupType, 6000, 4000);
    }

    public static MetricGroupInfo createMetricGroupInfo(String metricGroupId, String metricGroupType,
                                                        long refreshWildCardInterval, long metricFetchInterval) {

        return new MetricGroupInfo(
                metricGroupId,
                metricGroupType,
                refreshWildCardInterval,
                metricFetchInterval,
                null
        );
    }
}

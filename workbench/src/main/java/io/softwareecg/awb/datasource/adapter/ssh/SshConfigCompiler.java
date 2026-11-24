package io.softwareecg.awb.datasource.adapter.ssh;

import io.softwareecg.awb.datasource.model.CollectionPlan;
import io.softwareecg.awb.datasource.model.HostAddress;
import io.softwareecg.collector.config.CollectorConfigurations;
import io.softwareecg.collector.config.model.CollectorConfig;
import io.softwareecg.collector.config.model.CollectorDef;
import io.softwareecg.collector.config.model.Host;
import io.softwareecg.collector.config.model.HostGroup;
import io.softwareecg.collector.config.model.Metric;
import io.softwareecg.collector.config.model.MetricGroup;
import io.softwareecg.collector.config.model.ProcessDef;

import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Turns an {@link SshPlan} into a collector configuration, in the collector's own terms: the
 * hosts are a host group, the processes two process groups (the host as a whole, and the
 * processes to watch), the metrics two metric groups of the Unix source, and two collectors
 * multiply them out - every host for the system metrics, every host with every process for
 * the process metrics.
 * <p>
 * Every metric carries the name its series get in the analysis as its label, so a stand-alone
 * collector running the exported configuration writes the same series as the AWB does.
 * As everywhere, the password is not part of the configuration.
 */
public final class SshConfigCompiler {

    static final String SYSTEM_METRICS = "linux-system";

    static final String PROCESS_METRICS = "linux-process";

    static final String SYSTEM_PROCESS = "system";

    static final String WATCHED_PROCESSES = "processes";

    private static final String ON_THE_HOST = "${REF:hostAddress}";

    private SshConfigCompiler() {
    }

    public static String toYaml(SshPlan plan) {
        boolean system = !plan.systemMetrics().isEmpty();
        boolean processes = !plan.processMetrics().isEmpty() && !plan.processes().isEmpty();
        Duration interval = Duration.ofMillis(Math.max(1_000, plan.interval().toMillis()));

        Map<String, MetricGroup> metricGroups = new LinkedHashMap<>();
        Map<String, List<ProcessDef>> processGroups = new LinkedHashMap<>();
        List<CollectorDef> collectors = new ArrayList<>();

        if (system) {
            metricGroups.put(SYSTEM_METRICS, metricGroup(plan.systemMetrics()));
            processGroups.put(SYSTEM_PROCESS, List.of(new ProcessDef(SYSTEM_PROCESS, null, ON_THE_HOST, null)));
            // a collector of this type and its hosts share the machine or - as here - the login
            collectors.add(CollectorDef.sidecar(SYSTEM_METRICS, SYSTEM_PROCESS, plan.hostGroup(), interval, null));
        }
        if (processes) {
            metricGroups.put(PROCESS_METRICS, metricGroup(plan.processMetrics()));
            List<ProcessDef> watched = new ArrayList<>();
            for (String process : plan.processes()) {
                watched.add(new ProcessDef(process, null, ON_THE_HOST, null));
            }
            processGroups.put(WATCHED_PROCESSES, watched);
            collectors.add(CollectorDef.sidecar(PROCESS_METRICS, WATCHED_PROCESSES, plan.hostGroup(), interval, null));
        }

        List<Host> hosts = new ArrayList<>();
        for (HostAddress host : plan.hosts()) {
            hosts.add(new Host(host.name(), host.address(), null, null));
        }
        HostGroup hostGroup = new HostGroup(
                plan.username() == null || plan.username().isBlank() ? null : plan.username(),
                plan.password() == null || plan.password().isBlank() ? null : CollectionPlan.SECRET_PLACEHOLDER,
                hosts);

        return CollectorConfigurations.toYaml(new CollectorConfig(plan.projectName(), metricGroups, processGroups,
                Map.of(plan.hostGroup(), hostGroup), null, collectors));
    }

    private static MetricGroup metricGroup(List<TopMetric> topMetrics) {
        List<Metric> metrics = new ArrayList<>();
        for (TopMetric metric : topMetrics) {
            metrics.add(new Metric(metric.counter(), metric.seriesName(), null, null));
        }
        return new MetricGroup(SshPlan.SOURCE_TYPE, metrics);
    }
}

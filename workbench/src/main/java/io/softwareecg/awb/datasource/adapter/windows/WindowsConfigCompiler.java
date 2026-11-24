package io.softwareecg.awb.datasource.adapter.windows;

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
 * Turns a {@link WindowsPlan} into a collector configuration, in the collector's own terms:
 * the hosts are a host group, the processes two process groups (the host as a whole, and the
 * processes to watch), the metrics two metric groups of the source for Windows performance
 * counters, and two collectors multiply them out - every host for the system metrics, every
 * host with every process for the process metrics.
 * <p>
 * Every metric carries the name its series get in the analysis as its label, so a stand-alone
 * collector running the exported configuration writes the same series as the AWB does. A
 * further counter the user named has no such name: its series is called as the counter is.
 */
public final class WindowsConfigCompiler {

    static final String SYSTEM_METRICS = "windows-system";

    static final String PROCESS_METRICS = "windows-process";

    static final String SYSTEM_PROCESS = "system";

    static final String WATCHED_PROCESSES = "processes";

    /**
     * Fits every counter that has an instance, as the source names it with its machine -
     * {@code \\host\Network Interface(Intel[R] 82574L)\Bytes Received/sec} - whatever the
     * language of the names. Its first group is the instance.
     */
    static final String INSTANCE_FILTER = "^\\\\\\\\[^\\\\]+\\\\[^(]+\\((.*)\\)\\\\[^\\\\]+$";

    private WindowsConfigCompiler() {
    }

    public static String toYaml(WindowsPlan plan) {
        boolean system = plan.readsSystem();
        boolean processes = !plan.processMetrics().isEmpty() && !plan.processes().isEmpty();
        Duration interval = Duration.ofMillis(Math.max(1_000, plan.interval().toMillis()));

        Map<String, MetricGroup> metricGroups = new LinkedHashMap<>();
        Map<String, List<ProcessDef>> processGroups = new LinkedHashMap<>();
        List<CollectorDef> collectors = new ArrayList<>();

        if (system) {
            List<Metric> metrics = metrics(plan.systemMetrics());
            for (String counter : plan.counters()) {
                metrics.add(new Metric(counter, null, null, counter.indexOf('*') >= 0));
            }
            metricGroups.put(SYSTEM_METRICS, new MetricGroup(WindowsPlan.SOURCE_TYPE, metrics));
            processGroups.put(SYSTEM_PROCESS, List.of(new ProcessDef(SYSTEM_PROCESS, null, null, null)));
            collectors.add(CollectorDef.remote(SYSTEM_METRICS, SYSTEM_PROCESS, plan.hostGroup(), interval, null));
        }
        if (processes) {
            metricGroups.put(PROCESS_METRICS, new MetricGroup(WindowsPlan.SOURCE_TYPE, metrics(plan.processMetrics())));
            List<ProcessDef> watched = new ArrayList<>();
            for (String process : plan.processes()) {
                watched.add(new ProcessDef(process, null, null, null));
            }
            processGroups.put(WATCHED_PROCESSES, watched);
            collectors.add(CollectorDef.remote(PROCESS_METRICS, WATCHED_PROCESSES, plan.hostGroup(), interval, null));
        }

        List<Host> hosts = new ArrayList<>();
        for (HostAddress host : plan.hosts()) {
            hosts.add(new Host(host.name(), host.address(), null, null));
        }

        return CollectorConfigurations.toYaml(new CollectorConfig(plan.projectName(), metricGroups, processGroups,
                Map.of(plan.hostGroup(), new HostGroup(null, null, hosts)), null, collectors));
    }

    private static List<Metric> metrics(List<WindowsMetric> windowsMetrics) {
        List<Metric> metrics = new ArrayList<>();
        for (WindowsMetric metric : windowsMetrics) {
            metrics.add(metric.isPerInstance()
                    // one series for each instance, named with it
                    ? new Metric(metric.counter(), metric.seriesName() + " ${FILTER:1}", INSTANCE_FILTER, true)
                    : new Metric(metric.counter(), metric.seriesName(), null, false));
        }
        return metrics;
    }
}

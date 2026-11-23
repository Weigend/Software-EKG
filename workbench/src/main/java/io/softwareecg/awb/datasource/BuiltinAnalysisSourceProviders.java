package io.softwareecg.awb.datasource;

import io.softwareecg.awb.datasource.spi.AnalysisSourceDefinition;
import io.softwareecg.awb.datasource.spi.AnalysisSourceOption;
import io.softwareecg.awb.datasource.spi.AnalysisSourceProvider;
import io.softwareecg.awb.datasource.spi.AnalysisTopic;

import java.util.Map;

/** Built-in source descriptors. Concrete discovery and plan adapters live under datasource.adapter. */
public final class BuiltinAnalysisSourceProviders {
    private BuiltinAnalysisSourceProviders() {}

    public static AnalysisSourceProvider prometheus() {
        return provider(AnalysisSourceDefinition.PROMETHEUS, Map.of(
                AnalysisTopic.LINUX, option(AnalysisSourceDefinition.PROMETHEUS,
                        "A server already collects from all hosts (node exporter). The hosts are found for you, their history comes along."),
                AnalysisTopic.JVM, option(AnalysisSourceDefinition.PROMETHEUS,
                        "Metrics of Micrometer or the JMX exporter, through a server. The processes are found for you, their history comes along."),
                AnalysisTopic.WINDOWS, new AnalysisSourceOption(AnalysisSourceDefinition.PROMETHEUS,
                        "One server that collects from all hosts (windows exporter).", false),
                AnalysisTopic.DATABASES, option(AnalysisSourceDefinition.PROMETHEUS,
                        "One server that collects from the database exporters."),
                AnalysisTopic.KUBERNETES, option(AnalysisSourceDefinition.PROMETHEUS,
                        "One server that collects from the cluster."),
                AnalysisTopic.SERVICES, option(AnalysisSourceDefinition.PROMETHEUS,
                        "One server that collects from the cluster.")));
    }

    public static AnalysisSourceProvider sshTop() {
        return provider(AnalysisSourceDefinition.SSH_TOP, Map.of(AnalysisTopic.LINUX,
                option(AnalysisSourceDefinition.SSH_TOP,
                        "Each host is read with top. Needs only an SSH login, no Prometheus. Shows single processes too. No history.")));
    }

    public static AnalysisSourceProvider windowsCounters() {
        return provider(AnalysisSourceDefinition.WINDOWS_COUNTERS, Map.of(AnalysisTopic.WINDOWS,
                option(AnalysisSourceDefinition.WINDOWS_COUNTERS,
                        "Each host is read through its performance counters, from Windows. Shows single processes too. No history.")));
    }

    public static AnalysisSourceProvider jolokia() {
        return provider(AnalysisSourceDefinition.JOLOKIA, Map.of(AnalysisTopic.JVM,
                option(AnalysisSourceDefinition.JOLOKIA, "Each JVM is read directly over HTTP.")));
    }

    public static AnalysisSourceProvider jmx() {
        return provider(AnalysisSourceDefinition.JMX, Map.of(AnalysisTopic.JVM,
                option(AnalysisSourceDefinition.JMX, "Each JVM is read directly over JMX.")));
    }

    public static AnalysisSourceProvider postgres() {
        return provider(AnalysisSourceDefinition.POSTGRES, Map.of(AnalysisTopic.DATABASES,
                option(AnalysisSourceDefinition.POSTGRES, "Each database is asked directly.")));
    }

    private static AnalysisSourceOption option(AnalysisSourceDefinition source, String description) {
        return new AnalysisSourceOption(source, description);
    }

    private static AnalysisSourceProvider provider(AnalysisSourceDefinition definition,
                                                   Map<AnalysisTopic, AnalysisSourceOption> topics) {
        return new AnalysisSourceProvider() {
            @Override public AnalysisSourceDefinition definition() { return definition; }
            @Override public Map<AnalysisTopic, AnalysisSourceOption> topics() { return topics; }
        };
    }
}

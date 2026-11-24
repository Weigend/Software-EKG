package io.softwareecg.awb.datasource.adapter.prometheus;

import io.softwareecg.awb.datasource.model.CollectionPlan;
import io.softwareecg.awb.datasource.adapter.collector.CollectorConfigCompiler;
import io.softwareecg.collector.config.CollectorConfigurations;
import io.softwareecg.collector.config.model.CollectorConfig;
import io.softwareecg.collector.config.model.CollectorDef;
import io.softwareecg.collector.config.model.Host;
import io.softwareecg.collector.config.model.HostGroup;
import io.softwareecg.collector.config.model.Metric;
import io.softwareecg.collector.config.model.MetricGroup;
import io.softwareecg.collector.config.model.ProcessDef;
import io.softwareecg.collector.source.prometheus.PrometheusCounterSource;

import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Turns a {@link PrometheusPlan} into a collector configuration, see {@link CollectorConfigCompiler}.
 * <p>
 * The Prometheus server is the one host of the configuration; every selected group becomes a
 * metric group with its expressions and one collector.
 */
public final class PrometheusConfigCompiler {

    static final String NODE_GROUP = "prometheus";

    private PrometheusConfigCompiler() {
    }

    public static String toYaml(PrometheusPlan plan) {
        Map<String, MetricGroup> metricGroups = new LinkedHashMap<>();
        List<CollectorDef> collectors = new ArrayList<>();
        Duration interval = Duration.ofMillis(Math.max(1_000, plan.interval().toMillis()));

        for (PrometheusPlan.GroupSelection selection : plan.groups()) {
            List<Metric> metrics = new ArrayList<>();
            for (MetricExpression expression : selection.expressions()) {
                boolean named = expression.name() != null && !expression.name().isBlank();
                metrics.add(new Metric(expression.expression(), named ? expression.name() : null, null, null));
            }
            metricGroups.put(selection.group(), new MetricGroup(PrometheusCounterSource.ID, metrics));
            // systems that appear or vanish are picked up every ten collection cycles
            collectors.add(CollectorDef.remote(selection.group(), NODE_GROUP, NODE_GROUP, interval, interval.multipliedBy(10)));
        }

        ProcessDef server = new ProcessDef("prometheus", null, null,
                "lookback=" + Math.max(0, plan.lookback().toSeconds()) + "s");

        return CollectorConfigurations.toYaml(new CollectorConfig(
                plan.projectName(),
                metricGroups,
                Map.of(NODE_GROUP, List.of(server)),
                Map.of(NODE_GROUP, server(plan.url(), plan.username(), plan.password() != null && !plan.password().isBlank())),
                null,
                collectors));
    }

    /**
     * Changes how the source is reached: address, user and whether a password or token is used.
     * What is collected stays as it is.
     *
     * @param config      a configuration written by {@link #toYaml(PrometheusPlan)}
     * @param url         the new address of the Prometheus server
     * @param username    the user for basic authentication, or blank
     * @param withSecret  whether a password or token is used (it is kept in the secret store)
     */
    public static String withConnection(String config, String url, String username, boolean withSecret) {
        CollectorConfig read = CollectorConfigurations.read(config);
        if (!read.hostGroups().containsKey(NODE_GROUP)) {
            throw new IllegalArgumentException("The configuration has no host to change.");
        }
        Map<String, HostGroup> hostGroups = new LinkedHashMap<>(read.hostGroups());
        hostGroups.put(NODE_GROUP, server(url, username, withSecret));
        return CollectorConfigurations.toYaml(new CollectorConfig(read.project(), read.metricGroups(),
                read.processGroups(), hostGroups, read.platform(), read.collectors()));
    }

    private static HostGroup server(String url, String username, boolean withSecret) {
        return new HostGroup(null, null, List.of(new Host("prometheus", url,
                username == null || username.isBlank() ? null : username,
                withSecret ? CollectionPlan.SECRET_PLACEHOLDER : null)));
    }
}

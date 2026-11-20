package io.softwareecg.awb.datasource.bl;

import io.softwareecg.awb.datasource.bl.prometheus.MetricExpression;
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
 * Turns an {@link AnalysisPlan} into a collector configuration. The result is an ordinary
 * {@code config.yaml}: the embedded collector of the AWB runs it, and it can be handed to a
 * stand-alone collector on another machine.
 * <p>
 * The configuration never contains the password or token. In its place stands the placeholder
 * {@link #SECRET_PLACEHOLDER}, which a stand-alone collector resolves from the environment
 * variable {@value #SECRET_ENVIRONMENT_VARIABLE}. For the embedded collector the AWB fills the
 * secret in from its secret store, in memory only ({@link #withSecret(String, String)}).
 * <p>
 * The Prometheus server is the one host of the configuration; every selected group becomes a
 * metric group with its expressions and one collector.
 */
public final class CollectorConfigCompiler {

    static final String NODE_GROUP = "prometheus";

    /**
     * The environment variable a stand-alone collector reads the password or token from.
     */
    public static final String SECRET_ENVIRONMENT_VARIABLE = "EKG_SOURCE_SECRET";

    /**
     * What stands in the configuration in place of the password or token.
     */
    public static final String SECRET_PLACEHOLDER = "${ENV:" + SECRET_ENVIRONMENT_VARIABLE + "}";

    private CollectorConfigCompiler() {
    }

    public static String toYaml(AnalysisPlan plan) {
        Map<String, MetricGroup> metricGroups = new LinkedHashMap<>();
        List<CollectorDef> collectors = new ArrayList<>();
        Duration interval = Duration.ofMillis(Math.max(1_000, plan.interval().toMillis()));

        for (AnalysisPlan.GroupSelection selection : plan.groups()) {
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
     * Fills the secret into a configuration, for handing it to the embedded collector. The
     * result must not be stored or shown.
     *
     * @param config a configuration without its secret, as the compilers of the AWB write it
     * @param secret the password or token; {@code null} leaves the configuration as it is
     */
    public static String withSecret(String config, String secret) {
        if (secret == null) {
            return config;
        }
        CollectorConfig read = CollectorConfigurations.read(config);
        Map<String, HostGroup> hostGroups = new LinkedHashMap<>();
        read.hostGroups().forEach((id, group) -> {
            List<Host> hosts = new ArrayList<>();
            for (Host host : group.hosts()) {
                hosts.add(new Host(host.name(), host.address(), host.username(), filled(host.password(), secret)));
            }
            hostGroups.put(id, new HostGroup(group.username(), filled(group.password(), secret), hosts));
        });
        return CollectorConfigurations.toYaml(new CollectorConfig(read.project(), read.metricGroups(),
                read.processGroups(), hostGroups, read.platform(), read.collectors()));
    }

    /**
     * Changes how the source is reached: address, user and whether a password or token is used.
     * What is collected stays as it is.
     *
     * @param config      a configuration written by {@link #toYaml(AnalysisPlan)}
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

    /**
     * @return the configuration as YAML. Configurations that were stored by an earlier version
     *         of the AWB are XML; they are converted.
     */
    public static String asYaml(String config) {
        return CollectorConfigurations.isLegacyXml(config)
                ? CollectorConfigurations.toYaml(CollectorConfigurations.read(config)) : config;
    }

    private static HostGroup server(String url, String username, boolean withSecret) {
        return new HostGroup(null, null, List.of(new Host("prometheus", url,
                username == null || username.isBlank() ? null : username,
                withSecret ? SECRET_PLACEHOLDER : null)));
    }

    private static String filled(String password, String secret) {
        return password == null ? null : password.replace(SECRET_PLACEHOLDER, secret);
    }
}

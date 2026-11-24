package io.softwareecg.awb.datasource.adapter.collector;

import io.softwareecg.collector.config.CollectorConfigurations;
import io.softwareecg.collector.config.model.CollectorConfig;
import io.softwareecg.collector.config.model.Host;
import io.softwareecg.collector.config.model.HostGroup;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * What the configurations of all kinds of sources have in common. A configuration is an ordinary
 * {@code config.yaml}: the embedded collector of the AWB runs it, and it can be handed to a
 * stand-alone collector on another machine.
 * <p>
 * It never contains the password or token. In its place stands the placeholder
 * {@link #SECRET_PLACEHOLDER}, which a stand-alone collector resolves from the environment
 * variable {@value #SECRET_ENVIRONMENT_VARIABLE}. For the embedded collector the AWB fills the
 * secret in from its secret store, in memory only ({@link #withSecret(String, String)}).
 */
public final class CollectorConfigCompiler {

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
     * @return the configuration as YAML. Configurations that were stored by an earlier version
     *         of the AWB are XML; they are converted.
     */
    public static String asYaml(String config) {
        return CollectorConfigurations.isLegacyXml(config)
                ? CollectorConfigurations.toYaml(CollectorConfigurations.read(config)) : config;
    }

    private static String filled(String password, String secret) {
        return password == null ? null : password.replace(SECRET_PLACEHOLDER, secret);
    }
}

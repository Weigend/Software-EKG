package io.softwareecg.collector.config.impl;

import io.softwareecg.collector.config.CollectorConfiguration;
import io.softwareecg.collector.config.model.CollectorConfig;
import io.softwareecg.collector.config.ClassicCollectorJobConfiguration;
import io.softwareecg.collector.config.CloudCollectorJobConfiguration;
import io.softwareecg.collector.config.SidecarCollectorJobConfiguration;

import java.util.List;

/**
 * The implementation behind {@link io.softwareecg.collector.config.CollectorConfigurations}.
 */
public final class ConfigurationReader {

    private ConfigurationReader() {
    }

    public static CollectorConfig read(String text) {
        return LegacyXmlConfig.isXml(text) ? LegacyXmlConfig.read(text) : YamlConfig.read(text);
    }

    public static boolean isLegacyXml(String text) {
        return LegacyXmlConfig.isXml(text);
    }

    public static String toYaml(CollectorConfig config) {
        return YamlConfig.write(config);
    }

    public static CollectorConfiguration jobsOf(CollectorConfig config) {
        config.validate();
        List<ClassicCollectorJobConfiguration> remote = JobConfigurationFactory.remoteJobs(config);
        List<SidecarCollectorJobConfiguration> sidecar = JobConfigurationFactory.sidecarJobs(config);
        return new Jobs(config, remote, sidecar);
    }

    private record Jobs(CollectorConfig model,
                        List<ClassicCollectorJobConfiguration> remote,
                        List<SidecarCollectorJobConfiguration> sidecar) implements CollectorConfiguration {

        @Override
        public List<ClassicCollectorJobConfiguration> getClassicCollectorJobConfiguration() {
            return remote;
        }

        @Override
        public List<CloudCollectorJobConfiguration> getCloudCollectorJobConfiguration() {
            // collectors that find their pods through the platform are not built
            return List.of();
        }

        @Override
        public List<SidecarCollectorJobConfiguration> getSidecarCollectorConfiguration() {
            return sidecar;
        }
    }
}

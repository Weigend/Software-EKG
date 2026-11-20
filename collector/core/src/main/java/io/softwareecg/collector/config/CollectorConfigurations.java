package io.softwareecg.collector.config;

import io.softwareecg.collector.config.impl.ConfigurationReader;
import io.softwareecg.collector.config.model.CollectorConfig;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

/**
 * Where collector configurations come from: a {@code config.yaml}, its text, or a
 * {@link CollectorConfig} put together in a program.
 * <p>
 * A configuration is YAML. A file in the XML format of earlier versions is still read, so that
 * what exists keeps running; {@link #toYaml(CollectorConfig)} of {@link #read(String)} converts it.
 */
public final class CollectorConfigurations {

    private CollectorConfigurations() {
    }

    /**
     * @throws InvalidConfigurationException if the file is not readable or not a valid configuration
     */
    public static CollectorConfiguration fromFile(File file) {
        try {
            return fromString(Files.readString(file.toPath(), StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new InvalidConfigurationException("The configuration " + file + " is not readable: " + e.getMessage(), e);
        } catch (InvalidConfigurationException e) {
            throw new InvalidConfigurationException(file + ": " + e.getDetail(), e);
        }
    }

    /**
     * @param text a configuration as YAML (or as XML of an earlier version)
     * @throws InvalidConfigurationException if it is not a valid configuration
     */
    public static CollectorConfiguration fromString(String text) {
        return of(read(text));
    }

    /**
     * @throws InvalidConfigurationException if the configuration is incomplete, see {@link CollectorConfig#validate()}
     */
    public static CollectorConfiguration of(CollectorConfig config) {
        return ConfigurationReader.jobsOf(config);
    }

    /**
     * Reads a configuration without turning it into jobs, for looking at it or changing it.
     *
     * @param text a configuration as YAML (or as XML of an earlier version)
     * @throws InvalidConfigurationException if it is not a valid configuration
     */
    public static CollectorConfig read(String text) {
        return ConfigurationReader.read(text);
    }

    /**
     * @return {@code true} if the text is a configuration in the XML format of earlier versions
     */
    public static boolean isLegacyXml(String text) {
        return ConfigurationReader.isLegacyXml(text);
    }

    /**
     * @return the configuration as the text of a {@code config.yaml}
     */
    public static String toYaml(CollectorConfig config) {
        return ConfigurationReader.toYaml(config);
    }
}

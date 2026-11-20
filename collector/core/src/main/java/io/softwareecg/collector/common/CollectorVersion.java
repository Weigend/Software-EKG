package io.softwareecg.collector.common;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * The version of the collector, as the build wrote it down. There is no version number in the
 * code or in the scripts: the one in the build ({@code revision} in the pom.xml) is the only one.
 */
public final class CollectorVersion {

    private static final String VERSION = read();

    private CollectorVersion() {
    }

    /**
     * @return the version, for example {@code 7.1.0}; empty if the build left none
     */
    public static String get() {
        return VERSION;
    }

    private static String read() {
        try (InputStream in = CollectorVersion.class.getResourceAsStream("/io/softwareecg/collector/version.properties")) {
            if (in == null) {
                return "";
            }
            Properties properties = new Properties();
            properties.load(in);
            String version = properties.getProperty("version", "").strip();
            // not built by Maven: the placeholder is still there
            return version.startsWith("${") ? "" : version;
        } catch (IOException e) {
            return "";
        }
    }
}

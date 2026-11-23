package io.softwareecg.awb.importer.adapter.prometheus;

import io.softwareecg.awb.runtime.config.EkgConfigStore;

import java.util.List;
import java.util.Map;

import static io.softwareecg.awb.importer.adapter.prometheus.PrometheusImportModule.IMPORT_MODULE_ID;
import static io.softwareecg.awb.runtime.config.ModuleType.IMPORT_MODULE;

/**
 * Config provider for the Prometheus import module that knows
 * the context of each config values and provide it via a domain specific API.
 *
 * @since 18.11.2018
 */
public class PrometheusModuleConfig {

    private static final EkgConfigStore EKG_CONFIG_STORE = EkgConfigStore.getInstance();

    /**
     * Returns a (whitelist) list of regex expressions the the Prometheus import module can use
     * to filter Prometheus time series before retrieve the whole metadata from the remote database.
     *
     * The filter expressions will read from the series-filter-regex.conf config file that has to be in
     * the configs/PrometheusImporter/ directory of the Software-EKG application path.
     * If no config is located at that place a default config from the static classpath will used instead.
     *
     * @return a list of regex filter expression to filter series from Prometheus
     */
    public static List<String> getRegexSeriesFilter() {
        return EKG_CONFIG_STORE.getListConfig(IMPORT_MODULE, IMPORT_MODULE_ID, "series-filter-regex");
    }

    /**
     * Returns a (whitelist) list of full-match (equal) expressions the the Prometheus import module can use
     * to filter Prometheus time series before retrieve the whole metadata from the remote database.
     *
     * The filter expressions will read from the series-filter-regex.conf config file that has to be in
     * the configs/PrometheusImporter/ directory of the Software-EKG application path.
     * If no config is located at that place a default config from the static classpath will used instead.
     *
     * @return a list of regex filter expression to filter series from Prometheus
     */
    public static List<String> getStandardSeriesFilter() {
        return EKG_CONFIG_STORE.getListConfig(IMPORT_MODULE, IMPORT_MODULE_ID, "series-filter-std");
    }

    /**
     * Returns the configured import time range for local imports.
     * <p>
     * The configuration is read from local-import.conf and can be:
     * <ul>
     *   <li>"all" - import all available data from Prometheus</li>
     *   <li>A number - import the last N hours of data</li>
     * </ul>
     *
     * @return the import range configuration, or "all" as default
     */
    public static String getLocalImportRange() {
        Map<String, String> config = EKG_CONFIG_STORE.getMapConfig(IMPORT_MODULE, IMPORT_MODULE_ID, "local-import");
        String range = config.get("import.range");
        return range != null ? range.trim() : "all";
    }
}

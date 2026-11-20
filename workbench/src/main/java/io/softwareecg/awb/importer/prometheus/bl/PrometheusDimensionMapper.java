package io.softwareecg.awb.importer.prometheus.bl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.softwareecg.awb.importer.prometheus.bl.GenericPrometheusMapper.DimensionalMetric;
import io.softwareecg.awb.sdk.core.log.EkgLogger;
import io.softwareecg.awb.sdk.datamodel.RawSeriesData;
import io.softwareecg.awb.sdk.datamodel.SeriesData;
import io.softwareecg.awb.sdk.importer.tasks.steps.mapper.RawToMetadataMapper;
import org.slf4j.Logger;

import java.util.Map;

/**
 * Mapper class that converts a given {@link RawSeriesData} to a SeriesMetaData filled
 * with the properties from the raw series.
 * <p>
 * Uses the {@link GenericPrometheusMapper} for deterministic dimension mapping:
 * <ul>
 *   <li>ProcessGroup = job label</li>
 *   <li>Process = instance label (full URL)</li>
 *   <li>Host = first part of instance (before dot or colon)</li>
 *   <li>HostGroup = "cluster"</li>
 *   <li>MetricGroup = first segment of metric name after job prefix strip</li>
 *   <li>Metric = remaining segments with label suffixes</li>
 * </ul>
 * The mapper also creates a unique key of the source data that allows the workbench to
 * fetch the time series data from Prometheus.
 *
 * @since 31.07.2018
 */
public class PrometheusDimensionMapper implements RawToMetadataMapper<RawSeriesData, SeriesData> {

    private static final Logger LOGGER = EkgLogger.get();

    private final String projectName;
    private final GenericPrometheusMapper prometheusMapper;

    /**
     * Creates a new mapper with default settings.
     * <p>
     * Note: For optimal results, the prometheusMapper should be pre-analyzed with
     * all available metrics before mapping. Use {@link #PrometheusDimensionMapper(String, GenericPrometheusMapper)}
     * with a pre-analyzed mapper for better MetricGroup distribution.
     *
     * @param projectName the project name to use for all mapped series
     */
    public PrometheusDimensionMapper(String projectName) {
        this(projectName, new GenericPrometheusMapper());
    }

    /**
     * Creates a new mapper with a pre-configured GenericPrometheusMapper.
     * <p>
     * Use this constructor when you have pre-analyzed the metrics for optimal
     * MetricGroup distribution.
     *
     * @param projectName the project name to use for all mapped series
     * @param prometheusMapper the pre-configured/analyzed mapper
     */
    public PrometheusDimensionMapper(String projectName, GenericPrometheusMapper prometheusMapper) {
        this.projectName = projectName;
        this.prometheusMapper = prometheusMapper;
    }

    @Override
    public SeriesData apply(RawSeriesData rawSeriesData) {

        try {
            String remoteSeriesKey = new ObjectMapper().writeValueAsString(rawSeriesData.getSeriesLabels());

            // Use the new deterministic mapping
            DimensionalMetric dm = prometheusMapper.transform(rawSeriesData);
            
            Map<String, String> labels = rawSeriesData.getSeriesLabels();
            
            // For _info metrics, dm will be null - create minimal series data
            if (dm == null) {
                LOGGER.debug("Skipping _info metric: {}", rawSeriesData.getMetricName());
                return null;
            }

            SeriesData seriesMetaData = new SeriesData(
                    projectName,
                    dm.hostGroup(),                                    // HostGroup: "cluster"
                    dm.host(),                                         // Host: first part of instance
                    labels.getOrDefault("namespace", "*"),             // Namespace: from label
                    labels.getOrDefault("service", "*"),               // Service: from label
                    labels.getOrDefault("pod", "*"),                   // Pod: from label
                    labels.getOrDefault("container", "*"),             // Container: from label
                    "*",                                               // Measurement: set at import time
                    dm.processGroup(),                                 // ProcessGroup: job name
                    dm.process(),                                      // Process: full instance URL
                    dm.metricGroup(),                                  // MetricGroup: first segment
                    dm.metric()                                        // Metric: with label suffixes
            );

            seriesMetaData.setRemoteSeriesKey(remoteSeriesKey);

            return seriesMetaData;

        } catch (JsonProcessingException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * Returns the underlying GenericPrometheusMapper for analysis operations.
     */
    public GenericPrometheusMapper getPrometheusMapper() {
        return prometheusMapper;
    }
}

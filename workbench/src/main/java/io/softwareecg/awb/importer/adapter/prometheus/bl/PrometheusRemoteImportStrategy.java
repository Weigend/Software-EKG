package io.softwareecg.awb.importer.adapter.prometheus.bl;

import io.softwareecg.awb.importer.adapter.prometheus.client.PrometheusClient;
import io.softwareecg.awb.importer.adapter.prometheus.client.model.api.ResultStatus;
import io.softwareecg.awb.importer.adapter.prometheus.client.model.api.label.ListLabelResult;
import io.softwareecg.awb.importer.adapter.prometheus.client.model.api.query.SeriesDefinition;
import io.softwareecg.awb.timeseries.port.TimeSeriesImportSource;
import io.softwareecg.awb.runtime.logging.EkgLogger;
import io.softwareecg.awb.secrets.security.SecurityContext;
import io.softwareecg.awb.timeseries.RawSeriesData;
import io.softwareecg.awb.timeseries.SeriesData;
import io.softwareecg.awb.importer.framework.SplitSourceProjectImportStrategy;
import io.softwareecg.awb.importer.framework.steps.mapper.RawToMetadataMapper;
import org.slf4j.Logger;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

/**
 * An RemoteProject import strategy that defines the required steps to import the
 * metric metadata from a Prometheus server.
 *
 * @since 31.07.2018
 */
public class PrometheusRemoteImportStrategy extends SplitSourceProjectImportStrategy {

    private static final Logger LOGGER = EkgLogger.get();

    private PrometheusClient prometheusClient;

    //=================================================================================================================
    // overwritten API methods of the SplitSourceProjectImportStrategy super class
    //=================================================================================================================

    /* (non-Javadoc)
     * @see io.softwareecg.awb.importer.framework.SplitSourceProjectImportStrategy#readRawSeriesData
     */
    @Override
    protected Stream<RawSeriesData> readRawSeriesData(TimeSeriesImportSource sourceRepositoryDefinition) {

        setupPrometheusClient(sourceRepositoryDefinition);

        ListLabelResult result = prometheusClient.listMetrics();

        if (ResultStatus.SUCCESS != result.getStatus()) {
          LOGGER.error("Retrieve error during queering metrics from {}", sourceRepositoryDefinition.getRepositoryUrl());
        }

        if (result.getData() == null || result.getData().length == 0) {
            return Stream.empty();
        }

        final int totalAmountMetrics = result.getData().length;
        final AtomicInteger currentMetricNumber = new AtomicInteger(0);

        notifyImportStarted("start to import " + totalAmountMetrics + " metrics from Prometheus");

        PrometheusMetricFilter filter = new PrometheusMetricFilter();
        
        return Stream.of(result.getData())
                .peek(metricName -> notifyProgress(currentMetricNumber.incrementAndGet(), totalAmountMetrics))
                .filter(filter)
                .filter(metricName -> !metricName.endsWith("_info")) // Skip _info metrics
                .flatMap(metricName -> {
                    var seriesResult = prometheusClient.listSeriesForMetric(metricName);
                    if (seriesResult == null || seriesResult.getData() == null) {
                        return Stream.empty();
                    }
                    return Stream.of(seriesResult.getData());
                })
                .map(PrometheusRemoteImportStrategy::toRawData);
    }

    /**
     * Return an RawToMetadataMapper implementation that is aware for Prometheus
     * series data and know how to map generic Prometheus labels to the correct EKG filter dimensions.
     *
     * @return an RawToMetadataMapper implementation
     */
    @Override
    public RawToMetadataMapper<RawSeriesData, SeriesData> getRawToMetadataMapper(String projectName) {
        return new PrometheusDimensionMapper(projectName);
    }

    //=================================================================================================================
    // internal helper logic
    //=================================================================================================================

    /**
     * Retrieves and configure the Prometheus client proxy that will used to load the time series
     * metadata from the Prometheus database specified by the given {@link TimeSeriesImportSource} instance.
     *
     * @param sourceRepository the source definition that specifies interface endpoints
     *                                   and credentials for the Prometheus database
     */
    private void setupPrometheusClient(TimeSeriesImportSource sourceRepository) {
        prometheusClient = new PrometheusClient();
        prometheusClient.bindToAddress(sourceRepository.getRepositoryUrl());

        SecurityContext securityContext = new SecurityContext();
        securityContext.setUserName(sourceRepository.getUsername());
        securityContext.setPassword(sourceRepository.getPassword());
        securityContext.setResourceAuthType(sourceRepository.getAuthType());
        securityContext.setSsoOAuthServer(sourceRepository.getOAuthServerUrl());

        prometheusClient.setSecurityContext(securityContext);
    }

    /**
     * Converts the given {@link SeriesDefinition} element to a {@link RawSeriesData}
     * instance by copy all labels and according values to the generic filter dimensions.
     *
     * @param seriesDefinition the SeriesDefinition from Prometheus
     * @return the RawSeriesData that represents the Prometheus series data.
     */
    private static RawSeriesData toRawData(SeriesDefinition seriesDefinition) {

        RawSeriesData rawSeriesData = new RawSeriesData(seriesDefinition.getMetricName());

        for (String label : seriesDefinition.getLabels()) {
            rawSeriesData.addLabel(label, seriesDefinition.getLabelValue(label));
        }

        return rawSeriesData;
    }

}

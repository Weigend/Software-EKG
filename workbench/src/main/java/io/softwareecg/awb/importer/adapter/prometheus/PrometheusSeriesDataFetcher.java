package io.softwareecg.awb.importer.adapter.prometheus;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.softwareecg.awb.importer.adapter.prometheus.client.PrometheusClient;
import io.softwareecg.awb.importer.adapter.prometheus.client.model.query.MatrixSeries;
import io.softwareecg.awb.importer.adapter.prometheus.client.model.query.QueryResult;
import io.softwareecg.awb.importer.adapter.prometheus.client.model.query.RangeQuery;
import io.softwareecg.awb.repository.model.TimeSeriesImportSource;
import io.softwareecg.awb.runtime.logging.EkgLogger;
import io.softwareecg.awb.secrets.security.SecurityContext;
import io.softwareecg.awb.timeseries.Value;
import io.softwareecg.awb.importer.spi.RemoteSeriesDataFetcher;
import org.slf4j.Logger;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * This class is an implementation of the {@link RemoteSeriesDataFetcher} interface
 * for the Prometheus time series database.
 * It will fetch time series data using the Prometheus 2.x v1 REST API with an
 * EKG own API client.
 *
 * @since 01.08.2018
 */
public class PrometheusSeriesDataFetcher implements RemoteSeriesDataFetcher {

    private static final Logger LOGGER = EkgLogger.get();

    private static final String PROMETHEUS_METRIC_KEY = "METRIC_NAME";

    private PrometheusClient prometheusClient = null;

    //=================================================================================================================
    // implementation of the RemoteSeriesDataFetcher interface
    //=================================================================================================================

    @Override
    public Value[] fetchSeriesData(String remoteSeriesKey, long start, long end) {

        if (prometheusClient == null) {
            String errorMsg = "No PrometheusClient was initialized before querying metric data.";
            LOGGER.error(errorMsg);
            throw new IllegalStateException(errorMsg);
        }

        Map<String, String> metricLabels = convertToMap(remoteSeriesKey);
        RangeQuery rangeQuery = RangeQuery.Builder
                .newRangeQuery()
                .forMetric(metricLabels.get(PROMETHEUS_METRIC_KEY))
                .withStartEndTimeStamp(start, end)
                .build();

        for (Map.Entry<String, String> entry : metricLabels.entrySet()) {
            if (PROMETHEUS_METRIC_KEY.equals(entry.getKey())) {
                continue;
            }
            rangeQuery.addLabelFilterParam(entry.getKey(), entry.getValue());
        }

        QueryResult queryResult = prometheusClient.queryRange(rangeQuery);

        if (queryResult.getData() == null) {
            // this occurs in error cases for example if Prometheus isn't available
            return null;
        }

        MatrixSeries[] matrixSeriesArray = queryResult.getData().getResult();

        if (matrixSeriesArray.length == 0) {
            return new Value[0];
        }

        if (matrixSeriesArray.length > 1) {
            String errorMsg = "The query with params: '" + remoteSeriesKey + "' results in an ambiguous result.";
            LOGGER.error(errorMsg);
            throw new IllegalStateException(errorMsg);
        }

        double[][] seriesData = matrixSeriesArray[0].getValues();
        Value[] resultValues = new Value[seriesData.length];

        for (int i =0; i < seriesData.length; i++) {
            resultValues[i] = new Value((long)(seriesData[i][0] * 1000), seriesData[i][1]);
        }

        return resultValues;
    }

    @Override
    public Value[] fetchSeriesData(Map<String, String> searchLabelExp, long start, long end) {
        return new Value[0];
    }

    @Override
    public void setSourceRepositoryDefinition(TimeSeriesImportSource sourceRepository) {
        prometheusClient = new PrometheusClient();
        prometheusClient.bindToAddress(sourceRepository.getRepositoryUrl());

        SecurityContext securityContext = new SecurityContext();
        securityContext.setUserName(sourceRepository.getUsername());
        securityContext.setPassword(sourceRepository.getPassword());
        securityContext.setResourceAuthType(sourceRepository.getAuthType());
        securityContext.setSsoOAuthServer(sourceRepository.getOAuthServerUrl());

        prometheusClient.setSecurityContext(securityContext);
    }


    //=================================================================================================================
    // internal helper logic
    //=================================================================================================================


    private Map<String, String> convertToMap(String jsonValueMap) {

        ObjectMapper mapper = new ObjectMapper();
        TypeReference<HashMap<String, String>> typeRef = new TypeReference<>() {};

        try {
            return mapper.readValue(jsonValueMap, typeRef);
        }
        catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }
}

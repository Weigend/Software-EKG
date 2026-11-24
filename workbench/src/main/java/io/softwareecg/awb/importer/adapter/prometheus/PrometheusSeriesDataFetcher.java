package io.softwareecg.awb.importer.adapter.prometheus;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.softwareecg.awb.importer.adapter.prometheus.client.PrometheusClient;
import io.softwareecg.awb.importer.adapter.prometheus.client.model.query.MatrixSeries;
import io.softwareecg.awb.importer.adapter.prometheus.client.model.query.QueryResult;
import io.softwareecg.awb.importer.adapter.prometheus.client.model.query.RangeQuery;
import io.softwareecg.awb.repository.api.TimeSeriesImportSource;
import io.softwareecg.awb.runtime.logging.EkgLogger;
import io.softwareecg.awb.secrets.security.SecurityContext;
import io.softwareecg.awb.timeseries.Value;
import org.slf4j.Logger;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Reads the values of series from one Prometheus server, using the Prometheus 2.x v1 REST API
 * with an EKG own API client.
 *
 * @since 01.08.2018
 */
public class PrometheusSeriesDataFetcher {

    private static final Logger LOGGER = EkgLogger.get();

    private static final String PROMETHEUS_METRIC_KEY = "METRIC_NAME";

    private final PrometheusClient prometheusClient;

    /**
     * @param source the server to read from
     */
    public PrometheusSeriesDataFetcher(TimeSeriesImportSource source) {
        prometheusClient = new PrometheusClient();
        prometheusClient.bindToAddress(source.getRepositoryUrl());

        SecurityContext securityContext = new SecurityContext();
        securityContext.setUserName(source.getUsername());
        securityContext.setPassword(source.getPassword());
        securityContext.setResourceAuthType(source.getAuthType());
        securityContext.setSsoOAuthServer(source.getOAuthServerUrl());

        prometheusClient.setSecurityContext(securityContext);
    }

    /**
     * @return the values of the series; {@code null} if the server cannot be reached
     */
    public Value[] fetchSeriesData(String remoteSeriesKey, long start, long end) {

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

package io.softwareecg.awb.importer.adapter.prometheus.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import io.softwareecg.awb.importer.adapter.prometheus.client.model.api.label.ListLabelResult;
import io.softwareecg.awb.importer.adapter.prometheus.client.model.api.query.ListSeriesResult;
import io.softwareecg.awb.importer.adapter.prometheus.client.model.api.query.QueryResult;
import io.softwareecg.awb.importer.adapter.prometheus.client.model.api.query.RangeQuery;
import io.softwareecg.awb.runtime.events.AwbErrorEvent;
import io.softwareecg.wfx.platform.api.EventBus;
import io.softwareecg.awb.runtime.logging.EkgLogger;
import io.softwareecg.wfx.lookup.api.Lookup;
import io.softwareecg.awb.secrets.security.RequestSecHeaderEnricher;
import io.softwareecg.awb.secrets.security.SecurityContext;
import org.apache.http.HttpResponse;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustAllStrategy;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.ssl.SSLContextBuilder;
import org.slf4j.Logger;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;

import static com.fasterxml.jackson.databind.DeserializationFeature.READ_ENUMS_USING_TO_STRING;

/**
 * Client proxy for the Prometheus time series server REST API v1
 * (tested against Prometheus v2.3.1) that can use to query/read
 * time series data.
 * No write/update use cases are supported.
 *
 * @since 12.07.2018
 */
public class PrometheusClient {

    private static final Logger LOGGER = EkgLogger.get();

    private static final String ERR_MSG = "Could not access Prometheus. Reason: ";

    private static final String API_APPENDIX = "/v1";

    private static final String QUERY_API_APPENDIX = "/query";

    private static final String LABEL_API_APPENDIX = "/label";

    private static final String SERIES_API_APPENDIX = "/series?match[]=";

    private String stdQueryUrl;

    private String labelQueryUrl;

    private String seriesQueryUrl;

    private CloseableHttpClient httpClient;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private String prometheusApiUrl;

    private SecurityContext securityContext;

    private RequestSecHeaderEnricher requestSecHeaderEnricher;

    //=================================================================================================================
    // constructors of the PrometheusClient class
    //=================================================================================================================

    @SuppressWarnings("unused") // used via CDI
    public PrometheusClient() {
        // NoOp
    }

    /**
     * Constructs a new PrometheusClient instance that will use
     * the given absolute url to call the Prometheus server API.
     * The URL must be the absolute URL inclusive protocol, host, domain, and
     * the URI to the API endpoint (without version).
     * Example: http://localhost:9090/api/
     *
     * @param prometheusApiUrl the absolute URL to the Prometheus REST API endpoint
     */
    public PrometheusClient(String prometheusApiUrl) {
        this.prometheusApiUrl = prometheusApiUrl;
    }

    //=================================================================================================================
    // public API to setup the PrometheusClient
    //=================================================================================================================

    /**
     * Binds the given endpoint URL to the Prometheus client
     * that points to the REST API the client will use.
     * The method can be used if the client was instanced without any
     * parameters or to change it at runtime.
     * <p>
     * The URL can be provided in multiple formats:
     * <ul>
     *   <li>http://localhost:9090 (base URL - /api will be added)</li>
     *   <li>http://localhost:9090/ (base URL with slash)</li>
     *   <li>http://localhost:9090/api (explicit API path)</li>
     *   <li>http://localhost:9090/api/ (explicit API path with slash)</li>
     *   <li>http://localhost:9090/api/v1 (full path - v1 will be stripped)</li>
     * </ul>
     *
     * @param prometheusApiUrl the absolute URL to the Prometheus server or REST API endpoint
     */
    public void bindToAddress(String prometheusApiUrl) {

        RequestConfig.Builder requestBuilder = RequestConfig.custom();
        requestBuilder = requestBuilder.setConnectTimeout(10_000);
        requestBuilder = requestBuilder.setConnectionRequestTimeout(30_000);
        requestBuilder = requestBuilder.setSocketTimeout(40_000);

        httpClient = HttpClientBuilder.create()
                .setSSLSocketFactory(createAcceptAllSslFactory())
                .setDefaultRequestConfig(requestBuilder.build())
                .build();

        // Normalize the URL: remove trailing slashes and version paths
        String apiUrl = prometheusApiUrl.replaceAll("(/v1|/v1/|/)?$", "");
        
        // Add /api suffix if not present
        if (!apiUrl.endsWith("/api")) {
            apiUrl = apiUrl + "/api";
        }
        
        stdQueryUrl = apiUrl + API_APPENDIX + QUERY_API_APPENDIX;
        labelQueryUrl = apiUrl + API_APPENDIX + LABEL_API_APPENDIX;
        seriesQueryUrl = apiUrl + API_APPENDIX + SERIES_API_APPENDIX;
        
        LOGGER.info("Prometheus client bound to: {} (query: {}, label: {}, series: {})", 
                prometheusApiUrl, stdQueryUrl, labelQueryUrl, seriesQueryUrl);
    }


    public void setSecurityContext(SecurityContext securityContext) {
        this.securityContext = securityContext;
        this.requestSecHeaderEnricher = new RequestSecHeaderEnricher(securityContext);
    }

    //=================================================================================================================
    // public API to retrieve series data via PrometheusClient
    //=================================================================================================================

    /**
     * Queries the Prometheus label API endpoint to retrieve and return
     * an array of all metric name that are known by the Prometheus instance.
     *
     * @return a ListLabelResult instance that contains the array of metrics and additional meta data like error codes
     */
    public ListLabelResult listMetrics() {

        checkClient();

        HttpUriRequest getRequest = new HttpGet(labelQueryUrl + "/__name__/values");
        requestSecHeaderEnricher.configureRequest(getRequest);

        try (CloseableHttpResponse response = httpClient.execute(getRequest)) {

            if (!checkResponse(response)) {
                return new ListLabelResult();
            }

            final ObjectReader jacksonReader = objectMapper
                    .readerFor(ListLabelResult.class)
                    .with(READ_ENUMS_USING_TO_STRING);

            return jacksonReader.readValue(response.getEntity().getContent());

        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * Queries the Prometheus query API endpoint to retrieve and return
     * time series data for a specified time period.
     * An interval must always specified. If the caller doesn't set it in the RangeQuery parameter
     * the default interval of it will used instead.
     *
     * @param rangeQuery the range query used to fetch the data from Prometheus
     * @return a {@link QueryResult} instance that contain the fetched data as object representation or errors
     */
    public QueryResult queryRange(RangeQuery rangeQuery) {

        checkClient();

        HttpUriRequest request = new HttpGet(stdQueryUrl + rangeQuery.getQueryString());
        requestSecHeaderEnricher.configureRequest(request);

        try (CloseableHttpResponse response = httpClient.execute(request)) {

            if (!checkResponse(response)) {
                return new QueryResult();
            }

            final ObjectReader jacksonReader = objectMapper
                    .readerFor(QueryResult.class)
                    .with(READ_ENUMS_USING_TO_STRING);

            return jacksonReader.readValue(response.getEntity().getContent());

        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * Queries the Prometheus series API endpoint to retrieve and return
     * the label data for a single metric specified by the 'metricName' parameter.
     * <p>
     * The labels will add various filter dimension to the metric for example on which
     * host it was measured. This will result in a list of unique records (metric + labels)
     * that will return in a {@link ListSeriesResult} instance that also contains additional
     * metadata about the service call itself like error information's.
     *
     * @param metricName the name of the metric the client should fetch the labels for
     * @return a ListSeriesResult container with the label data and additional metadata about the service call
     */
    public ListSeriesResult listSeriesForMetric(String metricName) {

        checkClient();

        HttpGet getRequest = new HttpGet(seriesQueryUrl + metricName);
        requestSecHeaderEnricher.configureRequest(getRequest);

        try (CloseableHttpResponse response = httpClient.execute(getRequest)) {

            if (!checkResponse(response)) {
                return new ListSeriesResult();
            }

            final ObjectReader jacksonReader = objectMapper
                    .readerFor(ListSeriesResult.class)
                    .with(READ_ENUMS_USING_TO_STRING);

            return jacksonReader.readValue(response.getEntity().getContent());

        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    //=================================================================================================================
    // internal helper logic
    //=================================================================================================================


    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    private boolean checkResponse(HttpResponse response) {

        int statusCode = response.getStatusLine().getStatusCode();
        String statusMessage = response.getStatusLine().getReasonPhrase();

        if (statusCode == 403) {
            LOGGER.error("Unable to call the Prometheus API because " +
                    "the access is forbidden. Reason-Phrase: " + statusMessage);

            Lookup.lookup(EventBus.class).publish(new AwbErrorEvent(this,
                    ERR_MSG + statusMessage, null));
            return false;

        } else if (statusCode == 405) {
            LOGGER.error("Unable to call the Prometheus API because the method (POST/GET) isn't allowed. " +
                    "Status-Message: " + statusMessage);

            Lookup.lookup(EventBus.class).publish(new AwbErrorEvent(this,
                    ERR_MSG + statusMessage, null));
            return false;

        } else if (statusCode == 503) {
            LOGGER.error("Unable to call the Prometheus API because the Prometheus instance isn't available. " +
                    "Status-Message: " + statusMessage);

            Lookup.lookup(EventBus.class).publish(new AwbErrorEvent(this,
                    ERR_MSG + statusMessage, null));
            return false;


        } else if (statusCode >= 500) {
            LOGGER.error("Unable to call the Prometheus API because there is a server-side error. " +
                    "Status-Message: " + statusMessage);

            Lookup.lookup(EventBus.class).publish(new AwbErrorEvent(this,
                    ERR_MSG + statusMessage, null));
            return false;


        } else if (statusCode >= 400) {
            LOGGER.error("Retrieve an error trying to call the Prometheus API: Status-Message: " + statusMessage);

            Lookup.lookup(EventBus.class).publish(new AwbErrorEvent(this,
                    ERR_MSG + statusMessage, null));
            return false;

        }

        return true;
    }


    private void checkClient() {
        if (httpClient == null) {
            bindToAddress(this.prometheusApiUrl);
        }
    }

    /**
     * Returns a new instance of SSLConnectionSocketFactory that use an TrustStrategy that
     * accepts all SSL certificates independent if they are valid or not.
     *
     * @return the factory instance
     */
    private static SSLConnectionSocketFactory createAcceptAllSslFactory() {

        try {

            // use the TrustSelfSignedStrategy to allow Self Signed Certificates
            SSLContext sslContext = SSLContextBuilder
                    .create()
                    .loadTrustMaterial(new TrustAllStrategy())
                    .build();

            // we can optionally disable hostname verification.
            // if you don't want to further weaken the security, you don't have to include this.
            HostnameVerifier allowAllHosts = new NoopHostnameVerifier();

            // create an SSL Socket Factory to use the SSLContext with the trust self signed certificate strategy
            // and allow all hosts verifier.
            return new SSLConnectionSocketFactory(sslContext, allowAllHosts);

        } catch (KeyManagementException | NoSuchAlgorithmException | KeyStoreException e) {
            throw new IllegalStateException(e);
        }
    }
}
package io.softwareecg.awb.importer.testsupport.mocks;

import io.softwareecg.awb.repository.api.Repository;
import io.softwareecg.awb.repository.api.RepositoryException;
import io.softwareecg.awb.timeseries.codec.DateValuePairCodec;
import io.softwareecg.awb.timeseries.TimeSeries;
import org.apache.http.client.HttpClient;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustStrategy;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.client.solrj.request.UpdateRequest;
import org.apache.solr.client.solrj.response.SolrPingResponse;
import org.apache.solr.common.SolrInputDocument;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * ImportService implementation that collects the data in memory for testing usage and also
 * write it to a remote Solr instance.
 *
 * @since 24.03.2020
 */
public class HybridSeriesImportService implements CollectorSeriesImportService {

    private final String solrUrl;

    private final SolrClient solrClient;

    private final String solrCollection;

    private boolean projectDataDeleted = false;

    private final List<TimeSeries> timeSeries = new ArrayList<>();

    public HybridSeriesImportService(String solrHostName, int solrPort, String solrCollection, boolean useHttps) {
        if (useHttps) {
            solrUrl = "https://" + solrHostName + ":" + solrPort + "/solr/";
        } else {
            solrUrl = "http://" + solrHostName + ":" + solrPort + "/solr/";
        }

        this.solrCollection = solrCollection;

        solrClient= new HttpSolrClient.Builder(solrUrl)
                .withHttpClient(buildAnyCertAcceptingHttpClient())
                .build();
    }

    @Override
    public String getRepositoryId() {
            return null;
        }

    @Override
    public long resolveLastUpdate(TimeSeries seriesWithoutValues) throws RepositoryException {
        return 0;
    }

    @Override
    public void add(TimeSeries series) throws RepositoryException {
        timeSeries.add(series);

        if (ping()) {

            if (!projectDataDeleted) {
                getDeleteProjectData(series.getProject());
            }

            addDocument(series);
        }
    }

    @Override
    public void add(Stream<TimeSeries> series) throws RepositoryException {
        timeSeries.addAll(series.collect(Collectors.toList()));

        if (ping()) {
            if (!projectDataDeleted && !timeSeries.isEmpty()) {
                getDeleteProjectData(timeSeries.get(0).getProject());
            }
            addDocuments(timeSeries.iterator());
        }
    }

    @Override
    public void add(Iterable<TimeSeries> series) throws RepositoryException {
        series.forEach(timeSeries::add);

        if (ping()) {
            if (!projectDataDeleted && !timeSeries.isEmpty()) {
                getDeleteProjectData(timeSeries.get(0).getProject());
            }
            addDocuments(timeSeries.iterator());
        }
    }

    @Override
    public void commit() throws RepositoryException {
        if (ping()) {
            try {
                UpdateRequest commitRequest = new UpdateRequest();
                commitRequest.setAction(UpdateRequest.ACTION.COMMIT, true, true);

                commitRequest.process(solrClient, solrCollection);

                projectDataDeleted = false;
            } catch (SolrServerException | IOException e) {
                throw new RepositoryException("I/O error accessing Solr.", e);
            }
        }
    }

    @Override
    public List<TimeSeries> getCollectedSeriesData() {
        return timeSeries;
    }

    //=================================================================================================================
    // remote Solr client implementation
    //=================================================================================================================


    private void getDeleteProjectData(String projectName) {
        try {
            UpdateRequest deleteRequest = new UpdateRequest();
            deleteRequest.deleteByQuery("type:TIME_SERIES AND project_name:" + projectName);
            deleteRequest.setCommitWithin(-1);


            deleteRequest.process(solrClient, solrCollection);
        } catch (Exception e) {
            throw new IllegalStateException("I/O error accessing Solr.", e);
        }
    }


    public boolean ping() {
        try {
            SolrPingResponse pingResponse = solrClient.ping(solrCollection);
            return pingResponse.getStatus() < 400;

        } catch (SolrServerException | IOException e) {
            //throw new IllegalStateException("Ping to Solr persistence layer failed", e);
            return false;
        }
    }

    public void addDocuments(Iterator<TimeSeries> entities) {
        try {
            UpdateRequest updateRequest = new UpdateRequest();
            updateRequest.setDocIterator(new Iterator<>() {

                @Override
                public boolean hasNext() {
                    return entities.hasNext();
                }

                @Override
                public SolrInputDocument next() {
                    TimeSeries timeSeries = entities.next();
                    if (timeSeries == null) return null;
                    return mapToSolrInputDocument(timeSeries);
                }

                @Override
                public void remove() {
                    entities.remove();
                }
            });
//
//            if (StringUtils.isNotBlank(username)) {
//                updateRequest.setBasicAuthCredentials(username, password);
//            }

            updateRequest.process(solrClient, solrCollection);

        } catch (SolrServerException | IOException e) {
            throw new IllegalStateException("Exception at Solr persistence layer occurred:", e);
        }
    }

    private void addDocument(TimeSeries entity) {
        try {
            UpdateRequest updateRequest = new UpdateRequest();
            updateRequest.add(mapToSolrInputDocument(entity));
            updateRequest.setCommitWithin(-1);

//            if (StringUtils.isNotBlank(username)) {
//                updateRequest.setBasicAuthCredentials(username, password);
//            }

            updateRequest.process(solrClient, solrCollection);
        } catch (SolrServerException | IOException e) {
            throw new IllegalStateException("Exception occurred during add a new entity to Solr", e);
        }
    }


    // ================================================================================================================
    //  internal helper
    // ================================================================================================================

    private static SolrInputDocument mapToSolrInputDocument(TimeSeries seriesEntity) {

        SolrInputDocument inputDocument = new SolrInputDocument();

        addIfNotBlank(inputDocument, "type", "TIME_SERIES");
        addIfNotBlank(inputDocument, "project_name", seriesEntity.getProject());
        addIfNotBlank(inputDocument, "ts_measurement", seriesEntity.getMeasurement());

        addIfNotBlank(inputDocument, "ts_hostGroup", seriesEntity.getHostGroup());
        addIfNotBlank(inputDocument, "ts_host", seriesEntity.getHost());
        addIfNotBlank(inputDocument, "ts_process", seriesEntity.getProcess());

        addIfNotBlank(inputDocument, "ts_namespace", seriesEntity.getMetricName());
        addIfNotBlank(inputDocument, "ts_container", seriesEntity.getContainer());
        addIfNotBlank(inputDocument, "ts_pod", seriesEntity.getPod());
        addIfNotBlank(inputDocument, "ts_service", seriesEntity.getService());

        addIfNotBlank(inputDocument, "ts_metricName", seriesEntity.getMetricName());
        addIfNotBlank(inputDocument, "ts_metricGroup", seriesEntity.getMetricGroup());

        addIfNotBlank(inputDocument, "ts_data_amountValues", seriesEntity.getValues().size());
        addIfNotBlank(inputDocument, "ts_importDate", new Date());

        addIfNotBlank(inputDocument, "ts_start", new Date(seriesEntity.getStartDate()));
        addIfNotBlank(inputDocument, "ts_end", new Date(seriesEntity.getEndDate()));

        addIfNotBlank(inputDocument, "ts_data", DateValuePairCodec.compressAndEncodeDataPlainBytes(seriesEntity.getValues()));

        return inputDocument;
    }

    private static void addIfNotBlank(SolrInputDocument document, String fieldName, Object value) {
        if (value != null && !((value instanceof String) && "".equals(value))) {
            document.addField(fieldName, value);
        }
    }

    /**
     * Factory method that builds a new Apache HttpClient that accept
     * any SSL certificate used by the remote independent if self signed or whatever.
     */
    private HttpClient buildAnyCertAcceptingHttpClient() {

        return HttpClients
                .custom()
                .setSSLSocketFactory(getAcceptAllSLLFactory())
                .build();
    }

    /**
     * Creates a SSL socket factory that will wont check either the remote host names
     * nor the signing of the host certificates.
     * This is not secure but required if we don't want to import the certificates
     * of each environment.
     *
     * @return a SSLConnectionSocketFactory instance that won't check any of the SSL secure mechanisms
     */
    private SSLConnectionSocketFactory getAcceptAllSLLFactory()  {

        try {

            // use the TrustSelfSignedStrategy to allow Self Signed Certificates
            SSLContext sslContext = SSLContextBuilder
                    .create()
                    .loadTrustMaterial((TrustStrategy) (chain, authType) -> true)
                    .build();

            // we can optionally disable hostname verification.
            // if you don't want to further weaken the security, you don't have to include this.
            HostnameVerifier allowAllHosts = new NoopHostnameVerifier();

            // create an SSL Socket Factory to use the SSLContext with the trust self signed certificate strategy
            // and allow all hosts verifier.
            return new SSLConnectionSocketFactory(sslContext, allowAllHosts);

        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

}

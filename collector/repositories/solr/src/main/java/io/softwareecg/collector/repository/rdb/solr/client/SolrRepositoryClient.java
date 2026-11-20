package io.softwareecg.collector.repository.rdb.solr.client;

import io.softwareecg.collector.repository.rdb.api.RdbRepositoryClient;
import io.softwareecg.collector.repository.rdb.api.RdbRepositoryException;
import io.softwareecg.collector.repository.rdb.api.RdbRepositoryNotAvailableException;
import io.softwareecg.collector.repository.rdb.api.TimeSeries;
import io.softwareecg.collector.repository.rdb.api.querymodel.delete.DeleteParams;
import io.softwareecg.collector.repository.rdb.api.querymodel.search.SearchParams;
import io.softwareecg.collector.repository.rdb.api.querymodel.search.SearchResult;
import io.softwareecg.collector.repository.rdb.api.querymodel.status.QueryStatus;
import io.softwareecg.collector.repository.rdb.solr.client.util.ObjectBinder;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrRequest;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.CloudSolrClient;
import org.apache.solr.client.solrj.io.SolrClientCache;
import org.apache.solr.client.solrj.request.QueryRequest;
import org.apache.solr.client.solrj.request.UpdateRequest;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.common.params.CommonParams;
import org.apache.solr.common.params.CursorMarkParams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.ConnectException;
import java.util.*;

/**
 * An implementation of the {@link RdbRepositoryClient} interface that
 * will proxy an Solr (Cloud/Class) based remote repository.
 * This implementation currently support Solr version 6.x -7.x.
 *
 * @since 31.10.2018
 */
public class SolrRepositoryClient implements RdbRepositoryClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(SolrRepositoryClient.class);
    private static final String IO_ERROR_MESSAGE = "I/O error accessing Solr.";

    /**
     * The name of the system property to set the maximum length of all filter-queries.
     * If the maximum is exceeded, the logging of the fq-parameter will be suppressed.
     */
    private static final String PROP_MAX_FQ_LENGTH = "psmghub.searchIndexAccess.maxFqLength";

    /**
     * The default value for the maximum length of all filter-queries.
     * If the maximum is exceeded, the logging of the fq-parameter will be suppressed.
     */
    private static final int DEFAULT_MAX_FQ_LENGTH = 2048;

    // If zkHost is set, we internally use a CloudSolrClient, otherwise we use a HttpSolrClient - see constructor.
    private final SolrClientCache solrClientCache;
    private final String zkHost;
    private final String baseUrl;
    private final String username;
    private final String password;

    private final String collection;
    private final SolrClient solrClient;
    private final SolrQueryFactory queryFactory;

    /**
     * Constructs a {@link SolrRepositoryClient} bound to the given Solr collection, i.e. all queries will be directed
     * to this collection.
     *
     * @param collection  the Solr collection
     * @param clientCache cache providing {@link CloudSolrClient}s and
     *                    {@link org.apache.solr.client.solrj.impl.HttpSolrClient}s
     * @param zkHosts     ZooKeeper hosts as comma-separated string (may be null). If this is set, we internally
     *                    use a {@link CloudSolrClient} - always use this for internal access from within the
     *                    cluster
     * @param baseUrl     The baseUrl of one Solr node (may be null). If this is set, we internally use a
     *                    {@link org.apache.solr.client.solrj.impl.HttpSolrClient} - use this for external access
     *                    from outside the cluster (e.g. from a test driver in module tests).
     * @param username      the username for BasicAuth authentication or null if no authentication required
     * @param password      the password for BasicAuth authentication or null if no authentication required
     */
    public SolrRepositoryClient(String collection, SolrClientCache clientCache, String zkHosts,
                                               String baseUrl, String username, String password) {
        this(collection, clientCache, zkHosts, baseUrl, username, password, new SolrQueryFactory());
    }

    public SolrRepositoryClient(String collection, SolrClientCache clientCache, String zkHosts,
                                               String baseUrl, String username, String password, SolrQueryFactory queryFactory) {
        this.collection = collection;
        this.solrClientCache = clientCache;
        this.zkHost = zkHosts;
        this.baseUrl = baseUrl;
        this.username = username;
        this.password = password;
        this.solrClient = null;
        this.queryFactory = queryFactory;
    }


    @Override
    public <T> SearchResult<T> search(Class<T> type, SearchParams searchParams) throws RdbRepositoryException {
        try {
            validateSearchParams(type, searchParams);

            SolrQuery solrQuery = queryFactory.createSelectQuery(type, searchParams);

            if (searchParams.hasCursor()) {
                solrQuery.set(CursorMarkParams.CURSOR_MARK_PARAM, searchParams.getCursorMark());
            }

            return executeSearchQuery(type, solrQuery);

        } catch (SolrServerException e) {

            if (e.getRootCause() instanceof ConnectException) {
                throw new RdbRepositoryNotAvailableException(e);
            }

            throw new RdbRepositoryException(IO_ERROR_MESSAGE, e);

        } catch (IOException e) {
            throw new RdbRepositoryException(IO_ERROR_MESSAGE, e);
        }
    }

    @Override
    public void add(Collection<TimeSeries> entities) throws RdbRepositoryException {
        try {

            Iterator<TimeSeries> entityIterator = entities.iterator();

            UpdateRequest updateRequest = new UpdateRequest();

            updateRequest.setDocIterator(new Iterator<>() {

                @Override
                public boolean hasNext() {
                    return entityIterator.hasNext();
                }

                @Override
                public SolrInputDocument next() {
                    Object o = entityIterator.next();
                    if (o == null) return null;
                    return ObjectBinder.mapToSolrInputDocument(o);
                }

                @Override
                public void remove() {
                    entityIterator.remove();
                }
            });

            updateRequest.setCommitWithin(-1);

            if (StringUtils.isNotBlank(username)) {
                updateRequest.setBasicAuthCredentials(username, password);
            }

            updateRequest.process(getSolrClient(), collection);

        } catch (SolrServerException | IOException e) {
            LOGGER.error("Exception at Solr persistence layer occurred: ", e);
            throw new RdbRepositoryException(IO_ERROR_MESSAGE, e);
        }
    }

    @Override
    public void add(TimeSeries entity) throws RdbRepositoryException {
        try {
            UpdateRequest updateRequest = new UpdateRequest();
            updateRequest.add(ObjectBinder.mapToSolrInputDocument(entity));
            updateRequest.setCommitWithin(-1);

            if (StringUtils.isNotBlank(username)) {
                updateRequest.setBasicAuthCredentials(username, password);
            }

            updateRequest.process(getSolrClient(), collection);
        } catch (SolrServerException | IOException e) {
            throw new RdbRepositoryException(IO_ERROR_MESSAGE, e);
        }
    }

    @Override
    public void delete(DeleteParams deleteParams) throws RdbRepositoryException {
        try {
            UpdateRequest deleteRequest = new UpdateRequest();
            String deleteQuery = queryFactory.createDeleteQuery(deleteParams);

            deleteRequest.deleteByQuery(deleteQuery);
            deleteRequest.setCommitWithin(-1);

            if (StringUtils.isNotBlank(username)) {
                deleteRequest.setBasicAuthCredentials(username, password);
            }

            deleteRequest.process(getSolrClient(), collection);
        } catch (SolrServerException | IOException e) {
            throw new RdbRepositoryException(IO_ERROR_MESSAGE, e);
        }
    }

    @Override
    public void commit() throws RdbRepositoryException {
        try {
            UpdateRequest commitRequest = new UpdateRequest();
            commitRequest.setAction(UpdateRequest.ACTION.COMMIT, true, true);

            if (StringUtils.isNotBlank(username)) {
                commitRequest.setBasicAuthCredentials(username, password);
            }

            commitRequest.process(getSolrClient(), collection);
        } catch (SolrServerException | IOException e) {
            throw new RdbRepositoryException(IO_ERROR_MESSAGE, e);
        }
    }

    @Override
    public void close() {
        // No op in this implementation because we use a singleton CloudSolrClient passed in from outside.
        // The interface still implements Closeable so that we can change the client usage without outside changes.
    }


    //=================================================================================================================
    //
    //=================================================================================================================

    /**
     * Returns a {@link SolrClient} to use for requests. A {@link CloudSolrClient} is used if {@link #zkHost} is set,
     * otherwise a {@link org.apache.solr.client.solrj.impl.HttpSolrClient} with {@link #baseUrl}.
     *
     * @return the {@link SolrClient}
     */
    private SolrClient getSolrClient() {
        if (solrClient != null) {
            return solrClient;
        } else if (!StringUtils.isBlank(zkHost)) {
            return solrClientCache.getCloudSolrClient(zkHost);
        } else if (!StringUtils.isBlank(baseUrl)) {
            return solrClientCache.getHttpSolrClient(baseUrl);
        } else {
            throw new IllegalStateException("Neither zkHost nor baseUrl are set - can't create a SolrClient");
        }
    }

    private <T> void validateSearchParams(Class<T> type, SearchParams searchParams) {
        Validate.notNull(type);
        Validate.notNull(searchParams);
        Validate.notNull(searchParams.getFilterQueries());
    }

    private <T> SearchResult<T> executeSearchQuery(Class<T> type, SolrQuery solrQuery)
            throws SolrServerException, IOException {
        QueryResponse response = executeSolrQuery(solrQuery);
        List<T> documents = ObjectBinder.mapToBean(type, response.getResults());
        return new SearchResult<>(documents, createQueryStatus(response));
    }

    /**
     * Creates a {@link QueryStatus} for the given Solr {@link QueryResponse}.
     *
     * @param response the Solr {@link QueryResponse}.
     * @return the {@link QueryStatus}
     */
    private QueryStatus createQueryStatus(QueryResponse response) {
        return new QueryStatus(response.getResults().getNumFound(), response.getQTime(), response.getNextCursorMark());
    }

    /**
     * Executes the given {@link SolrQuery}.
     *
     * @param solrQuery the query
     * @return the response
     * @throws SolrServerException if there is an error on the server
     * @throws IOException         If there is a low-level I/O error.
     */
    private QueryResponse executeSolrQuery(SolrQuery solrQuery) throws SolrServerException, IOException {
        SolrQuery modSolrQuery = reduceLogOverhead(solrQuery);
        SolrClient solrClient = getSolrClient();

        QueryRequest queryRequest = new QueryRequest(modSolrQuery, SolrRequest.METHOD.GET);

        if (StringUtils.isNotBlank(username)) {
            queryRequest.setBasicAuthCredentials(username, password);
        }

        return queryRequest.process(solrClient, collection);
    }


    /**
     * Disables logging of the fq-parameter of the length of all filter-queries exceeds a configurable maximum.
     *
     * @param solrQuery The Solr query.
     * @return The modified Solr query.
     */
    private SolrQuery reduceLogOverhead(SolrQuery solrQuery) {
        // if the query contains no filter-queries, then abort
        String[] filterQueries = solrQuery.getFilterQueries();
        if (filterQueries == null || filterQueries.length == 0) {
            return solrQuery;
        }

        // if the length of the filter-queries doesn't exceed the maximum, then abort
        int currFqLength = Arrays.stream(filterQueries).mapToInt(String::length).sum();
        if (currFqLength <= getMaxFilterQueryLength()) {
            return solrQuery;
        }

        // otherwise, log only the non-fq parameters
        Set<String> paramNames = new TreeSet<>(solrQuery.getParameterNames());
        paramNames.remove(CommonParams.FQ);
        String logParamsList = StringUtils.join(paramNames, ",");

        return solrQuery.setParam(CommonParams.LOG_PARAMS_LIST, logParamsList);
    }

    /**
     * Returns the maximum length of all filter-queries. If this value is exceeded, then logging of the
     * fq-parameter is suppressed.
     * <p/>
     * The parameter can be set via the system property {@value #PROP_MAX_FQ_LENGTH}. If unset, then
     * the default value {@value DEFAULT_MAX_FQ_LENGTH} is used.
     *
     * @return The maximum length of all filter-queries.
     */
    private int getMaxFilterQueryLength() {
        String value = System.getProperty(PROP_MAX_FQ_LENGTH);
        return StringUtils.isNumeric(value)
                ? Integer.parseUnsignedInt(value)
                : DEFAULT_MAX_FQ_LENGTH;
    }
}

package io.softwareecg.awb.repository.adapter;

import java.util.List;
import io.softwareecg.awb.repository.persistence.schema.EkgSchemaField;
import io.softwareecg.awb.repository.persistence.schema.DocumentType;
import io.softwareecg.awb.repository.persistence.DeleteParams;
import io.softwareecg.awb.repository.api.RepositoryException;
import io.softwareecg.awb.repository.connection.EkgRepository;
import io.softwareecg.awb.repository.api.SeriesSearch;
import io.softwareecg.awb.repository.persistence.TimeSeriesRepository;
import io.softwareecg.awb.repository.persistence.search.expr.ExprFactory;
import io.softwareecg.awb.repository.series.StoredSeriesSearch;
import io.softwareecg.awb.repository.connection.TimeSeriesRepositoryProvider;
import io.softwareecg.awb.repository.connection.EkgRepositoryDbType;
import io.softwareecg.awb.secrets.model.ResourceAuthType;
import io.softwareecg.awb.runtime.logging.EkgLogger;
import io.softwareecg.awb.runtime.discovery.ServiceDiscovery;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.Map;

/**
 * The base class of all EKG repositories that provides the common
 * logic to instantiate the TimeSeriesRepository with the connection settings of the current repository instance.
 *
 * @since 22.09.2018
 */
public abstract class RepositoryAdapterBase implements EkgRepository {

    /**
     * The logger used by this instance to communicate errors
     * and special events.
     */
    protected static final Logger LOGGER = EkgLogger.get();

    /**
     * The unique id of the types that will created by Solr itself
     * that the record that defines this types get persisted.
     */
    protected String id;

    /**
     * The type of the types
     * (Solr Cloud, Solr Classic Standalone or ElasticSearch)
     */
    protected EkgRepositoryDbType ekgRepositoryDbType;

    /**
     * The unique alias name of the types
     */
    protected String repositoryName;

    /**
     * The connection URL of the types
     * (host + port or complete URL with URI parts depending on types type)
     */
    protected String connectionUrl;

    /**
     * The name of Solr database index. In case of this classic Solr repository
     * this field keep <code>null</code> because the name of the Solr Core
     * should given via connection string.
     * In case of Solr Cloud repositories it will filled with the Collection name.
     */
    protected String dbIndexName;

    /**
     * The authentication type the types used
     * for client connections
     */
    protected ResourceAuthType repositoryAuthType;

    /**
     * The username used for authentication (optional)
     */
    protected String username;

    /**
     * The password used for authentication (optional)
     */
    protected String password;

    /**
     * The client that provides access to the underlying database layer
     * and implement the low-level API methods for CRUD actions.
     */
    protected TimeSeriesRepository cachedClient;

    /**
     * A simple map used as cache for Service classes resolved via CDI
     * and reflection to save performance than the services will
     * used again and again.
     * Because the service is bind to a specific repository the cache must be here.
     */
    protected Map<Class<?>, Object> serviceCache = new HashMap<>();

    //=================================================================================================================
    // Repository accessor API
    //=================================================================================================================

    @Override
    public boolean isEmbedded() {
        return false;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getRepositoryName() {
        return repositoryName;
    }

    @Override
    public String getConnectionUrl() {
        return connectionUrl;
    }

    @Override
    public String getDBIndexName() {
        return dbIndexName;
    }

    @Override
    public EkgRepositoryDbType getEkgRepositoryDbType() {
        return ekgRepositoryDbType;
    }

    @Override
    public ResourceAuthType getRepositoryAuthType() {
        return repositoryAuthType;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public String getPassword() {
        return password;
    }

    /**
     * Returns the EKG repository specific client factory.
     * Each deriving class should provide it's own factory implementation.
     *
     * @return the client factory for this repository type
     */
    protected abstract TimeSeriesRepositoryProvider getRepositoryClientFactory();

    //=================================================================================================================
    // Repository service API
    //=================================================================================================================

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getBoundedService(Class<T> serviceInterface) {
        if (serviceCache.containsKey(serviceInterface)) {
            return (T) serviceCache.get(serviceInterface);
        }

        try {
            // fails here, with the reason, if the repository cannot be connected
            getRepositoryClient();
            // an instance of its own: the bean of the injection is the same for all repositories
            T service = ServiceDiscovery.lookup(serviceInterface, this);
            serviceCache.put(serviceInterface, service);
            return service;

        } catch (Exception e) {
            LOGGER.info("Repository '{}' is offline: {}", getRepositoryName(), e.getMessage());
            LOGGER.debug("Detailed connection failure:", e);
            return null;
        }
    }

    @Override
    public TimeSeriesRepository getRepositoryClient() {

        if (cachedClient == null) {
            String connectionUrl = getConnectionUrl();
            if (StringUtils.isNotBlank(getDBIndexName())) {
                connectionUrl = connectionUrl.replaceAll("/+$", "") + "/" + getDBIndexName();
            }

            cachedClient = getRepositoryClientFactory().createRepositoryClient(connectionUrl, getUsername(), getPassword());
        }

        return cachedClient;
    }

    @Override
    public boolean isReachable() {
        return getRepositoryClient().ping();
    }

    @Override
    public long countValues() throws RepositoryException {
        return getRepositoryClient().sumFieldValue(EkgSchemaField.TS_DATA_AMOUNT_VALUES, List.of());
    }

    @Override
    public long countValuesOf(String projectName) throws RepositoryException {
        return getRepositoryClient().sumFieldValue(EkgSchemaField.TS_DATA_AMOUNT_VALUES,
                List.of(ExprFactory.exactFilter(EkgSchemaField.PROJECT_NAME, projectName)));
    }

    @Override
    public SeriesSearch series() {
        return new StoredSeriesSearch(getRepositoryClient());
    }

    @Override
    public void deleteTimeSeriesOf(String projectName) throws RepositoryException {
        DeleteParams timeSeries = new DeleteParams();
        timeSeries.addFilter(EkgSchemaField.DOC_TYPE, DocumentType.TIME_SERIES.toString());
        timeSeries.addFilter(EkgSchemaField.PROJECT_NAME, projectName);
        TimeSeriesRepository client = getRepositoryClient();
        client.delete(timeSeries);
        client.commit();
    }
}

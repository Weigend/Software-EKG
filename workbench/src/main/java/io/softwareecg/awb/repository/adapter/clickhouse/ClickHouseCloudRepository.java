package io.softwareecg.awb.repository.adapter.clickhouse;

import io.avaje.inject.External;
import io.softwareecg.awb.repository.api.EkgRepository;
import io.softwareecg.awb.repository.port.TimeSeriesRepository;
import io.softwareecg.awb.repository.spi.TimeSeriesRepositoryProvider;
import io.softwareecg.awb.repository.model.EkgRepositoryDbType;
import io.softwareecg.awb.secrets.model.ResourceAuthType;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import io.softwareecg.awb.repository.adapter.RepositoryAdapterBase;
/**
 * An EkgRepository implementation that represents a Software-EKG repository built
 * on top of a remote ClickHouse cluster for high-performance time series storage.
 *
 * <p>ClickHouse is a column-oriented DBMS optimized for OLAP queries and time series data.
 * Compared to the Solr-based repositories, ClickHouse offers significantly better compression
 * for time series data through specialized codecs (DoubleDelta for timestamps, Gorilla for
 * float values) and columnar storage.</p>
 */
public class ClickHouseCloudRepository extends RepositoryAdapterBase {

    protected final TimeSeriesRepositoryProvider repositoryClientFactory;

    public ClickHouseCloudRepository(TimeSeriesRepositoryProvider repositoryClientFactory) {
        this.repositoryClientFactory = repositoryClientFactory;
    }

    public ClickHouseCloudRepository(String id,
                                     String repositoryName,
                                     String connectionUrl,
                                     String dbIndexName,
                                     EkgRepositoryDbType ekgRepositoryDbType,
                                     ResourceAuthType resourceAuthType,
                                     String username,
                                     String password,
                                     TimeSeriesRepositoryProvider repositoryClientFactory) {

        this.id = id;
        this.repositoryName = repositoryName;
        this.connectionUrl = connectionUrl;
        this.dbIndexName = dbIndexName;
        this.ekgRepositoryDbType = ekgRepositoryDbType;
        this.repositoryAuthType = resourceAuthType;
        this.username = username;
        this.password = password;
        this.repositoryClientFactory = repositoryClientFactory;
    }

    @Override
    public TimeSeriesRepository getRepositoryClient() {

        if (cachedClient == null) {
            cachedClient = repositoryClientFactory.createRepositoryClient(
                    getConnectionUrl(), resolveTableName(), getUsername(), getPassword());
        }

        return cachedClient;
    }

    private String resolveTableName() {
        return ClickHouseRepositoryNaming.resolveTableName(getDBIndexName(), getRepositoryName(), getId());
    }

    @Override
    protected TimeSeriesRepositoryProvider getRepositoryClientFactory() {
        return repositoryClientFactory;
    }

    /**
     * Factory to create a {@link ClickHouseCloudRepository}.
     */
    @Singleton
    public static class Factory implements EkgRepository.Factory {

        protected final TimeSeriesRepositoryProvider clientFactory;

        @Inject
        public Factory(@External @ClickHouse TimeSeriesRepositoryProvider clientFactory) {
            this.clientFactory = clientFactory;
        }

        /** No-arg constructor for tests that mock the factory client. */
        protected Factory() {
            this.clientFactory = null;
        }

        @Override
        public EkgRepository getInstance(String id,
                                         String name,
                                         String connectionUrl,
                                         String dbIndexName,
                                         EkgRepositoryDbType ekgRepositoryDbType,
                                         ResourceAuthType resourceAuthType,
                                         String username,
                                         String password) {

            if (!isTypeSupported(ekgRepositoryDbType)) {
                throw new IllegalArgumentException("Can not create ClickHouse repository of type " + ekgRepositoryDbType);
            }

            return new ClickHouseCloudRepository(id, name, connectionUrl, dbIndexName,
                    ekgRepositoryDbType, resourceAuthType, username, password,
                    clientFactory);
        }

        @Override
        public boolean isTypeSupported(EkgRepositoryDbType type) {
            return type == EkgRepositoryDbType.CLICKHOUSE_CLOUD;
        }
    }
}

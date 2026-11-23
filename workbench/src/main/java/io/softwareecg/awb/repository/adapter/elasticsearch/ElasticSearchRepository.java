package io.softwareecg.awb.repository.adapter.elasticsearch;

import io.avaje.inject.External;
import io.softwareecg.awb.repository.api.EkgRepository;
import io.softwareecg.awb.timeseries.spi.TimeSeriesRepositoryProvider;
import io.softwareecg.awb.timeseries.spi.EkgRepositoryDbType;
import io.softwareecg.awb.timeseries.repositorytype.ElasticSearch;
import io.softwareecg.awb.secrets.model.ResourceAuthType;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import io.softwareecg.awb.repository.adapter.RepositoryAdapterBase;
/**
 * An EkgRepository implementation that represents a Software-EKG repository build
 * on top of a ElasticSearch NoSQL database.
 * <p>
 * Constructor-injection model (post-Avaje migration): the {@link TimeSeriesRepositoryProvider}
 * is now passed in via the constructor by the inner {@link Factory}. The legacy
 * {@code @Inject @ElasticSearch} field-injection has been removed because it requires
 * mutable repository instances created via CDI's {@code Instance<T>}.get() pattern,
 * which Avaje does not support.
 *
 * @since 22.09.2018
 */
public class ElasticSearchRepository extends RepositoryAdapterBase {

    protected final TimeSeriesRepositoryProvider repositoryClientFactory;

    /**
     * Convenience constructor used by the {@link Factory} when only the framework
     * dependency is known yet — the data fields can still be set afterwards
     * (used by tests that build the repository step by step).
     */
    public ElasticSearchRepository(TimeSeriesRepositoryProvider repositoryClientFactory) {
        this.repositoryClientFactory = repositoryClientFactory;
    }

    /**
     * Initialize an ElasticSearch repository.
     *
     * @param repositoryName        the alias name of the types
     * @param connectionUrl         the connection url used to reach the ElasticSearch server in the network
     * @param ekgRepositoryDbType   the type of this types.
     * @param resourceAuthType      the authentication type the types used for client connections
     * @param username              the optional username used for authentication if authentication type is not "none".
     * @param password              the optional password used for authentication if authentication type is not "none".
     */
    public ElasticSearchRepository(String id,
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
    protected TimeSeriesRepositoryProvider getRepositoryClientFactory() {
        return repositoryClientFactory;
    }

    /**
     * Factory to create an {@link ElasticSearchRepository}.
     */
    @Singleton
    public static class Factory implements EkgRepository.Factory {

        protected final TimeSeriesRepositoryProvider clientFactory;

        @Inject
        public Factory(@External @ElasticSearch TimeSeriesRepositoryProvider clientFactory) {
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
                throw new IllegalArgumentException("Can not create ElasticSearch types of type " + ekgRepositoryDbType);
            }

            return new ElasticSearchRepository(id, name, connectionUrl, dbIndexName,
                    ekgRepositoryDbType, resourceAuthType, username, password,
                    clientFactory);
        }

        @Override
        public boolean isTypeSupported(EkgRepositoryDbType type) {
            return type == EkgRepositoryDbType.ELASTICSEARCH_STANDALONE;
        }
    }
}

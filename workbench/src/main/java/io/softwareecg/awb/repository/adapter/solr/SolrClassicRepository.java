//______________________________________________________________________________
//
//                  Project:    Software EKG
//______________________________________________________________________________
package io.softwareecg.awb.repository.adapter.solr;

import io.avaje.inject.External;
import io.softwareecg.awb.repository.api.EkgRepository;
import io.softwareecg.awb.repository.spi.TimeSeriesRepositoryProvider;
import io.softwareecg.awb.repository.spi.EkgRepositoryDbType;
import io.softwareecg.awb.secrets.model.ResourceAuthType;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import io.softwareecg.awb.repository.adapter.RepositoryAdapterBase;
/**
 * An EkgRepository implementation that represents a Software-EKG repository build
 * on top of a classic standalone Solr search index.
 * <p>
 * Constructor-injection model (post-Avaje migration): the {@link TimeSeriesRepositoryProvider}
 * is passed in via the constructor by the inner {@link Factory}. The legacy
 * {@code @Inject @Solr @Remote} field-injection is gone; child classes
 * ({@code SolrCloudRepository}) call the protected constructor with the same factory.
 *
 * @since 03.06.2018
 */
public class SolrClassicRepository extends RepositoryAdapterBase {

    protected final TimeSeriesRepositoryProvider repositoryClientFactory;

    /**
     * Convenience constructor used by tests that build the repository step by step.
     */
    public SolrClassicRepository(TimeSeriesRepositoryProvider repositoryClientFactory) {
        this.repositoryClientFactory = repositoryClientFactory;
    }

    /**
     * Initialize a Solr repository with all properties.
     *
     * @param id                    the repository identifier
     * @param repositoryName        the alias name of the types
     * @param connectionUrl         the connection url used to reach Solr (host + ip for SolrCloud)
     * @param dbIndexName           the index/collection name within the Solr instance
     * @param ekgRepositoryDbType   the type of this repository
     * @param resourceAuthType      the authentication type used for client connections
     * @param username              optional username used when auth-type is not "none"
     * @param password              optional password used when auth-type is not "none"
     * @param repositoryClientFactory the (qualified) TimeSeriesRepositoryProvider to build clients from
     */
    public SolrClassicRepository(String id,
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
     * Factory to create a {@link SolrClassicRepository}.
     */
    @Singleton
    public static class Factory implements EkgRepository.Factory {

        protected final TimeSeriesRepositoryProvider clientFactory;

        @Inject
        public Factory(@External @Solr TimeSeriesRepositoryProvider clientFactory) {
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
                throw new IllegalArgumentException("Can not create solr types of type " + ekgRepositoryDbType);
            }

            return new SolrClassicRepository(id, name, connectionUrl, dbIndexName,
                    ekgRepositoryDbType, resourceAuthType, username, password,
                    clientFactory);
        }

        @Override
        public boolean isTypeSupported(EkgRepositoryDbType type) {
            return type == EkgRepositoryDbType.SOLR_STANDALONE;
        }
    }
}

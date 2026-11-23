//______________________________________________________________________________
//
//                  Project:    Software EKG
//______________________________________________________________________________
package io.softwareecg.awb.repository.adapter.solr;

import io.avaje.inject.External;
import io.softwareecg.awb.repository.api.EkgRepository;
import io.softwareecg.awb.timeseries.port.TimeSeriesRepository;
import io.softwareecg.awb.timeseries.spi.TimeSeriesRepositoryProvider;
import io.softwareecg.awb.repository.model.EkgRepositoryDbType;
import io.softwareecg.awb.timeseries.repositorytype.Solr;
import io.softwareecg.awb.secrets.model.ResourceAuthType;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

/**
 * The cloud solr repository connects to a SolrCloud via Zookeeper.
 *
 * @since 21.09.2015
 */
public class SolrCloudRepository extends SolrClassicRepository {

    public SolrCloudRepository(TimeSeriesRepositoryProvider repositoryClientFactory) {
        super(repositoryClientFactory);
    }

    public SolrCloudRepository(String id,
                               String repositoryName,
                               String connectionUrl,
                               String dbIndexName,
                               EkgRepositoryDbType ekgRepositoryDbType,
                               ResourceAuthType resourceAuthType,
                               String username,
                               String password,
                               TimeSeriesRepositoryProvider repositoryClientFactory) {
        super(id, repositoryName, connectionUrl, dbIndexName, ekgRepositoryDbType,
                resourceAuthType, username, password, repositoryClientFactory);
    }

    @Override
    public TimeSeriesRepository getRepositoryClient() {

        if (cachedClient == null) {
            cachedClient = repositoryClientFactory.createRepositoryClient(
                    getConnectionUrl(), getDBIndexName(), getUsername(), getPassword());
        }

        return cachedClient;
    }

    /**
     * Factory to create a SolrCloud repository.
     */
    @Singleton
    public static class Factory extends SolrClassicRepository.Factory {

        @Inject
        public Factory(@External @Solr TimeSeriesRepositoryProvider clientFactory) {
            super(clientFactory);
        }

        /** No-arg constructor for tests that mock the factory client. */
        protected Factory() {
            super();
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
                throw new IllegalArgumentException("Can not create solr cloud types of type " + ekgRepositoryDbType);
            }

            return new SolrCloudRepository(id, name, connectionUrl, dbIndexName,
                    ekgRepositoryDbType, resourceAuthType, username, password,
                    clientFactory);
        }

        @Override
        public boolean isTypeSupported(EkgRepositoryDbType type) {
            return type == EkgRepositoryDbType.SOLR_CLOUD;
        }
    }
}

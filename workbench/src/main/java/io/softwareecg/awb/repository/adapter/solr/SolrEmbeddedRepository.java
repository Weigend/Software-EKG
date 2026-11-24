//______________________________________________________________________________
//
//                  Project:    Software EKG
//______________________________________________________________________________
package io.softwareecg.awb.repository.adapter.solr;

import io.avaje.inject.External;
import io.softwareecg.awb.repository.connection.EkgRepository;
import io.softwareecg.awb.repository.persistence.TimeSeriesRepository;
import io.softwareecg.awb.repository.connection.TimeSeriesRepositoryProvider;
import io.softwareecg.awb.repository.connection.EkgRepositoryDbType;
import io.softwareecg.awb.repository.api.Embedded;
import io.softwareecg.awb.repository.api.RepositoryException;
import io.softwareecg.awb.secrets.model.ResourceAuthType;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

/**
 * Implements the local embedded solr types, which runs within the same jvm as the current instance.
 *
 * @since 03.06.2015
 */
@Embedded
@Singleton
public class SolrEmbeddedRepository extends SolrClassicRepository {

    /**
     * Constructor-injected: receive the embedded-Solr {@link TimeSeriesRepositoryProvider}
     * directly, so the repository is fully wired without field-level injection.
     */
    @Inject
    public SolrEmbeddedRepository(@External @Embedded TimeSeriesRepositoryProvider repositoryClientFactory) {
        super("local", "Local Repository", null, null, EkgRepositoryDbType.SOLR_EMBEDDED,
                ResourceAuthType.NONE, null, null, repositoryClientFactory);
    }

    @Override
    public boolean isEmbedded() {
        return true;
    }

    @Override
    public void close() {
        try {
            if (cachedClient != null) {
                cachedClient.close();
            }
        } catch (RepositoryException e) {
            // do nothing
        }
    }

    @Override
    public TimeSeriesRepository getRepositoryClient() {

        if (cachedClient == null) {
            cachedClient = repositoryClientFactory.createRepositoryClient(null, null, null);
        }

        return cachedClient;
    }


    /**
     * Factory for the embedded solr repository — returns the same singleton instance
     * each time getInstance is called (the embedded repo is application-wide).
     */
    @Singleton
    @Embedded
    public static class Factory implements EkgRepository.Factory {

        private final SolrClassicRepository repository;

        @Inject
        public Factory(@Embedded SolrClassicRepository repository) {
            this.repository = repository;
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
                throw new IllegalArgumentException("Can not create embedded solr types of type "
                        + ekgRepositoryDbType);
            }

            return repository;
        }


        @Override
        public boolean isTypeSupported(EkgRepositoryDbType type) {
            return type == EkgRepositoryDbType.SOLR_EMBEDDED;
        }
    }
}

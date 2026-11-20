package io.softwareecg.awb.da.solr.embedded;

import io.softwareecg.awb.da.solr.SolrRepositoryClientProvider;
import io.softwareecg.awb.da.api.RepositoryClient;
import io.softwareecg.awb.da.api.RepositoryClientFactory;
import io.softwareecg.awb.da.api.types.Embedded;
import jakarta.inject.Inject;
import jakarta.inject.Provider;
import jakarta.inject.Singleton;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.embedded.EmbeddedSolrServer;
import org.apache.solr.core.CoreContainer;


/**
 * Factory class that provides a RepositoryClient that provides
 * the read/write access to an embedded Solr instance.
 *
 * <p>The {@link CoreContainer} is taken via {@link Provider} so the
 * (relatively expensive) {@code CoreContainer.createAndLoad(...)} call is
 * deferred until the first {@link #createRepositoryClient(String, String, String)}
 * invocation. Combined with the {@code @Lazy} on
 * {@link CoreContainerProducer#createCoreContainer()}, this keeps the embedded
 * Solr off the JavaFX {@code init()} startup path so the cost shows up while
 * the WFX preloader is visible (see {@code RepositoryManagerModule.preload}).
 *
 * @since 18.08.2018
 */
@Embedded
@Singleton
public class EmbeddedRepositoryClientProvider implements RepositoryClientFactory {

    private static final String CORE_NAME = "ekgdata";

    private final Provider<CoreContainer> coreContainerProvider;

    private SolrClient solrEmbeddedClient;

    private RepositoryClient cachedClient;

    @Inject
    public EmbeddedRepositoryClientProvider(Provider<CoreContainer> coreContainerProvider) {
        this.coreContainerProvider = coreContainerProvider;
    }

    @Override
    public synchronized RepositoryClient createRepositoryClient(String connectionUrl, String userName, String password) {

        if (cachedClient == null) {
            if (solrEmbeddedClient == null) {
                solrEmbeddedClient = new EmbeddedSolrServer(coreContainerProvider.get(), CORE_NAME);
            }
            SolrRepositoryClientProvider clientProvider = new SolrRepositoryClientProvider(solrEmbeddedClient);
            cachedClient = clientProvider.getClient(CORE_NAME);
        }

        return cachedClient;
    }

    @Override
    public RepositoryClient createRepositoryClient(String clusterManagerUrl, String repositoryName,
                                                   String userName, String password) {

        return createRepositoryClient(null, null, null);
    }
}

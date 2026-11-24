package io.softwareecg.awb.repository.adapter.solr.embedded;

import io.softwareecg.awb.repository.adapter.solr.SolrRepositoryClientProvider;
import io.softwareecg.awb.repository.persistence.TimeSeriesRepository;
import io.softwareecg.awb.repository.connection.TimeSeriesRepositoryProvider;
import io.softwareecg.awb.repository.connection.RepositoryTypeDescriptor;
import io.softwareecg.awb.repository.api.Embedded;
import jakarta.inject.Inject;
import jakarta.inject.Provider;
import jakarta.inject.Singleton;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.embedded.EmbeddedSolrServer;
import org.apache.solr.core.CoreContainer;
import java.util.List;


/**
 * Factory class that provides a TimeSeriesRepository that provides
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
public class EmbeddedRepositoryClientProvider implements TimeSeriesRepositoryProvider {

    @Override
    public List<RepositoryTypeDescriptor> repositoryTypes() {
        return List.of(new RepositoryTypeDescriptor("SOLR_EMBEDDED", "Solr Embedded",
                "icons/repository-db-type-solr.svg", false, "Repository URL:", "", "Index Name:", "", true,
                value -> true));
    }

    private static final String CORE_NAME = "ekgdata";

    private final Provider<CoreContainer> coreContainerProvider;

    private SolrClient solrEmbeddedClient;

    private TimeSeriesRepository cachedClient;

    @Inject
    public EmbeddedRepositoryClientProvider(Provider<CoreContainer> coreContainerProvider) {
        this.coreContainerProvider = coreContainerProvider;
    }

    @Override
    public synchronized TimeSeriesRepository createRepositoryClient(String connectionUrl, String userName, String password) {

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
    public TimeSeriesRepository createRepositoryClient(String clusterManagerUrl, String repositoryName,
                                                   String userName, String password) {

        return createRepositoryClient(null, null, null);
    }
}

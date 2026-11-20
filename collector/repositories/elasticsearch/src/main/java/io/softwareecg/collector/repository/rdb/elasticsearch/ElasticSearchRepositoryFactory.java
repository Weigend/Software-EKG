package io.softwareecg.collector.repository.rdb.elasticsearch;

import io.softwareecg.collector.common.LoggerFactory;
import io.softwareecg.collector.config.CounterDescriptor;
import io.softwareecg.collector.repository.RepositoryFactory;
import io.softwareecg.collector.repository.StaticTimeSeriesRepository;
import io.softwareecg.collector.repository.rdb.api.RdbProperties;
import io.softwareecg.collector.repository.rdb.api.RdbRepositoryClient;
import io.softwareecg.collector.repository.rdb.api.datamanagement.RemoteRepository;
import io.softwareecg.collector.repository.rdb.api.datamanagement.TimeSeriesManager;
import io.softwareecg.collector.repository.rdb.elasticsearch.client.ElasticSearchRepositoryClientProvider;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;

/**
 * An concrete implementation of the {@link RepositoryFactory} interface that will
 * create, setup and return an repository that is based on a remote instance of
 * ElasticSearch with support for version 6.x.
 * The provided repository supports the interfaces StaticTimeSeriesRepository & FlexTimeSeriesRepository.
 *
 * @since 02.11.2018
 */
public class ElasticSearchRepositoryFactory implements RepositoryFactory {

    private final Logger LOGGER = LoggerFactory.create();

    private RdbRepositoryClient rdbClient;

    @Override
    public StaticTimeSeriesRepository createStaticRepository(CounterDescriptor descriptor) {

        if (rdbClient == null ){
            rdbClient = getConfiguredRepositoryClient();
        }

        TimeSeriesManager.getInstance().workWithClient(rdbClient);

        RemoteRepository repository = new RemoteRepository(
                RdbProperties.getRepositoryName(),
                rdbClient,
                RdbProperties.getMaxIntervalInMsWithoutCommit(),
                RdbProperties.getCommitMaxUpdateEvents()
        );

        // create an ElasticSearch index in the remote database if enabled in the config and doesn't already exists.
        if (RdbProperties.autoCreateNoneExistingRepository()) {
            repository.ensureRepositoryExists(getClientProvider().getAdminClient());
        }

        return repository;
    }

    //=================================================================================================================
    //  internal helper do instantiate and configure the Solr client proxies
    //=================================================================================================================

    private RdbRepositoryClient getConfiguredRepositoryClient() {
        String repositoryName = RdbProperties.getRepositoryName();

        if (StringUtils.isBlank(repositoryName)) {
            LOGGER.warn("Unable to create Solr client as result of missing configuration value 'repositoryName'.");
        }

        return getClientProvider().getClient(repositoryName);
    }

    private ElasticSearchRepositoryClientProvider getClientProvider() {

        return new ElasticSearchRepositoryClientProvider(
                StringUtils.stripEnd(RdbProperties.getDatabaseUrl(), "/"),
                RdbProperties.getUser(),
                RdbProperties.getUserPassword()
        );
    }
}

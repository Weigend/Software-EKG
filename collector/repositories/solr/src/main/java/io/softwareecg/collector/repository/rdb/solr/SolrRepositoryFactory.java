/*
 * ______________________________________________________________________________
 *
 * Project: Software EKG
 * ______________________________________________________________________________
 *
 * created by: f.lautenschlager
 * creation date: 11.02.14 10:13
 * description: The solr storage factory
 * ______________________________________________________________________________
 *
 * Copyright: (c) Weigend AM, all rights reserved
 * ______________________________________________________________________________
 */
package io.softwareecg.collector.repository.rdb.solr;

import io.softwareecg.collector.common.LoggerFactory;
import io.softwareecg.collector.config.CounterDescriptor;
import io.softwareecg.collector.repository.RepositoryFactory;
import io.softwareecg.collector.repository.StaticTimeSeriesRepository;
import io.softwareecg.collector.repository.rdb.api.RdbProperties;
import io.softwareecg.collector.repository.rdb.api.RdbRepositoryClient;
import io.softwareecg.collector.repository.rdb.api.datamanagement.RemoteRepository;
import io.softwareecg.collector.repository.rdb.api.datamanagement.TimeSeriesManager;
import io.softwareecg.collector.repository.rdb.solr.client.SolrConnectionInfo;
import io.softwareecg.collector.repository.rdb.solr.client.SolrRepositoryClientProvider;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;

import java.util.Arrays;
import java.util.Collection;

/**
 * An concrete implementation of the {@link RepositoryFactory} interface that will
 * create, setup and return an repository that is based on a remote instance of
 * Apache Solr. The provided repository will support both: Solr Cloud/Solr Classic
 * and StaticTimeSeriesRepository & FlexTimeSeriesRepository.
 *
 * @since 31.10.2018
 */
public class SolrRepositoryFactory implements RepositoryFactory {

    private final Logger LOGGER = LoggerFactory.create();

    @Override
    public StaticTimeSeriesRepository createStaticRepository(CounterDescriptor descriptor) {

        RdbRepositoryClient rdbClient = getConfiguredRepositoryClient();
        TimeSeriesManager.getInstance().workWithClient(rdbClient);

        RemoteRepository repository = new RemoteRepository(
                RdbProperties.getRepositoryName(),
                rdbClient,
                RdbProperties.getMaxIntervalInMsWithoutCommit(),
                RdbProperties.getCommitMaxUpdateEvents()
        );

        // create an Solr collection/core in the remote database if enabled in the config and doesn't already exists.
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


    private SolrRepositoryClientProvider getClientProvider() {

        if (RdbProperties.isClusterMode()) {
            SolrConnectionInfo connectionInfo = new SolrConnectionInfo() {
                @Override
                public Collection<String> getZkHosts() {
                    return Arrays.asList(RdbProperties.getClusterManagerConnectionString().split(","));
                }

                @Override
                public String getZkHostsString() {
                    return RdbProperties.getClusterManagerConnectionString();
                }

                @Override
                public int getZkClientTimeout() {
                    return 15_000;
                }

                @Override
                public int getZkConnectTimeout() {
                    return 10_000;
                }

                @Override
                public String getUsername() {
                    return RdbProperties.getUser();
                }

                @Override
                public String getPassword() {
                    return RdbProperties.getUserPassword();
                }
            };

            return new SolrRepositoryClientProvider(connectionInfo);

        } else {
            return new SolrRepositoryClientProvider(
                    StringUtils.stripEnd(RdbProperties.getDatabaseUrl(), "/"),
                    RdbProperties.getUser(),
                    RdbProperties.getUserPassword()
            );
        }
    }
}

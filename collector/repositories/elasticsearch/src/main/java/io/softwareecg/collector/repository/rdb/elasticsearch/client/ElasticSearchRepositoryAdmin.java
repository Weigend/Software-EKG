package io.softwareecg.collector.repository.rdb.elasticsearch.client;

import io.softwareecg.collector.common.LoggerFactory;
import io.softwareecg.collector.repository.rdb.api.RdbRepositoryAdmin;
import io.softwareecg.collector.repository.rdb.api.RdbRepositoryException;
import io.softwareecg.collector.repository.rdb.api.querymodel.admin.CollectionProperties;
import io.softwareecg.collector.repository.rdb.elasticsearch.client.util.SchemaBuilder;
import org.elasticsearch.action.admin.indices.create.CreateIndexRequest;
import org.elasticsearch.action.admin.indices.create.CreateIndexResponse;
import org.elasticsearch.action.admin.indices.get.GetIndexRequest;
import org.elasticsearch.action.admin.indices.get.GetIndexResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.cluster.metadata.AliasMetaData;
import org.elasticsearch.common.settings.Settings;
import org.slf4j.Logger;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * This class is a concrete implementation of the {@link RdbRepositoryAdmin} interface
 * based on ElasticSearch.
 * It provides a proxy client with a simple small API to execute low level
 * administration requests.
 *
 * @since 03.11.2018
 */
public class ElasticSearchRepositoryAdmin implements RdbRepositoryAdmin {

    private static final Logger LOGGER = LoggerFactory.create();

    private final RestHighLevelClient elasticClient;

    /**
     * Constructs a new instance of ElasticSearchRepositoryAdmin that can use
     * to list and create ElasticSearch indices.
     *
     * @param elasticClient the proxy client that provides the API of the ElasticSearch database
     */
    public ElasticSearchRepositoryAdmin(RestHighLevelClient elasticClient) {
        this.elasticClient = elasticClient;
    }

    //================================================================================================================
    // implementation of the RdbRepositoryAdmin interface
    //================================================================================================================
    @Override
    public boolean createEkgRepository(String indexName) throws RdbRepositoryException {
        try {
            CreateIndexRequest request = new CreateIndexRequest(indexName);
            setupIndexProperties(request);
            defineSchema(request);

            LOGGER.info("Create new ElasticSearch index '{}' with EKG schema.", indexName);

            CreateIndexResponse createIndexResponse = elasticClient.indices().create(request, RequestOptions.DEFAULT);

            LOGGER.info("ElasticSearch index creation acknowlege state: {}", createIndexResponse.isAcknowledged());

            return createIndexResponse.isAcknowledged() && createIndexResponse.isShardsAcknowledged();

        } catch (IOException e) {
            throw new RdbRepositoryException(e);
        }
    }

    @Override
    public Collection<CollectionProperties> listRepositories() throws RdbRepositoryException {

        try {
            GetIndexRequest request = new GetIndexRequest();
            request.indices("*");

            GetIndexResponse response = elasticClient.indices().get(request, RequestOptions.DEFAULT);

            List<CollectionProperties> resultList = new ArrayList<>();

            for (String indexName : response.indices()) {

                int amountOfReplicas = response.getSettings().get(indexName).getAsInt("index.number_of_replicas", 1);
                int amountOfShards = response.getSettings().get(indexName).getAsInt("index.number_of_shards", 1);

                CollectionProperties properties = new CollectionProperties();
                properties.setCollectionName(indexName);
                properties.setAmountOfReplicas(amountOfReplicas);
                properties.setAmountOfShards(amountOfShards);

                List<AliasMetaData> aliases = response.getAliases().get(indexName);
                if (aliases.isEmpty()) {
                    properties.setCollectionAlias(indexName);
                } else {
                    properties.setCollectionAlias(aliases.get(0).alias());
                }
            }

            return resultList;

        } catch (IOException e) {
            LOGGER.info("Error occurred trying to retrieve the indices from ElasticSearch.", e);
            throw new RdbRepositoryException(e);
        }
    }


    //=================================================================================================================
    // internal logic
    //=================================================================================================================

    private static void setupIndexProperties(CreateIndexRequest request) {
        request.settings(Settings.builder()
                .put("index.number_of_shards", 1)
                .put("index.number_of_replicas", 1)
        );
    }

    private static void defineSchema(CreateIndexRequest request) throws IOException {
        request.mapping(SchemaBuilder.DOC_TYPE_KEY, SchemaBuilder.buildSchema());
    }
}

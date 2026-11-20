package io.softwareecg.collector.repository.rdb.elasticsearch.client;

import io.softwareecg.collector.repository.rdb.api.RdbRepositoryClient;
import io.softwareecg.collector.repository.rdb.api.RdbRepositoryException;
import io.softwareecg.collector.repository.rdb.api.TimeSeries;
import io.softwareecg.collector.repository.rdb.api.querymodel.delete.DeleteParams;
import io.softwareecg.collector.repository.rdb.api.querymodel.search.SearchParams;
import io.softwareecg.collector.repository.rdb.api.querymodel.search.SearchResult;
import io.softwareecg.collector.repository.rdb.api.querymodel.status.QueryStatus;
import io.softwareecg.collector.repository.rdb.elasticsearch.client.utils.ObjectBinder;
import io.softwareecg.collector.repository.rdb.elasticsearch.client.utils.SchemaBuilder;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.elasticsearch.action.ActionRequest;
import org.elasticsearch.action.admin.indices.flush.FlushRequest;
import org.elasticsearch.action.admin.indices.flush.FlushResponse;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchScrollRequest;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.action.update.UpdateResponse;
import org.elasticsearch.client.Request;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.Strings;
import org.elasticsearch.common.xcontent.ToXContent;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.elasticsearch.rest.RestStatus;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * An implementation of the {@link RdbRepositoryClient} interface that
 * will proxy an ElasticSearch based remote repository.
 * This implementation currently support ElasticSearch at version 6.x
 *
 * @since 22.09.2018
 */
public class ElasticSearchRepositoryClient implements RdbRepositoryClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(ElasticSearchRepositoryClient.class);

    private static final int BATCH_UPDATE_BULK_SIZE = 10_000;

    private static final String DELETE_BY_QUERY_HANDLER = "/_delete_by_query";

    private static final String IO_ERROR_MESSAGE = "I/O error accessing ElasticSearch server.";

    private final String elasticSearchIndex;

    private final ElasticSearchQueryFactory queryFactory;

    private final RestHighLevelClient elasticSearchClient;

    public ElasticSearchRepositoryClient(RestHighLevelClient elasticClient, String dbIndex) {
        this.elasticSearchIndex = dbIndex;
        this.elasticSearchClient = elasticClient;
        this.queryFactory = new ElasticSearchQueryFactory();
    }

    @Override
    public <T> SearchResult<T> search(Class<T> type, SearchParams searchParams) throws RdbRepositoryException {

        try {
            validateSearchParams(type, searchParams);

            ActionRequest searchQuery = queryFactory.createScrollableSearchQuery(searchParams, elasticSearchIndex);

            SearchResponse response;
            if (searchQuery instanceof SearchRequest) {
                response = elasticSearchClient.search((SearchRequest) searchQuery, RequestOptions.DEFAULT);
            } else {
                response = elasticSearchClient.scroll((SearchScrollRequest) searchQuery, RequestOptions.DEFAULT);
            }

            if (!checkResponse(response.status(), searchParams.getFilterQueries(), "Search ")) {
                throw new RdbRepositoryException("the facet search could proceed correctly. Retrieve unexpected " +
                        "response status: " + response.status().name());
            }

            List<T> documents = ObjectBinder.mapToBeans(type, response.getHits());
            return new SearchResult<>(documents, createSearchQueryStatus(response));

        } catch (Exception e) {
            throw new RdbRepositoryException(IO_ERROR_MESSAGE, e);
        }
    }

    @Override
    public void add(Collection<TimeSeries> timeSeries) throws RdbRepositoryException {
        try {

            BulkRequest bulkRequest = new BulkRequest();

            int count = 0;

            for (TimeSeries series : timeSeries) {

                // every time the loop exceed the bulk size all changes will send to ElasticSearch
                // and a new chunk will started.
                if (count > BATCH_UPDATE_BULK_SIZE) {
                    BulkResponse updateResponse = elasticSearchClient.bulk(bulkRequest, RequestOptions.DEFAULT);
                    if (!checkResponse(updateResponse.status(), "", "Add a batch of entities ") &&
                            checkFailures(updateResponse.hasFailures(), updateResponse.buildFailureMessage())) {
                        break;
                    }

                    bulkRequest = new BulkRequest();
                }

                // add the single document to the bulk request
                IndexRequest indexRequest = new IndexRequest(elasticSearchIndex,
                                                        SchemaBuilder.DOC_TYPE_KEY, UUID.randomUUID().toString());

                indexRequest.source(ObjectBinder.mapToContentMap(series));

                bulkRequest.add(indexRequest);

                count++;
            }

            // send the remaining document of the unfinished chunk if necessary
            if (bulkRequest.numberOfActions() > 0) {
                BulkResponse updateResponse = elasticSearchClient.bulk(bulkRequest, RequestOptions.DEFAULT);
                checkResponse(updateResponse.status(), "", "Add a batch of entities ");
                checkFailures(updateResponse.hasFailures(), updateResponse.buildFailureMessage());
            }

        } catch (Exception e) {
            LOGGER.error("Exception at Solr persistence layer occurred: ", e);
            throw new RdbRepositoryException(IO_ERROR_MESSAGE, e);
        }
    }

    @Override
    public void add(TimeSeries entity) throws RdbRepositoryException {

        try {
            Map<String, Object> entityAsMap = ObjectBinder.mapToContentMap(entity);

            if (StringUtils.isNotBlank((String) entityAsMap.get("id"))) {
                UpdateRequest updateRequest = new UpdateRequest(elasticSearchIndex,
                        SchemaBuilder.DOC_TYPE_KEY, (String) entityAsMap.get("id"));

                updateRequest.doc(entityAsMap);

                // update data
                UpdateResponse updateResponse = elasticSearchClient.update(updateRequest, RequestOptions.DEFAULT);
                if (checkResponse(updateResponse.status(), entity, "Add new entity ")) {
                    commit();
                }

            } else {

                IndexRequest indexRequest = new IndexRequest(elasticSearchIndex,
                                SchemaBuilder.DOC_TYPE_KEY, UUID.randomUUID().toString());

                indexRequest.source(entityAsMap);

                // add data
                IndexResponse updateResponse = elasticSearchClient.index(indexRequest, RequestOptions.DEFAULT);
                if (checkResponse(updateResponse.status(), entity, "Add new entity ")) {
                    commit();
                }
            }

        } catch (Exception e) {
            throw new RdbRepositoryException(IO_ERROR_MESSAGE, e);
        }
    }


    @Override
    public void delete(DeleteParams deleteParams) throws RdbRepositoryException {

        try {
            Request request = new Request("POST", elasticSearchIndex + DELETE_BY_QUERY_HANDLER);
            request.setJsonEntity(convertSourceToJson(queryFactory.createDeleteQuery(deleteParams)));

            Response response = elasticSearchClient.getLowLevelClient().performRequest(request);

            if (response.getStatusLine().getStatusCode() >= 400) {
                LOGGER.error("Error occurred trying to delete records from ElasticSearch. Reason: {}",
                        response.getStatusLine().getReasonPhrase());
            }

            commit();

        } catch (Exception e) {
            throw new RdbRepositoryException(IO_ERROR_MESSAGE, e);
        }
    }

    @Override
    public void commit() throws RdbRepositoryException {
        try {
            // commit the change
            FlushResponse flushResponse = elasticSearchClient.indices().flush(new FlushRequest(elasticSearchIndex),
                    RequestOptions.DEFAULT);

            checkResponse(flushResponse.getStatus(), elasticSearchIndex, "Commit index ");
        } catch (Exception e) {
            throw new RdbRepositoryException(IO_ERROR_MESSAGE, e);
        }
    }

    @Override
    public void close() throws RdbRepositoryException {
        try {
            elasticSearchClient.close();
        } catch (IOException e) {
            throw new RdbRepositoryException(e);
        }
    }

    //=================================================================================================================
    // internal helpers
    //=================================================================================================================


    private boolean checkFailures(boolean hasFailures, String buildFailureMessage) {
        if (hasFailures) {
            LOGGER.error(buildFailureMessage);
        }

        return hasFailures;
    }


    private <T> void validateSearchParams(Class<T> type, SearchParams searchParams) {
        Validate.notNull(type);
        Validate.notNull(searchParams);
        Validate.notNull(searchParams.getFilterQueries());
    }


    /**
     * Creates a {@link QueryStatus} for the given Solr {@link SearchResponse}.
     *
     * @param response the ElasticSearch {@link SearchResponse}.
     * @return the {@link QueryStatus}
     */
    private QueryStatus createSearchQueryStatus(SearchResponse response) {
      return new QueryStatus(response.getHits().getTotalHits(),
              (int) response.getTook().getMillis(), response.getScrollId());
    }


    private boolean checkResponse(RestStatus status, Object reqContent, String action) {

        if (status != RestStatus.OK && status != RestStatus.CREATED) {
            LOGGER.error(action + reqContent + " at/to ElasticSearch repository results in an " +
                    "unexpected result " + status);

            return false;
        }

        return true;
    }

    public static String convertSourceToJson(SearchSourceBuilder source) throws IOException {
        XContentBuilder xContent = source.toXContent(XContentFactory.jsonBuilder(), ToXContent.EMPTY_PARAMS);
        return Strings.toString(xContent);
    }
}

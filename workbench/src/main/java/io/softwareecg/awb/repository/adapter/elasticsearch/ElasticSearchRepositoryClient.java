package io.softwareecg.awb.repository.adapter.elasticsearch;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.aggregations.Aggregate;
import co.elastic.clients.elasticsearch._types.aggregations.StringTermsBucket;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.MatchAllQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch._types.query_dsl.WildcardQuery;
import co.elastic.clients.elasticsearch.core.BulkRequest;
import co.elastic.clients.elasticsearch.core.BulkResponse;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.ScrollResponse;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.bulk.BulkOperation;
import co.elastic.clients.elasticsearch.core.bulk.BulkResponseItem;
import io.softwareecg.awb.repository.adapter.elasticsearch.util.ObjectBinder;
import io.softwareecg.awb.repository.port.TimeSeriesRepository;
import io.softwareecg.awb.repository.port.DeleteParams;
import io.softwareecg.awb.repository.port.search.expr.Expression;
import io.softwareecg.awb.repository.port.search.facet.Facet;
import io.softwareecg.awb.repository.port.search.facet.FacetEntry;
import io.softwareecg.awb.repository.port.search.facet.FacetParams;
import io.softwareecg.awb.repository.port.search.facet.FacetResult;
import io.softwareecg.awb.repository.port.search.ExportParams;
import io.softwareecg.awb.repository.port.search.SearchParams;
import io.softwareecg.awb.repository.port.search.SearchResult;
import io.softwareecg.awb.repository.port.search.SortField;
import io.softwareecg.awb.repository.port.search.QueryStatus;
import io.softwareecg.awb.repository.schema.EkgSchemaField;
import io.softwareecg.awb.repository.schema.Field;
import io.softwareecg.awb.repository.api.RepositoryException;
import io.softwareecg.awb.runtime.logging.EkgLogger;
import org.apache.commons.lang3.NotImplementedException;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import co.elastic.clients.transport.rest5_client.low_level.Rest5Client;
import org.slf4j.Logger;

import java.io.IOException;
import java.util.*;
import java.util.stream.Stream;

/**
 * An implementation of the {@link TimeSeriesRepository} that is
 * built on top of ElasticSearch 9.x and its Java client API.
 *
 * @since 22.09.2018
 */
@SuppressWarnings("rawtypes")
public class ElasticSearchRepositoryClient implements TimeSeriesRepository {

    private static final Logger LOGGER = EkgLogger.get();

    private static final int BATCH_UPDATE_BULK_SIZE = 10_000;

    private static final String IO_ERROR_MESSAGE = "I/O error accessing ElasticSearch server.";

    private final String elasticSearchIndex;

    private final ElasticSearchQueryFactory queryFactory;

    private final ElasticsearchClient elasticSearchClient;

    private final Rest5Client restClient;

    public ElasticSearchRepositoryClient(ElasticsearchClient elasticClient, Rest5Client restClient, String dbIndex) {
        this.elasticSearchIndex = dbIndex;
        this.elasticSearchClient = elasticClient;
        this.restClient = restClient;
        this.queryFactory = new ElasticSearchQueryFactory();
    }

    @Override
    public <T> SearchResult<T> search(Class<T> type, SearchParams searchParams) throws RepositoryException {

        try {
            validateSearchParams(type, searchParams);

            if (searchParams.hasCursor() && !"*".equals(searchParams.getCursorMark())) {
                // scroll continuation
                ScrollResponse<Map> scrollResponse = elasticSearchClient.scroll(s -> s
                        .scrollId(searchParams.getCursorMark())
                        .scroll(ElasticSearchQueryFactory.SCROLL_KEEP_ALIVE), Map.class);

                List<T> documents = ObjectBinder.mapToBeans(type, scrollResponse.hits().hits());
                long totalHits = scrollResponse.hits().total() != null ? scrollResponse.hits().total().value() : 0;
                QueryStatus status = new QueryStatus(totalHits, (int) scrollResponse.took(), scrollResponse.scrollId());
                return new SearchResult<>(documents, status);
            } else {
                // initial search
                SearchRequest searchRequest = queryFactory.createSearchRequest(searchParams, elasticSearchIndex);
                SearchResponse<Map> response = elasticSearchClient.search(searchRequest, Map.class);

                List<T> documents = ObjectBinder.mapToBeans(type, response.hits().hits());
                return new SearchResult<>(documents, createSearchQueryStatus(response));
            }

        } catch (Exception e) {
            throw new RepositoryException(IO_ERROR_MESSAGE, e);
        }
    }

    @Override
    public <T> Stream<T> export(Class<T> type, ExportParams exportParams) {
        throw new NotImplementedException("Not implemented yet");
    }

    @Override
    public <T> SearchResult<T> collapse(Class<T> type, SearchParams searchParams, Field groupingField,
                                        SortField groupSortField) {
        throw new NotImplementedException("Not implemented yet");
    }

    @Override
    public long sumFieldValue(EkgSchemaField field, List<Expression> filterQueries) {
        return -1;
    }

    @Override
    public FacetResult facet(FacetParams facetParams) throws RepositoryException {

        try {
            Validate.notNull(facetParams);
            Validate.notEmpty(facetParams.getFacetFields());
            Validate.notNull(facetParams.getFacetMissing());
            Validate.notNull(facetParams.getFilterQueries());

            SearchRequest facetQuery = queryFactory.createFacetQuery(facetParams, elasticSearchIndex);

            SearchResponse<Map> response = elasticSearchClient.search(facetQuery, Map.class);

            return new FacetResult(extractFacets(response), createSearchQueryStatus(response));

        } catch (Exception e) {
            LOGGER.info("ElasticSearch operation failed: {}", e.getMessage());
            LOGGER.debug("Detailed ElasticSearch failure:", e);
            throw new RepositoryException(IO_ERROR_MESSAGE, e);
        }
    }

    @Override
    public void add(Iterator<?> entities) throws RepositoryException {
        try {

            List<BulkOperation> operations = new ArrayList<>();
            int count = 0;

            while (entities.hasNext()) {

                Object entity = entities.next();

                // every time the loop exceeds the bulk size all changes will be sent to ElasticSearch
                // and a new chunk will be started.
                if (count > BATCH_UPDATE_BULK_SIZE) {
                    executeBulk(operations);
                    operations = new ArrayList<>();
                    count = 0;
                }

                Map<String, Object> entityAsMap = ObjectBinder.mapToContentMap(entity);
                String docId = UUID.randomUUID().toString();

                operations.add(BulkOperation.of(op -> op
                        .index(idx -> idx.index(elasticSearchIndex).id(docId).document(entityAsMap))
                ));

                count++;
            }

            // send the remaining documents of the unfinished chunk if necessary
            if (!operations.isEmpty()) {
                executeBulk(operations);
            }

        } catch (Exception e) {
            LOGGER.info("ElasticSearch operation failed: {}", e.getMessage());
            LOGGER.debug("Detailed ElasticSearch failure:", e);
            throw new RepositoryException(IO_ERROR_MESSAGE, e);
        }
    }


    @Override
    public void add(Object entity) throws RepositoryException {

        try {
            Map<String, Object> entityAsMap = ObjectBinder.mapToContentMap(entity);

            if (StringUtils.isNotBlank((String) entityAsMap.get("id"))) {
                String docId = (String) entityAsMap.get("id");

                // update data
                elasticSearchClient.update(u -> u
                        .index(elasticSearchIndex)
                        .id(docId)
                        .doc(entityAsMap), Map.class);

            } else {
                String docId = UUID.randomUUID().toString();

                // add data
                elasticSearchClient.index(i -> i
                        .index(elasticSearchIndex)
                        .id(docId)
                        .document(entityAsMap));
            }

            commit();

        } catch (Exception e) {
            throw new RepositoryException(IO_ERROR_MESSAGE, e);
        }
    }

    @Override
    public void deleteAll() throws RepositoryException {
        delete(null);
    }

    @Override
    public void delete(DeleteParams deleteParams) throws RepositoryException {

        try {
            Query deleteQuery = buildDeleteQuery(deleteParams);

            elasticSearchClient.deleteByQuery(d -> d
                    .index(elasticSearchIndex)
                    .query(deleteQuery));

            commit();

        } catch (Exception e) {
            throw new RepositoryException(IO_ERROR_MESSAGE, e);
        }
    }

    @Override
    public void commit() throws RepositoryException {
        try {
            elasticSearchClient.indices().flush(f -> f.index(elasticSearchIndex));
        } catch (Exception e) {
            throw new RepositoryException(IO_ERROR_MESSAGE, e);
        }
    }

    @Override
    public boolean ping() {
        try {
            return elasticSearchClient.ping().value();
        } catch (Exception e) {
            LOGGER.debug("Ping failed for ElasticSearch repository: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public void close() throws RepositoryException {
        try {
            restClient.close();
        } catch (IOException e) {
            throw new RepositoryException(e);
        }
    }

    //=================================================================================================================
    // internal helpers
    //=================================================================================================================

    private void executeBulk(List<BulkOperation> operations) throws IOException {
        BulkResponse bulkResponse = elasticSearchClient.bulk(BulkRequest.of(b -> b.operations(operations)));

        if (bulkResponse.errors()) {
            StringBuilder failureMessage = new StringBuilder("Bulk operation had failures: ");
            for (BulkResponseItem item : bulkResponse.items()) {
                if (item.error() != null) {
                    failureMessage.append("[").append(item.error().reason()).append("] ");
                }
            }
            LOGGER.error(failureMessage.toString());
        }
    }

    private <T> void validateSearchParams(Class<T> type, SearchParams searchParams) {
        Validate.notNull(type);
        Validate.notNull(searchParams);
        Validate.notNull(searchParams.getFilterQueries());
    }

    /**
     * Creates a {@link QueryStatus} for the given {@link SearchResponse}.
     *
     * @param response the ElasticSearch {@link SearchResponse}.
     * @return the {@link QueryStatus}
     */
    private QueryStatus createSearchQueryStatus(SearchResponse<Map> response) {
        long totalHits = response.hits().total() != null ? response.hits().total().value() : 0;
        return new QueryStatus(totalHits, (int) response.took(), response.scrollId());
    }

    /**
     * Extracts the {@link Facet}s from the given {@link SearchResponse} converting ElasticSearch's aggregations
     * to our {@link Facet}s.
     *
     * @param response the {@link SearchResponse} of the facet query
     * @return the {@link Facet}s
     */
    private List<Facet> extractFacets(SearchResponse<Map> response) {

        Map<String, Aggregate> aggregations = response.aggregations();

        List<Facet> facetList = new ArrayList<>();
        for (Map.Entry<String, Aggregate> entry : aggregations.entrySet()) {

            List<FacetEntry> facetEntries = new ArrayList<>();
            for (StringTermsBucket bucket : entry.getValue().sterms().buckets().array()) {
                facetEntries.add(new FacetEntry(bucket.key().stringValue(), bucket.docCount()));
            }

            facetList.add(new Facet(entry.getKey(), facetEntries));
        }

        return facetList;
    }

    /**
     * Builds the delete query for the given delete parameters.
     * If deleteParams is null, a match-all query is returned.
     */
    private Query buildDeleteQuery(DeleteParams deleteParams) {
        if (deleteParams == null) {
            return MatchAllQuery.of(m -> m)._toQuery();
        }

        List<Query> filterClauses = deleteParams.getFilterExpMap().entrySet().stream()
                .map(entry -> WildcardQuery.of(w -> w
                        .field(entry.getKey().getName())
                        .value(entry.getValue()))._toQuery())
                .toList();

        return BoolQuery.of(b -> b
                .must(MatchAllQuery.of(m -> m)._toQuery())
                .filter(filterClauses))._toQuery();
    }
}

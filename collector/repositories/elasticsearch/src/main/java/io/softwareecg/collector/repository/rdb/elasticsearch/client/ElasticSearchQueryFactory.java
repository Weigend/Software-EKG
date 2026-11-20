package io.softwareecg.collector.repository.rdb.elasticsearch.client;

import io.softwareecg.collector.repository.rdb.api.querymodel.delete.DeleteParams;
import io.softwareecg.collector.repository.rdb.api.querymodel.expr.Expression;
import io.softwareecg.collector.repository.rdb.api.querymodel.search.SearchParams;
import io.softwareecg.collector.repository.rdb.api.querymodel.search.SortField;
import io.softwareecg.collector.repository.rdb.elasticsearch.client.expression.ExpressionRenderer;
import org.elasticsearch.action.ActionRequest;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchScrollRequest;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.sort.SortOrder;

import java.util.List;

/**
 * todo insert comment here
 *
 * @since 22.09.2018
 */
public class ElasticSearchQueryFactory {

    private final ExpressionRenderer renderer;

    /**
     * Default constructor that will create a new instance of this
     * factory with the default implementation of ExpressionRenderer
     * as renderer for the filter expressions.
     */
    public ElasticSearchQueryFactory() {
        this(new ExpressionRenderer());
    }

    /**
     * Parameterized constructor with will create a new instance of this
     * factory with the given implementation of ExpressionRenderer
     * as renderer for the filter
     *
     * @param renderer a ExpressionRenderer instance used to render the filter expressions
     */
    /* package-private */ ElasticSearchQueryFactory(ExpressionRenderer renderer) {
        this.renderer = renderer;
    }


    /**
     * Renders the given API filter expressions into filter queries that are provided
     * by an ElasticSearch QueryBuilder instance.
     * If the List is empty or null an MatchAll query will returned.
     *
     * @param expressions The filter expressions that should converted
     * @return The filter query as QueryBuilder.
     */
    private QueryBuilder toQueryFilters(List<Expression> expressions) {

        if (expressions == null) {
            return QueryBuilders.matchAllQuery();
        }

        BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();
        expressions.stream().map(renderer::render).forEach(boolQueryBuilder::filter);

        return boolQueryBuilder;
    }

    public SearchSourceBuilder createDeleteQuery(DeleteParams deleteParams) {

        if (deleteParams == null) {
            return new SearchSourceBuilder().query(QueryBuilders.matchAllQuery());
        }

        BoolQueryBuilder booleanQueryBuilder = QueryBuilders.boolQuery();
        booleanQueryBuilder.must(QueryBuilders.matchAllQuery());

        for (Expression filterExpressions : deleteParams.getFilterQueries()) {
            booleanQueryBuilder.filter(renderer.render(filterExpressions));
        }

        return new SearchSourceBuilder().query(booleanQueryBuilder);
    }


    public ActionRequest createScrollableSearchQuery(SearchParams searchParams, String elasticSearchIndex) {

        // if this is the n-th request during scrolling we can go the short way
        if (searchParams.hasCursor() && !"*".equals(searchParams.getCursorMark())) {
            SearchScrollRequest scrollRequest = new SearchScrollRequest(searchParams.getCursorMark());
            scrollRequest.scroll(TimeValue.timeValueMinutes(1L));
            return scrollRequest;
        }

        // the initial scroll request always needs the filter queries
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        searchSourceBuilder.query(toQueryFilters(searchParams.getFilterQueries()));
        searchSourceBuilder.size(searchParams.getMaxRows());

        for (SortField sortField : searchParams.getSortFields()) {
            searchSourceBuilder.sort(sortField.field().getName(),sortField.mode() == SortField.SortMode.ASC ?
                    SortOrder.ASC : SortOrder.DESC);
        }

        SearchRequest searchRequest = new SearchRequest(elasticSearchIndex);
        searchRequest.source(searchSourceBuilder);
        searchRequest.scroll(TimeValue.timeValueMinutes(1L));

        return searchRequest;
    }
}

package io.softwareecg.awb.repository.adapter.elasticsearch;

import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.Time;
import co.elastic.clients.elasticsearch._types.aggregations.Aggregation;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.MatchAllQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import io.softwareecg.awb.repository.adapter.elasticsearch.expression.ElasticSearchExpressionRenderer;
import io.softwareecg.awb.repository.persistence.search.expr.Expression;
import io.softwareecg.awb.repository.persistence.search.facet.FacetParams;
import io.softwareecg.awb.repository.persistence.search.SearchParams;
import io.softwareecg.awb.repository.persistence.search.SortField;
import io.softwareecg.awb.repository.persistence.schema.Field;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A factory for ElasticSearch search, facet, or update queries.
 *
 * @since 22.09.2018
 */
public class ElasticSearchQueryFactory {

    static final Time SCROLL_KEEP_ALIVE = Time.of(t -> t.time("1m"));

    private final ElasticSearchExpressionRenderer renderer;

    /**
     * Default constructor that will create a new instance of this
     * factory with the default implementation of ElasticSearchExpressionRenderer
     * as renderer for the filter expressions.
     */
    public ElasticSearchQueryFactory() {
        this(new ElasticSearchExpressionRenderer());
    }

    /**
     * Parameterized constructor with will create a new instance of this
     * factory with the given implementation of ElasticSearchExpressionRenderer
     * as renderer for the filter
     *
     * @param renderer a ElasticSearchExpressionRenderer instance used to render the filter expressions
     */
    /* package-private */ ElasticSearchQueryFactory(ElasticSearchExpressionRenderer renderer) {
        this.renderer = renderer;
    }


    public SearchRequest createFacetQuery(FacetParams facetParams, String elasticSearchIndex) {

        Query query = toQueryFilters(facetParams.getFilterQueries());

        Map<String, Aggregation> aggregations = new HashMap<>();
        for (Field field : facetParams.getFacetFields()) {
            aggregations.put(field.getName(),
                    Aggregation.of(a -> a.terms(t -> t.field(field.getName()))));
        }

        return SearchRequest.of(s -> s
                .index(elasticSearchIndex)
                .size(0)
                .query(query)
                .aggregations(aggregations)
        );
    }

    /**
     * Renders the given API filter expressions into filter queries.
     * If the List is empty or null a MatchAll query will be returned.
     *
     * @param expressions The filter expressions that should be converted
     * @return The filter query as Query.
     */
    Query toQueryFilters(List<Expression> expressions) {

        if (expressions == null || expressions.isEmpty()) {
            return MatchAllQuery.of(m -> m)._toQuery();
        }

        List<Query> filterClauses = expressions.stream().map(renderer::render).toList();
        return BoolQuery.of(b -> b.filter(filterClauses))._toQuery();
    }


    /**
     * Creates the initial search request for scroll-based searching.
     * For scroll continuation, use the scroll ID from the previous response directly.
     *
     * @param searchParams the search parameters
     * @param elasticSearchIndex the index to search
     * @return the initial SearchRequest with scroll enabled
     */
    public SearchRequest createSearchRequest(SearchParams searchParams, String elasticSearchIndex) {

        Query query = toQueryFilters(searchParams.getFilterQueries());

        return SearchRequest.of(s -> {
            s.index(elasticSearchIndex);
            s.query(query);
            s.size(searchParams.getMaxRows());
            s.scroll(SCROLL_KEEP_ALIVE);

            for (SortField sortField : searchParams.getSortFields()) {
                s.sort(sort -> sort.field(f -> f
                        .field(sortField.field().getName())
                        .order(sortField.mode() == SortField.SortMode.ASC ? SortOrder.Asc : SortOrder.Desc)
                ));
            }

            return s;
        });
    }
}

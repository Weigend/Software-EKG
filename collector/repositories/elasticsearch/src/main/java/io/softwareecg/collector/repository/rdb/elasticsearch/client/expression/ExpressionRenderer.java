package io.softwareecg.collector.repository.rdb.elasticsearch.client.expression;

import io.softwareecg.collector.repository.rdb.api.querymodel.expr.*;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.RangeQueryBuilder;

/**
 * A renderer/converter that will transform the EKG filter expression language
 * (object based API) to a similar representation ElasticSearch QueryBuilder
 * construct.
 *
 * @since 23.09.2018
 */
public class ExpressionRenderer {

    /**
     * Renders the given {@link Expression} as a(nested) QueryBuilder to be used in a filter query
     * segment of an ElasticSearch BoolQueryBuilder. At the end this will result an 0-n filter queries
     * (not term queries) send to ElasticSearch.
     *
     * @param expression the {@link Expression}
     * @return the filter query as String
     */
    public QueryBuilder render(Expression expression) {
        Validate.notNull(expression);

        if (expression instanceof AndExpression) {
            return renderAndExpression((AndExpression) expression);

        } else if (expression instanceof ExactFilterExpression) {
            return renderExactFilterExpression((ExactFilterExpression) expression);

        } else if (expression instanceof NotExpression) {
            return renderNotExpression((NotExpression) expression);

        } else if (expression instanceof OrExpression) {
            return renderOrExpression((OrExpression) expression);

        } else if (expression instanceof RangeFilterExpression) {
            return renderRangeFilterExpression((RangeFilterExpression) expression);

        } else {
            throw new IllegalArgumentException(ExpressionRenderer.class.getSimpleName() + " does not support "
                    + expression.getClass().getSimpleName());
        }
    }

    private QueryBuilder renderAndExpression(AndExpression expression) {
        Validate.noNullElements(expression.getExpressions());

        BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();
        expression.getExpressions().stream().map(this::render).forEach(boolQueryBuilder::must);
        return boolQueryBuilder;
    }

    private QueryBuilder renderExactFilterExpression(ExactFilterExpression expression) {
        Validate.notNull(expression.field());
        Validate.notNull(expression.values());

        return QueryBuilders.termsQuery(expression.field().getName(), expression.values());
    }

    private QueryBuilder renderNotExpression(NotExpression expression) {
        Validate.notNull(expression.getExpression());

        BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();
        boolQueryBuilder.must(QueryBuilders.matchAllQuery());
        boolQueryBuilder.mustNot(render(expression.getExpression()));

        return boolQueryBuilder;
    }

    private QueryBuilder renderOrExpression(OrExpression expression) {
        Validate.noNullElements(expression.getExpressions());

        BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();
        expression.getExpressions().stream().map(this::render).forEach(boolQueryBuilder::should);
        return boolQueryBuilder;
    }

    private QueryBuilder renderRangeFilterExpression(RangeFilterExpression expression) {
        Validate.notNull(expression.getField());

        RangeQueryBuilder rangeQueryBuilder = QueryBuilders.rangeQuery(expression.getField().getName());

        if (StringUtils.isNotBlank(expression.getLowerBound())) {
            rangeQueryBuilder.from(expression.getLowerBound());
        }

        if (StringUtils.isNotBlank(expression.getUpperBound())) {
            rangeQueryBuilder.from(expression.getUpperBound());
        }

        return rangeQueryBuilder;
    }
}

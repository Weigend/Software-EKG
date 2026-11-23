package io.softwareecg.awb.timeseries.adapter.elasticsearch.expression;

import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.MatchAllQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.MatchPhraseQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch._types.query_dsl.RangeQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.TermsQuery;
import co.elastic.clients.json.JsonData;
import co.elastic.clients.elasticsearch._types.query_dsl.TermsQueryField;
import co.elastic.clients.elasticsearch._types.query_dsl.WildcardQuery;
import org.apache.commons.lang3.NotImplementedException;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;

import java.util.List;

import io.softwareecg.awb.timeseries.port.query.expr.AndExpression;
import io.softwareecg.awb.timeseries.port.query.expr.ExactFilterExpression;
import io.softwareecg.awb.timeseries.port.query.expr.Expression;
import io.softwareecg.awb.timeseries.port.query.expr.FullTextFilterExpression;
import io.softwareecg.awb.timeseries.port.query.expr.NotExpression;
import io.softwareecg.awb.timeseries.port.query.expr.OrExpression;
import io.softwareecg.awb.timeseries.port.query.expr.RangeFilterExpression;
import io.softwareecg.awb.timeseries.port.query.expr.WildcardFilterExpression;
/**
 * A renderer/converter that will transform the EKG filter expression language
 * (object based API) to a similar representation ElasticSearch Query
 * construct.
 *
 * @since 23.09.2018
 */
public class ElasticSearchExpressionRenderer {

    /**
     * Renders the given {@link Expression} as a (nested) Query to be used in a filter query
     * segment of an ElasticSearch BoolQuery. At the end this will result in 0-n filter queries
     * (not term queries) sent to ElasticSearch.
     *
     * @param expression the {@link Expression}
     * @return the filter query as Query
     */
    public Query render(Expression expression) {
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

        } else if (expression instanceof WildcardFilterExpression) {
            return renderWildcardFilterExpression((WildcardFilterExpression) expression);

        } else if (expression instanceof FullTextFilterExpression) {
            return renderFullTextFilterExpression((FullTextFilterExpression) expression);

        } else {
            throw new IllegalArgumentException(ElasticSearchExpressionRenderer.class.getSimpleName() + " does not support "
                    + expression.getClass().getSimpleName());
        }
    }

    private Query renderAndExpression(AndExpression expression) {
        Validate.noNullElements(expression.getExpressions());

        List<Query> mustClauses = expression.getExpressions().stream().map(this::render).toList();
        return BoolQuery.of(b -> b.must(mustClauses))._toQuery();
    }

    private Query renderExactFilterExpression(ExactFilterExpression expression) {
        Validate.notNull(expression.field());
        Validate.notNull(expression.values());

        List<FieldValue> fieldValues = expression.values().stream()
                .map(FieldValue::of)
                .toList();

        return TermsQuery.of(t -> t
                .field(expression.field().getName())
                .terms(TermsQueryField.of(tf -> tf.value(fieldValues)))
        )._toQuery();
    }

    private Query renderNotExpression(NotExpression expression) {
        Validate.notNull(expression.getExpression());

        return BoolQuery.of(b -> b
                .must(MatchAllQuery.of(m -> m)._toQuery())
                .mustNot(render(expression.getExpression()))
        )._toQuery();
    }

    private Query renderOrExpression(OrExpression expression) {
        Validate.noNullElements(expression.getExpressions());

        List<Query> shouldClauses = expression.getExpressions().stream().map(this::render).toList();
        return BoolQuery.of(b -> b.should(shouldClauses))._toQuery();
    }

    private Query renderRangeFilterExpression(RangeFilterExpression expression) {
        Validate.notNull(expression.field());

        return RangeQuery.of(r -> r.untyped(u -> {
            u.field(expression.field().getName());

            if (StringUtils.isNotBlank(expression.lowerBound())) {
                u.gte(JsonData.of(expression.lowerBound()));
            }

            if (StringUtils.isNotBlank(expression.upperBound())) {
                u.lte(JsonData.of(expression.upperBound()));
            }

            return u;
        }))._toQuery();
    }

    private Query renderWildcardFilterExpression(WildcardFilterExpression expression) {
        Validate.notNull(expression.field());
        Validate.notNull(expression.values());

        String field = expression.field().getName();

        if (expression.values().size() > 1) {
            throw new NotImplementedException("The support for wildcard queries with multiple terms isn't implemented yet.");
        }

        if (expression.values().isEmpty()) {
            return MatchAllQuery.of(m -> m)._toQuery();
        }

        return WildcardQuery.of(w -> w.field(field).value(expression.values().get(0)))._toQuery();
    }

    private Query renderFullTextFilterExpression(FullTextFilterExpression expression) {
        Validate.notNull(expression.getField());
        Validate.notEmpty(expression.getValue());

        return MatchPhraseQuery.of(m -> m
                .field(expression.getField().getName())
                .query(expression.getValue())
        )._toQuery();
    }

}

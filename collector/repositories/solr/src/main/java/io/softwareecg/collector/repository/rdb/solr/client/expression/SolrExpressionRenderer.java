package io.softwareecg.collector.repository.rdb.solr.client.expression;

import io.softwareecg.collector.repository.rdb.api.querymodel.expr.*;
import org.apache.commons.lang3.Validate;

import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Renders {@link Expression}s into Solr filter query Strings.
 *
 * @since 06.02.2018
 */
public class SolrExpressionRenderer {

    /**
     * Renders the given {@link Expression} as a string to be used in a filter query (Solr fq-parameter).
     *
     * @param expression the {@link Expression}
     * @return the filter query as String
     */
    public String render(Expression expression) {
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
            throw new IllegalArgumentException(SolrExpressionRenderer.class.getSimpleName() + " does not support "
                    + expression.getClass().getSimpleName());
        }
    }

    private String renderAndExpression(AndExpression expression) {
        Validate.noNullElements(expression.getExpressions());

        return expression.getExpressions().stream()
                .map(this::render)
                .collect(Collectors.joining(" AND ", "(", ")"));
    }

    private String renderExactFilterExpression(ExactFilterExpression expression) {
        Validate.notNull(expression.field());
        Validate.notNull(expression.values());

        String field = expression.field().getName();
        return field + ":" + groupFieldValues(expression.values(),
                value -> SolrEscaping.escape(field, value, false, false));
    }

    private String renderNotExpression(NotExpression expression) {
        Validate.notNull(expression.getExpression());

        // In Solr, NOT(..) alone does not provide the boolean NOT semantics except on top level.
        // Use *:* AND NOT(..) to enforce the correct meaning (as Solr does automatically on top level).
        return "(*:* AND NOT " + render(expression.getExpression()) + ")";
    }

    private String renderOrExpression(OrExpression expression) {
        Validate.noNullElements(expression.getExpressions());

        return expression.getExpressions().stream()
                .map(this::render)
                .collect(Collectors.joining(" OR ", "(", ")"));
    }

    private String renderRangeFilterExpression(RangeFilterExpression expression) {
        Validate.notNull(expression.getField());

        String lowerBound = Objects.toString(expression.getLowerBound(), "*");
        String upperBound = Objects.toString(expression.getUpperBound(), "*");
        return expression.getField().getName() + ":[" + lowerBound + " TO " + upperBound + "]";
    }

    /**
     * Groups together the values of a field, escaping values with the given escape function.
     *
     * @param values the values
     * @param escapeFunction the function to escape each value
     * @return the grouped and escaped values, e.g. (A B C)
     */
    private String groupFieldValues(List<String> values, Function<String, String> escapeFunction) {
        return values.stream()
                .map(escapeFunction)
                .collect(Collectors.joining(" ", "(", ")"));
    }
}

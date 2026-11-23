package io.softwareecg.awb.timeseries.adapter.clickhouse.expression;

import org.apache.commons.lang3.Validate;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import io.softwareecg.awb.timeseries.port.query.expr.AndExpression;
import io.softwareecg.awb.timeseries.port.query.expr.ExactFilterExpression;
import io.softwareecg.awb.timeseries.port.query.expr.Expression;
import io.softwareecg.awb.timeseries.port.query.expr.FullTextFilterExpression;
import io.softwareecg.awb.timeseries.port.query.expr.NotExpression;
import io.softwareecg.awb.timeseries.port.query.expr.OrExpression;
import io.softwareecg.awb.timeseries.port.query.expr.RangeFilterExpression;
import io.softwareecg.awb.timeseries.port.query.expr.WildcardFilterExpression;
/**
 * Renders {@link Expression}s into ClickHouse SQL WHERE clause fragments.
 *
 */
public class ClickHouseExpressionRenderer {

    /**
     * Renders the given {@link Expression} as a SQL WHERE clause fragment.
     *
     * @param expression the {@link Expression}
     * @return the SQL fragment
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

        } else if (expression instanceof WildcardFilterExpression) {
            return renderWildcardFilterExpression((WildcardFilterExpression) expression);

        } else if (expression instanceof FullTextFilterExpression) {
            return renderFullTextFilterExpression((FullTextFilterExpression) expression);

        } else {
            throw new IllegalArgumentException(ClickHouseExpressionRenderer.class.getSimpleName()
                    + " does not support " + expression.getClass().getSimpleName());
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

        String fieldName = escapeIdentifier(expression.field().getName());
        List<String> values = expression.values();

        if (values.size() == 1) {
            return fieldName + " = " + escapeValue(values.get(0));
        }

        String inList = values.stream()
                .map(this::escapeValue)
                .collect(Collectors.joining(", "));
        return fieldName + " IN (" + inList + ")";
    }

    private String renderNotExpression(NotExpression expression) {
        Validate.notNull(expression.getExpression());
        return "NOT " + render(expression.getExpression());
    }

    private String renderOrExpression(OrExpression expression) {
        Validate.noNullElements(expression.getExpressions());

        return expression.getExpressions().stream()
                .map(this::render)
                .collect(Collectors.joining(" OR ", "(", ")"));
    }

    private String renderRangeFilterExpression(RangeFilterExpression expression) {
        Validate.notNull(expression.field());

        String fieldName = escapeIdentifier(expression.field().getName());
        String lowerBound = expression.lowerBound();
        String upperBound = expression.upperBound();

        if (lowerBound != null && upperBound != null) {
            return fieldName + " >= " + escapeValue(lowerBound)
                    + " AND " + fieldName + " <= " + escapeValue(upperBound);
        } else if (lowerBound != null) {
            return fieldName + " >= " + escapeValue(lowerBound);
        } else if (upperBound != null) {
            return fieldName + " <= " + escapeValue(upperBound);
        } else {
            return "1 = 1";
        }
    }

    private String renderWildcardFilterExpression(WildcardFilterExpression expression) {
        Validate.notNull(expression.field());
        Validate.notNull(expression.values());

        String fieldName = escapeIdentifier(expression.field().getName());
        List<String> values = expression.values();

        if (values.size() == 1) {
            return fieldName + " LIKE " + escapeWildcardValue(values.get(0));
        }

        return values.stream()
                .map(value -> fieldName + " LIKE " + escapeWildcardValue(value))
                .collect(Collectors.joining(" OR ", "(", ")"));
    }

    private String renderFullTextFilterExpression(FullTextFilterExpression expression) {
        Validate.notNull(expression.getField());
        Validate.notEmpty(expression.getValue());

        String fieldName = escapeIdentifier(expression.getField().getName());
        // ClickHouse doesn't have full-text search; approximate with LIKE
        String value = expression.getValue()
                .replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
        return fieldName + " LIKE '%" + value + "%'";
    }

    /**
     * Escapes a ClickHouse identifier (column name) by wrapping in backticks
     * if it contains special characters.
     */
    private String escapeIdentifier(String identifier) {
        if (identifier.matches("[a-zA-Z_][a-zA-Z0-9_]*")) {
            return identifier;
        }
        return "`" + identifier.replace("`", "``") + "`";
    }

    /**
     * Escapes a string value for use in ClickHouse SQL.
     * ISO-8601 date strings are wrapped with toDateTime64() after format conversion
     * so that ClickHouse DateTime64(3) columns can accept them.
     */
    private String escapeValue(String value) {
        if (value == null) {
            return "NULL";
        }
        if (isIsoDateTime(value)) {
            String chDate = value.replace("T", " ").replace("Z", "");
            return "toDateTime64('" + chDate + "', 3)";
        }
        return "'" + value.replace("\\", "\\\\").replace("'", "\\'") + "'";
    }

    private static boolean isIsoDateTime(String value) {
        return value.matches("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}(\\.\\d+)?(Z|[+-]\\d{2}:\\d{2})?");
    }

    /**
     * Converts wildcard pattern (* and ?) to ClickHouse LIKE syntax (% and _)
     * and escapes the value.
     */
    private String escapeWildcardValue(String value) {
        if (value == null) {
            return "NULL";
        }
        String escaped = value
                .replace("\\", "\\\\")
                .replace("'", "\\'")
                .replace("%", "\\%")
                .replace("_", "\\_")
                .replace("*", "%")
                .replace("?", "_");
        return "'" + escaped + "'";
    }
}

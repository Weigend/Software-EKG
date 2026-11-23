package io.softwareecg.awb.timeseries.port.query.expr;

import io.softwareecg.awb.timeseries.schema.Field;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * {@link Expression} to filter {@link Field}s for String values, interpreting wildcards.
 * See {@link ExprFactory#wildcardFilter(Field, String...)}.
 *
 * @since 14.02.2018
 */
public record WildcardFilterExpression(Field field, List<String> values) implements Expression {
    public WildcardFilterExpression(Field field, List<String> values) {
        this.field = field;
        this.values = new ArrayList<>(values);
    }

    /**
     * Returns the {@link Field} on which to filter.
     *
     * @return the {@link Field}
     */
    @Override
    public Field field() {
        return field;
    }

    /**
     * Returns the values to filter.
     *
     * @return the values
     */
    @Override
    public List<String> values() {
        return Collections.unmodifiableList(values);
    }

}

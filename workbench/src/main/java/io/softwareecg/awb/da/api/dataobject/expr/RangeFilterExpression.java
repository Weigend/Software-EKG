package io.softwareecg.awb.da.api.dataobject.expr;

import io.softwareecg.awb.da.api.schema.Field;

/**
 * {@link Expression} to filter {@link Field}s for ranges. See
 * {@link ExprFactory#numberRangeFilter(Field, Number, Number)}.
 *
 * @since 14.02.2018
 */
public record RangeFilterExpression(Field field, String lowerBound, String upperBound) implements Expression {

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
     * Returns the lower bound of the range.
     *
     * @return the lower bound
     */
    @Override
    public String lowerBound() {
        return lowerBound;
    }

    /**
     * Returns the upper bound of the range.
     *
     * @return the upper bound
     */
    @Override
    public String upperBound() {
        return upperBound;
    }

}

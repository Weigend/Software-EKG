package io.softwareecg.collector.repository.rdb.api.querymodel.expr;

import io.softwareecg.collector.repository.rdb.api.schema.Field;

import java.util.Objects;

/**
 * {@link Expression} to filter {@link Field}s for ranges.
 *
 * @since 14.02.2018
 */
public final class RangeFilterExpression implements Expression {
    private final Field field;
    private final String lowerBound;
    private final String upperBound;

    /* package-private */ RangeFilterExpression(Field field, String lowerBound, String upperBound) {
        this.field = field;
        this.lowerBound = lowerBound;
        this.upperBound = upperBound;
    }

    /**
     * Returns the {@link Field} on which to filter.
     *
     * @return the {@link Field}
     */
    public Field getField() {
        return field;
    }

    /**
     * Returns the lower bound of the range.
     *
     * @return the lower bound
     */
    public String getLowerBound() {
        return lowerBound;
    }

    /**
     * Returns the upper bound of the range.
     *
     * @return the upper bound
     */
    public String getUpperBound() {
        return upperBound;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RangeFilterExpression that = (RangeFilterExpression) o;
        return Objects.equals(field, that.field) &&
                Objects.equals(lowerBound, that.lowerBound) &&
                Objects.equals(upperBound, that.upperBound);
    }

    @Override
    public int hashCode() {
        return Objects.hash(field, lowerBound, upperBound);
    }
}

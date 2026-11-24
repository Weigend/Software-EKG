package io.softwareecg.awb.uitoolkit.chart.axis;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.BooleanPropertyBase;
import javafx.scene.chart.Axis;
import javafx.scene.chart.ValueAxis;

/**
 *
 * @since 29.08.2018
 *
 * @param <T> type of axis
 */
public abstract class BaseAxis<T extends Number> extends ValueAxis<T> implements SpawningAxis<T> {

    /** In pixels. */
    private static final double ROUNDING_ERROR = 1e-6;

    protected double overwriteUpperBound = Double.NaN;

    protected double overwriteLowerBound = Double.NaN;

    /**
     * Create a auto-ranging NumberAxis
     */
    public BaseAxis() {

    }

    /**
     * Create a non-auto-ranging NumberAxis with the given upper bound, lower bound and tick unit
     *
     * @param lowerBound The lower bound for this axis, ie min plottable value
     * @param upperBound The upper bound for this axis, ie max plottable value
     */
    public BaseAxis(double lowerBound, double upperBound) {
        super(lowerBound, upperBound);
    }

    /**
     * A value at the end of the axis is exactly there. Its position is computed, and a rounding error
     * beyond the end was enough to hide the tick and its label.
     * <p>
     * Here and not in {@link DateAxis}: JavaFX asks an axis of {@code Long} for the position of a
     * {@code Double}, too, which fails as soon as a method expects a {@code Long}.
     */
    @Override
    public double getDisplayPosition(T value) {
        double position = super.getDisplayPosition(value);
        double length = getSide() != null && getSide().isVertical() ? getHeight() : getWidth();
        return Math.abs(position - length) < ROUNDING_ERROR ? length : position;
    }

    //=================================================================================================================
    // parts of the SpawningAxis / Bondable interface implementation
    //=================================================================================================================

    @Override
    public void forceUpperBound(double upperBound) {
        overwriteUpperBound = upperBound;
    }

    @Override
    public void forceLowerBound(double lowerBound) {
        overwriteLowerBound = lowerBound;
    }

    @Override
    public double getEnforcedUpperBound() {
        return overwriteUpperBound;
    }

    @Override
    public double getEnforcedLowerBound() {
        return overwriteLowerBound;
    }


    @Override
    public void layoutCustomBounds() {
        layoutAxis(!Double.isNaN(overwriteLowerBound) || !Double.isNaN(overwriteUpperBound));
    }


    @Override
    public Axis<T> castToAxis() {
        return this;
    }

    //=================================================================================================================
    // other API methods
    //=================================================================================================================

    /**
     * Indicates whether zero in range is forced
     *
     * @return true/false
     */
    public final boolean isForceZeroInRange() {
        return forceZeroInRange.getValue();
    }

    /**
     * Sets the force zero in range flag
     * @param value new flag
     */
    public final void setForceZeroInRange(boolean value) {
        forceZeroInRange.setValue(value);
    }

    /**
     * Returns the force zero in range property
     * @return the force zero in range property
     */
    public final BooleanProperty forceZeroInRangeProperty() {
        return forceZeroInRange;
    }

    private void layoutAxis(boolean isOverwriteBoundDefined) {
        invalidateRange();
        setNeedsLayout(true);

        if (isOverwriteBoundDefined) {
            setAutoRanging(true);
        }

        layout();
    }



    /**
     * When true zero is always included in the visible range. This only has effect if auto-ranging is on.
     */
    private final BooleanProperty forceZeroInRange = new BooleanPropertyBase(true) {

        @Override
        public String getName() {
            return "forceZeroInRange";
        }

        @Override
        public Object getBean() {
            return BaseAxis.this;
        }

        @Override
        protected void invalidated() {
            // This will effect layout if we are auto ranging
            if (isAutoRanging()) {
                requestAxisLayout();
                invalidateRange();
            }
        }
    };
}

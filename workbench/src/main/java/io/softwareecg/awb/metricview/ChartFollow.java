package io.softwareecg.awb.metricview;

/**
 * Decides how an open chart follows values that are written while it is shown.
 * <ul>
 *   <li>A chart that shows the whole time range is loaded again and grows with the data.</li>
 *   <li>A zoomed chart whose right edge reached the newest values keeps its width and moves
 *       on with them.</li>
 *   <li>A chart zoomed into the past is left alone - nothing changes there.</li>
 * </ul>
 *
 * @param action what the chart does
 * @param shift  by how many milliseconds a zoomed window moves on; 0 for the other actions
 */
public record ChartFollow(Action action, long shift) {

    public enum Action {
        /** load the whole range again and let the axis fit the data */
        RELOAD_WHOLE_RANGE,
        /** move the zoomed window on by {@link ChartFollow#shift()} and load it */
        MOVE_WINDOW,
        /** leave the chart as it is */
        NONE
    }

    /**
     * @param wholeRangeShown  whether the chart shows the whole range (not zoomed) or is still empty
     * @param windowUpperBound the right edge of a zoomed chart, as time in milliseconds
     * @param previousNewest   the time of the newest value the chart has followed up to, 0 if none yet
     * @param newest           the time of the newest value written now
     */
    public static ChartFollow decide(boolean wholeRangeShown, double windowUpperBound, long previousNewest, long newest) {
        if (wholeRangeShown) {
            return new ChartFollow(Action.RELOAD_WHOLE_RANGE, 0);
        }
        long added = newest - previousNewest;
        if (previousNewest <= 0 || added <= 0 || windowUpperBound < previousNewest) {
            return new ChartFollow(Action.NONE, 0);
        }
        return new ChartFollow(Action.MOVE_WINDOW, added);
    }
}

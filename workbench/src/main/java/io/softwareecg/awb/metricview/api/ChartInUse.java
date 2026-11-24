package io.softwareecg.awb.metricview.api;

import java.util.Optional;

/**
 * The chart the user works with, of all that are open.
 */
public interface ChartInUse {

    /**
     * @return the chart that was used last; empty if there is none
     */
    Optional<ShownChart> shownChart();
}

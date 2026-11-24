package io.softwareecg.awb.metricview.api;

import io.softwareecg.awb.query.api.ComputationSpec;
import io.softwareecg.awb.query.api.MetricDataAccessService;
import io.softwareecg.awb.query.api.MetricQueryService;
import io.softwareecg.awb.query.api.QuerySpec;

import java.time.ZoneId;

/**
 * What a chart shows. It changes with the chart.
 */
public interface ShownChart {

    /**
     * @return the query of the data that is shown, with the interval it is zoomed to; what is
     *         chosen in the header if there is no data yet
     */
    QuerySpec query();

    /**
     * @return what is computed from the data, as it is chosen in the header
     */
    ComputationSpec computation();

    /**
     * @return the time zone the chart shows its times in, chosen at its lower left corner
     */
    ZoneId timeZone();

    /**
     * @return what the repository of the chart holds; {@code null} if there is none or it cannot be reached
     */
    MetricDataAccessService metrics();

    /**
     * @return the queries of the repository of the chart; {@code null} if there is none or it cannot be reached
     */
    MetricQueryService queries();
}

package io.softwareecg.awb.metricanalyzer.bl.visualisation.combine;

import io.softwareecg.awb.sdk.datamodel.TimeSeries;

import java.util.List;

/**
 * todo enter comment here
 *
 * @since 21.07.2018
 */
public interface TimeSeriesCombiner {

    TimeSeries combine(String newMetricName, List<TimeSeries> timeSeriesList);
}

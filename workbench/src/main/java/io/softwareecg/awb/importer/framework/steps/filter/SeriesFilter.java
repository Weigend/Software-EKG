package io.softwareecg.awb.importer.framework.steps.filter;

import io.softwareecg.awb.timeseries.RawSeriesData;

import java.util.function.Predicate;

/**
 * todo enter comment here
 *
 * @since 14.07.2018
 */
public interface SeriesFilter <T extends RawSeriesData> extends Predicate<T> {

}
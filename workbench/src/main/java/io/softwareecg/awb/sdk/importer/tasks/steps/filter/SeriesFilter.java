package io.softwareecg.awb.sdk.importer.tasks.steps.filter;

import io.softwareecg.awb.sdk.datamodel.RawSeriesData;

import java.util.function.Predicate;

/**
 * todo enter comment here
 *
 * @since 14.07.2018
 */
public interface SeriesFilter <T extends RawSeriesData> extends Predicate<T> {

}
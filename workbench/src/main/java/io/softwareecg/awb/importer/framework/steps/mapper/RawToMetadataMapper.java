package io.softwareecg.awb.importer.framework.steps.mapper;

import io.softwareecg.awb.timeseries.RawSeriesData;
import io.softwareecg.awb.timeseries.SeriesData;

import java.util.function.Function;

/**
 * todo enter comment here
 *
 * @since 14.07.2018
 */
public interface RawToMetadataMapper<TIN extends RawSeriesData, TOUT extends SeriesData>
        extends Function<TIN, TOUT> {

}
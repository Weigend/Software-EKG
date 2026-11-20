package io.softwareecg.awb.sdk.importer.tasks.steps.mapper;

import io.softwareecg.awb.sdk.datamodel.RawSeriesData;
import io.softwareecg.awb.sdk.datamodel.SeriesData;

import java.util.function.Function;

/**
 * todo enter comment here
 *
 * @since 14.07.2018
 */
public interface RawToMetadataMapper<TIN extends RawSeriesData, TOUT extends SeriesData>
        extends Function<TIN, TOUT> {

}
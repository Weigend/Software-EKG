package io.softwareecg.awb.sdk.importer.tasks.steps.mapper;

import io.softwareecg.awb.sdk.datamodel.RawSeriesData;
import io.softwareecg.awb.sdk.datamodel.SeriesData;

import java.util.function.Function;

/**
 * Interface that represents mappers that will map the generic labels/series attributes
 * to the fixed set of EKG filter dimensions.
 *
 * RawToSeriesDataMapper will fill all dimensions with converted/mapped source attributes or generic labels
 *
 * @since 14.07.2018
 */
public interface RawToSeriesDataMapper<TIN extends RawSeriesData, TOUT extends SeriesData>
        extends Function<TIN, TOUT> {

}
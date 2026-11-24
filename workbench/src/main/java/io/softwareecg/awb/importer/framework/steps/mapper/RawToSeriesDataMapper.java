package io.softwareecg.awb.importer.framework.steps.mapper;

import io.softwareecg.awb.importer.model.RawSeriesData;
import io.softwareecg.awb.importer.model.SeriesData;

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
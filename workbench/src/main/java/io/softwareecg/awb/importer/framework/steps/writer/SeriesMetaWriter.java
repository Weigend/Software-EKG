package io.softwareecg.awb.importer.framework.steps.writer;

import io.softwareecg.awb.projectcontext.model.TargetProjectConfig;
import io.softwareecg.awb.timeseries.SeriesData;

import java.util.function.Function;

/**
 * todo enter comment here
 *
 * @since 14.07.2018
 */
public interface SeriesMetaWriter<TIN extends SeriesData, TOUT extends SeriesData> extends Function<TIN, TOUT> {

    default void setProjectConfiguration(TargetProjectConfig projectConfiguration) {

    }

    void commit();
}
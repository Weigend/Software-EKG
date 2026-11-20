package io.softwareecg.awb.sdk.importer.tasks.steps.writer;

import io.softwareecg.awb.sdk.awbapi.project.TargetProjectConfig;
import io.softwareecg.awb.sdk.datamodel.SeriesData;

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
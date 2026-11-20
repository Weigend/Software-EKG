package io.softwareecg.awb.sdk.importer.tasks.steps.reader;

import io.softwareecg.awb.sdk.awbapi.project.Project;
import io.softwareecg.awb.sdk.datamodel.SeriesData;

import java.util.function.Function;

/**
 * todo enter comment here
 *
 * @since 14.07.2018
 */
public interface SeriesPayloadReader<TIN extends SeriesData, TOUT extends SeriesData> extends Function<TIN, TOUT> {

    default void setProject(Project project) {

    }
}
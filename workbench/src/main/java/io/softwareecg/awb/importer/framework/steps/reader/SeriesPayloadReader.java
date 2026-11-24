package io.softwareecg.awb.importer.framework.steps.reader;

import io.softwareecg.awb.projectcontext.api.ProjectView;
import io.softwareecg.awb.importer.model.SeriesData;

import java.util.function.Function;

/**
 * todo enter comment here
 *
 * @since 14.07.2018
 */
public interface SeriesPayloadReader<TIN extends SeriesData, TOUT extends SeriesData> extends Function<TIN, TOUT> {

    default void setProject(ProjectView project) {

    }
}
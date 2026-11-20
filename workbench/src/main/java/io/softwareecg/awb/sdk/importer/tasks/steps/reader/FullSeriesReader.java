package io.softwareecg.awb.sdk.importer.tasks.steps.reader;

import io.softwareecg.awb.sdk.awbapi.project.Project;
import io.softwareecg.awb.sdk.awbapi.repository.Repository;
import io.softwareecg.awb.sdk.datamodel.SeriesData;

import java.util.function.Function;

/**
 * todo enter comment here
 *
 * @since 14.07.2018
 */
public interface FullSeriesReader <TIN extends SeriesData, TOUT extends SeriesData> extends Function<TIN, TOUT> {

    void setMetaDataRepository(Repository repository);

    void setProject(Project project);
}
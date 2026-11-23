package io.softwareecg.awb.importer.framework.steps.reader;

import io.softwareecg.awb.projectcontext.api.ProjectView;
import io.softwareecg.awb.repository.api.Repository;
import io.softwareecg.awb.timeseries.SeriesData;

import java.util.function.Function;

/**
 * todo enter comment here
 *
 * @since 14.07.2018
 */
public interface SeriesReader<TIN extends SeriesData, TOUT> extends Function<TIN, TOUT> {

    void setMetaDataRepository(Repository repository);

    void setProject(ProjectView project);

}
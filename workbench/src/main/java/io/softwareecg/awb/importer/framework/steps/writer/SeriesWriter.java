package io.softwareecg.awb.importer.framework.steps.writer;

import io.softwareecg.awb.projectcontext.model.TargetProjectConfig;
import io.softwareecg.awb.timeseries.port.TimeSeriesWriteService;
import io.softwareecg.awb.timeseries.SeriesData;

import java.util.function.Function;

/**
 * A functional interface that represents writer used to write
 * SeriesData beans to the EKG repository (indexing).
 *
 * @since 14.07.2018
 */
public interface SeriesWriter<TIN extends SeriesData, TOUT extends SeriesData> extends Function<TIN, TOUT> {

    /**
     * Sets the repository all data of the should written to.
     *
     * @param seriesImportService todo write comment
     */
    default void setSeriesImportService(TimeSeriesWriteService seriesImportService) {
        // no op per default
    }

    /**
     * Sets the project all the series data written after that will
     * assigned to until a new project context is set.
     *
     * @param projectConfiguration the
     */
    default void setProjectConfiguration(TargetProjectConfig projectConfiguration) {
        // no op per default
    }

    /**
     * Commits/persists the data in the repository.
     * This function should called infrequent to avoid performance problems.
     */
    void commit();
}
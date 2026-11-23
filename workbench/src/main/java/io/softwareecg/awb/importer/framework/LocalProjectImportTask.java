package io.softwareecg.awb.importer.framework;

import io.softwareecg.awb.projectcontext.model.TargetProjectConfig;
import io.softwareecg.awb.repository.port.TimeSeriesWriteService;
import io.softwareecg.awb.timeseries.RawSeriesData;
import io.softwareecg.awb.timeseries.SeriesData;
import io.softwareecg.awb.importer.framework.steps.filter.SeriesDeltaFilter;
import io.softwareecg.awb.importer.framework.steps.writer.SeriesWriter;

import java.util.stream.Stream;

/**
 * Import task that is designed to import time-series completely to
 * a local types.
 *
 * @since 14.07.2018
 */
public class LocalProjectImportTask extends ImportTask {

    /**
     * true if existing data should be overwritten the the new imported data
     */
    private boolean overrideExistingSeries = true;

    /**
     * The import strategy that provides the steps used at import procedure
     */
    private final LocalProjectImportStrategy importStrategy;

    /**
     * The ProjectConfiguration used to configure the import components
     * and provides the target project the data belongs to
     */
    private final TargetProjectConfig projectConfiguration;

    /**
     * Constructs a new instance of ImportTask with with properties given as parameters.
     *
     * @param importService service that provides the functionality to import time series data
     * @param projectConfiguration the project configuration with project and additional metadata
     * @param importStrategy import strategy that provides the steps used at import procedure
     */
    public LocalProjectImportTask(TimeSeriesWriteService importService, TargetProjectConfig projectConfiguration,
                                  LocalProjectImportStrategy importStrategy) {

        super(importService, projectConfiguration);
        this.importStrategy = importStrategy;
        this.projectConfiguration = projectConfiguration;
    }

    /**
     * Sets a boolean flag to control existing time-series with same metadata should be overwritten
     *
     * @param overrideExistingSeries true if existing data should be overwritten the the new imported data
     */
    public void setOverrideExistingSeries(boolean overrideExistingSeries) {
        this.overrideExistingSeries = overrideExistingSeries;
    }

    @Override
    public long doImport() {

        // map to series filter dimensions generic
        Stream<RawSeriesData> rawDataStream = untilCancelled(importStrategy.readRawSeriesData());

        // filter series
        rawDataStream = rawDataStream.filter(importStrategy.getSeriesFilter());

        // map series to EKG dimensions
        Stream<SeriesData> dataStream =
                rawDataStream.map(importStrategy.getRawToSeriesMapper(projectConfiguration.getProjectName()));

        // (delta/full) optional read payload
        dataStream = dataStream.map(importStrategy.getSeriesPayloadReader());

        // (delta/full) optional filter series that don't need to update
        SeriesDeltaFilter<SeriesData> filter = importStrategy.getSeriesDeltaFilter();
        filter.overrideExistingSeries(overrideExistingSeries);
        dataStream = dataStream.filter(filter);

        // (delta/full) visualisation payload and optional metadata after filtering (cpu expensive steps will taken here)
        dataStream = dataStream.map(importStrategy.getComputeSeriesProcessor());

        // (delta/full) optional filter series that don't need to update and return the amount of written records
        SeriesWriter<SeriesData, SeriesData> writer = importStrategy.getSeriesDataWriter();
        writer.setProjectConfiguration(projectConfiguration);
        writer.setSeriesImportService(getImportService());
        long amountImportedSeries = dataStream.map(writer).count();
        if (!isCancelledByUser()) {
            // commit() also writes the series the writer still holds back
            writer.commit();
        }
        return amountImportedSeries;
    }
}

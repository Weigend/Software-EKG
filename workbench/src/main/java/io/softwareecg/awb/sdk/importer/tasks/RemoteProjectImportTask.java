package io.softwareecg.awb.sdk.importer.tasks;

import io.softwareecg.awb.sdk.awbapi.project.TargetProjectConfig;
import io.softwareecg.awb.sdk.awbapi.repository.ImporterSourceRepository;
import io.softwareecg.awb.sdk.awbapi.repository.Repository;
import io.softwareecg.awb.sdk.awbapi.repository.SeriesImportService;
import io.softwareecg.awb.sdk.awbapi.repository.SourceRepositoryService;
import io.softwareecg.awb.sdk.core.lookup.ServiceDiscovery;
import io.softwareecg.awb.sdk.datamodel.RawSeriesData;
import io.softwareecg.awb.sdk.datamodel.SeriesData;
import io.softwareecg.awb.sdk.importer.tasks.steps.writer.SeriesMetaWriter;

import java.util.stream.Stream;

/**
 * Import task that is designed to import the metadata of time-series to a local types
 * and leave time-series values at a remote source.
 *
 * This kind of import task will only used for remote sources that already contain time-series data
 * and EKG should use it with possibility for of better filter and viewing features.
 *
 * @since 14.07.2018
 */
public class RemoteProjectImportTask extends ImportTask {

    /**
     * The import strategy that provides the steps used at import procedure
     */
    private final SplitSourceProjectImportStrategy importStrategy;

    /**
     * An instance of ImporterSourceRepository that provides the time series data stored
     * remotely and the according meta data.
     */
    private final ImporterSourceRepository importerSourceRepository;


    /**
     * Constructs a new instance of ImportTask with with properties given as parameters.
     *
     * @param importService service that provides the functionality to import time series data
     * @param repository the target repository for the series meta data chosen by the EKG user
     * @param projectConfiguration the project configuration with project and additional metadata
     * @param importStrategy import strategy that provides the steps used at import procedure
     */
    public RemoteProjectImportTask(SeriesImportService importService,
                                   Repository repository,
                                   TargetProjectConfig projectConfiguration,
                                   SplitSourceProjectImportStrategy importStrategy) {

        super(importService, projectConfiguration);
        this.importStrategy = importStrategy;
        SourceRepositoryService service = ServiceDiscovery.lookup(SourceRepositoryService.class, repository);
        importerSourceRepository = service.queryImportSourceRepository(projectConfiguration.getProjectName());
    }


    @Override
    public long doImport() {

        // map to series filter dimensions generic
        Stream<RawSeriesData> rawDataStream = untilCancelled(importStrategy.readRawSeriesData(importerSourceRepository));

        // filter series
        rawDataStream = rawDataStream.filter(importStrategy.getSeriesFilter());

        // map series to EKG dimensions
        Stream<SeriesData> dataStream = rawDataStream.map(
                importStrategy.getRawToMetadataMapper(getTargetProject().getProjectName())
        );

        // (delta/full) optional filter series that don't need to update
        dataStream = dataStream.filter(importStrategy.getMetadataDeltaFilter());

        // (delta/full) visualisation metadata after filtering (cpu expensive steps will taken here)
        dataStream = dataStream.map(importStrategy.getComputeMetadataProcessor());

        // (delta/full) write series metadata to local types (remote type) and return the amount of written records
        // (delta/full) optional filter series that don't need to update and return the amount of written records
        SeriesMetaWriter<SeriesData, SeriesData> writer = importStrategy.getMetaDataWriter();
        writer.setProjectConfiguration(getTargetProject());
        long amountImportedSeries =  dataStream.map(writer).count();
        if (!isCancelledByUser()) {
            writer.commit();
        }
        return amountImportedSeries;
    }
}
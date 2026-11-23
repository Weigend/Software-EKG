package io.softwareecg.awb.importer.framework;

import io.softwareecg.awb.projectcontext.model.TargetProjectConfig;
import io.softwareecg.awb.timeseries.dimension.FilterDimension;
import io.softwareecg.awb.repository.port.TimeSeriesImportSource;
import io.softwareecg.awb.repository.port.TimeSeriesWriteService;
import io.softwareecg.wfx.platform.api.EventBus;
import io.softwareecg.wfx.platform.api.events.ProgressEvent;
import io.softwareecg.wfx.lookup.api.Lookup;
import io.softwareecg.awb.runtime.discovery.ServiceDiscovery;
import io.softwareecg.awb.timeseries.RawSeriesData;
import io.softwareecg.awb.timeseries.SeriesData;
import io.softwareecg.awb.importer.framework.steps.filter.MetadataDeltaFilter;
import io.softwareecg.awb.importer.framework.steps.filter.SeriesFilter;
import io.softwareecg.awb.importer.framework.steps.mapper.ComputeMetadataProcessor;
import io.softwareecg.awb.importer.framework.steps.mapper.RawToMetadataMapper;
import io.softwareecg.awb.importer.framework.steps.writer.SeriesMetaWriter;

import java.util.EventObject;
import java.util.stream.Stream;

import io.softwareecg.awb.repository.api.Repository;
import io.softwareecg.awb.repository.port.RepositoryException;
/**
 * todo enter comment here
 *
 * @since 14.07.2018
 */
public abstract class SplitSourceProjectImportStrategy {

    /**
     * The EKG event bus used by this strategy to notify the progress
     */
    private final EventBus<EventObject> eventBus = Lookup.lookup(EventBus.class);

    /**
     *
     */
    private TargetProjectConfig projectConfiguration;

    /**
     *
     */
    private TimeSeriesWriteService importService;

    /**
     *
     *
     * @param selectedRepository
     * @param currentProjectConfig
     * @return
     */
    public RemoteProjectImportTask createImportTask(Repository selectedRepository, TargetProjectConfig currentProjectConfig) {
        this.projectConfiguration = currentProjectConfig;
        this.importService = ServiceDiscovery.lookup(TimeSeriesWriteService.class, selectedRepository);
        return new RemoteProjectImportTask(importService, selectedRepository, projectConfiguration, this);
    }

    /**
     *
     *
     * @param projectName
     * @return
     */
    public RawToMetadataMapper<RawSeriesData, SeriesData> getRawToMetadataMapper(String projectName) {
        return rawSeriesData -> {

            // return a generic mapper
            return new SeriesData(
                    projectConfiguration.getProjectName(),
                    rawSeriesData.getSeriesLabels().get(FilterDimension.HOST_GROUP.name()),
                    rawSeriesData.getSeriesLabels().get(FilterDimension.HOST.name()),
                    rawSeriesData.getSeriesLabels().get(FilterDimension.NAMESPACE.name()),
                    rawSeriesData.getSeriesLabels().get(FilterDimension.SERVICE.name()),
                    rawSeriesData.getSeriesLabels().get(FilterDimension.POD.name()),
                    rawSeriesData.getSeriesLabels().get(FilterDimension.CONTAINER.name()),
                    rawSeriesData.getSeriesLabels().get(FilterDimension.MEASUREMENT.name()),
                    rawSeriesData.getSeriesLabels().get(FilterDimension.PROCESS_GROUP.name()),
                    rawSeriesData.getSeriesLabels().get(FilterDimension.PROCESS.name()),
                    rawSeriesData.getSeriesLabels().get(FilterDimension.METRIC_GROUP.name()),
                    rawSeriesData.getSeriesLabels().get(FilterDimension.METRIC_NAME.name())
            );
        };
    }

    /**
     *
     *
     * @return
     */
    public SeriesFilter<RawSeriesData> getSeriesFilter() {
        return series -> true;
    }

    public MetadataDeltaFilter<SeriesData> getMetadataDeltaFilter() {
        return seriesMetaData -> true;
    }

    public ComputeMetadataProcessor<SeriesData, SeriesData> getComputeMetadataProcessor() {
        return seriesMetaData -> seriesMetaData;
    }

    public SeriesMetaWriter<SeriesData, SeriesData> getMetaDataWriter() {
        return new SeriesMetaWriter<>() {

            private boolean overwriteExistingData;

            @Override
            public void setProjectConfiguration(TargetProjectConfig projectConfiguration) {
                overwriteExistingData = projectConfiguration.doOverwriteExistingData();
            }

            @Override
            public void commit() {
                try {
                    importService.commit();
                } catch (RepositoryException e) {
                    throw new IllegalStateException(e);
                }
            }

            @Override
            public SeriesData apply(SeriesData seriesMetaData) {
                try {

                    // todo: handle overwrite = true/false

                    importService.add(Stream.of(seriesMetaData));
                } catch (RepositoryException e) {
                    throw new IllegalStateException(e);
                }
                return seriesMetaData;
            }
        };
    }

    protected abstract Stream<RawSeriesData> readRawSeriesData(TimeSeriesImportSource sourceRepositoryDefinition);

    //-----------------------------------------------------------------------------------------------------------------
    // helper methods for common event/update functionality that can used by deriving classes
    //-----------------------------------------------------------------------------------------------------------------

    protected void notifyProgress(int currentDataEntryNumber, int totalAmountOfDataEntries) {
        double progress = (double) currentDataEntryNumber * ((double) totalAmountOfDataEntries / 100);

        publish(new ProgressEvent("Importing record " + currentDataEntryNumber + " of "
                + totalAmountOfDataEntries + "...", progress, this));
    }

    protected void notifyImportStarted(String message) {
        // notify other components that the import process begins
        publish(new ProgressEvent(message, 0.0, this));
    }

    /**
     * Publish a event to all registered listeners. The event ist dispatched by
     * using event.getClass().
     *
     * @param event the event.
     */
    protected void publish(EventObject event) {
        eventBus.publish(event);
    }
}

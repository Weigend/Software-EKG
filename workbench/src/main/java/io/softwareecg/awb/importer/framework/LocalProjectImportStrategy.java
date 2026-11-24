package io.softwareecg.awb.importer.framework;

import io.softwareecg.awb.projectcontext.api.TargetProjectConfig;
import io.softwareecg.awb.timeseries.dimension.FilterDimension;
import io.softwareecg.awb.repository.api.RepositoryException;
import io.softwareecg.awb.repository.api.TimeSeriesWriteService;
import io.softwareecg.wfx.platform.api.EventBus;
import io.softwareecg.wfx.platform.api.events.ProgressEvent;
import io.softwareecg.wfx.lookup.api.Lookup;
import io.softwareecg.awb.importer.model.RawSeriesData;
import io.softwareecg.awb.importer.model.SeriesData;
import io.softwareecg.awb.timeseries.TimeSeries;
import io.softwareecg.awb.importer.framework.steps.filter.SeriesDeltaFilter;
import io.softwareecg.awb.importer.framework.steps.filter.SeriesFilter;
import io.softwareecg.awb.importer.framework.steps.mapper.ComputeSeriesProcessor;
import io.softwareecg.awb.importer.framework.steps.mapper.RawToSeriesDataMapper;
import io.softwareecg.awb.importer.framework.steps.reader.SeriesPayloadReader;
import io.softwareecg.awb.importer.framework.steps.writer.SeriesWriter;

import java.util.*;
import java.util.stream.Stream;

/**
 * The base import strategy for all strategies that import the whole time series data
 * to the EKG own types.
 * The strategy defines a couple of hooks deriving strategies can use to add aditional
 * transform, filter or read/write steps.
 *
 * @since 14.07.2018
 */
public abstract class LocalProjectImportStrategy {

    /**
     * The EKG event bus used by this strategy to notify the progress
     */
    private final EventBus<EventObject> eventBus = Lookup.lookup(EventBus.class);

    /**
     * The start point of each LocalProjectImportStrategy implementation that will read the data
     * from the different kind of data sources and transform it to a stream of RawSeriesData that
     * will given to the further import steps.
     *
     * @return a stream of RawSeriesData that represents the source data the importer is used for
     */
    public abstract Stream<RawSeriesData> readRawSeriesData();

    /**
     * Default implementation of a factory method that provides a mapper
     * to convert a RawSeriesData instance to a SeriesData instance.
     *
     * The default implementation assumes that the RawSeriesData instance already provides
     * the correct filter dimensions using the Enum names of FilterDimension.
     *
     * The method should overwritten by derived ImportStrategy classes which need a more sophisticated behavior
     *
     * @param projectName the name of the project the series will imported to
     * @return a generic mapper to convert RawSeriesData to SeriesData
     */
    public RawToSeriesDataMapper<RawSeriesData, SeriesData> getRawToSeriesMapper(String projectName) {
        return rawSeriesData -> {

            // return a generic mapper
            return new SeriesData(
                    projectName,
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
                    rawSeriesData.getSeriesLabels().get(FilterDimension.METRIC_NAME.name()),
                    rawSeriesData.getValues()
            );
        };
    }

    /**
     * Creates a new LocalProjectImportTask that uses this strategy to proceed and import time series
     * data to EKG Repository
     *
     * @param importService service that provides the functionality to import time series data
     * @param projectConfiguration the project configuration that tell which project the data belong to
     * @return a new instance of LocalProjectImportTask
     */
    public LocalProjectImportTask createImportTask(TimeSeriesWriteService importService, TargetProjectConfig projectConfiguration) {
        return new LocalProjectImportTask(importService, projectConfiguration, this);
    }

    /**
     * Provides a SeriesPayloadReader instance that will optional used to read the whole or additional
     * payload data of the time series.
     * The returned instance has to be thread safe.
     *
     * @return an instance of SeriesPayloadReader
     */
    public SeriesPayloadReader<SeriesData, SeriesData> getSeriesPayloadReader() {
        return series -> series;
    }

    /**
     * Returns a filter function that decides if the Series should imported to
     * the repository or not. The method will called after reading the source
     * data and transform it to RawSeriesData.
     *
     * @return a function that retrieves a RawSeriesData entity as argument and return
     *         true if the series should imported or false if not.
     */
    public SeriesFilter<RawSeriesData> getSeriesFilter() {
        return series -> true;
    }

    public SeriesDeltaFilter<SeriesData> getSeriesDeltaFilter() {
        return seriesMetaData -> true;
    }

    public ComputeSeriesProcessor<SeriesData, SeriesData> getComputeSeriesProcessor() {
        return seriesData -> seriesData;
    }

    public SeriesWriter<SeriesData, SeriesData> getSeriesDataWriter() {

        return new SeriesWriter<>() {

            private static final int MAX_ENTRIES = 600;

            private TimeSeriesWriteService seriesImportService;

            private boolean overwriteExistingData;

            private final Map<Thread, List<TimeSeries>> seriesCache = new HashMap<>();

            @Override
            public void setSeriesImportService(TimeSeriesWriteService seriesImportService)  {
                this.seriesImportService = seriesImportService;
            }

            @Override
            public void setProjectConfiguration(TargetProjectConfig projectConfiguration) {
                overwriteExistingData = projectConfiguration.doOverwriteExistingData();
            }

            @Override
            public void commit() {
                try {
                    synchronized (seriesCache) {

                        for (List<TimeSeries> seriesList : seriesCache.values()) {
                            if (seriesList.isEmpty()) {
                                continue;
                            }

                            // todo handle overwrite data true/false

                            seriesImportService.add(seriesList.stream());
                            seriesList.clear();
                        }

                    }

                    seriesImportService.commit();

                } catch (RepositoryException e) {
                    throw new IllegalStateException(e);
                }
            }

            @Override
            public SeriesData apply(SeriesData seriesData) {

                List<TimeSeries> seriesList = seriesCache.get(Thread.currentThread());

                try {
                    if (seriesList == null) {
                        seriesList = new ArrayList<>();

                        synchronized (seriesCache) {
                            seriesCache.put(Thread.currentThread(), seriesList);
                        }
                    }

                    if (seriesList.size() >= MAX_ENTRIES) {
                        seriesList.add(seriesData);

                        // todo handle overwrite data true/false

                        seriesImportService.add(seriesList.stream());
                        seriesList.clear();

                    } else {
                        seriesList.add(seriesData);
                    }

                } catch (RepositoryException e) {
                    throw new IllegalStateException(e);
                }

                return seriesData;
            }
        };
    }



    protected void notifyImportStarted(String message) {
        // notify other components that the import process begins
        publish(new ProgressEvent(message, 0.0, this));
    }

    /**
     * Publish a event to all registered listeners. The event ist dispatched by
     * using event.getClass().
     *
     * Caution: the progress itself (double value) must be in the range of 0-1 with 1=100%
     *
     * @param event the event.
     */
    protected void publish(EventObject event) {
        eventBus.publish(event);
    }
}

package io.softwareecg.awb.datasource.model;

import io.softwareecg.awb.repository.api.Repository;
import io.softwareecg.awb.repository.api.RepositoryException;
import io.softwareecg.awb.repository.api.SeriesFilter;
import io.softwareecg.awb.timeseries.dimension.FilterDimension;

/**
 * Reads the end of a series from the repository of the analyses: the newest stored part of
 * exactly this series - same systems, same process, same metric - says where it ends.
 */
public final class RepositorySeriesEnds implements SeriesEnds {

    private final Repository repository;

    public RepositorySeriesEnds(Repository repository) {
        this.repository = repository;
    }

    @Override
    public long endOf(String projectName, String measurement, SeriesDimensions series) throws RepositoryException {
        return repository.series().endOf(SeriesFilter.ofProject(projectName)
                .is(FilterDimension.MEASUREMENT, measurement)
                .is(FilterDimension.HOST_GROUP, series.hostGroup())
                .is(FilterDimension.HOST, series.host())
                .is(FilterDimension.PROCESS_GROUP, series.processGroup())
                .is(FilterDimension.PROCESS, series.process())
                .is(FilterDimension.METRIC_GROUP, series.metricGroup())
                .is(FilterDimension.METRIC_NAME, series.metric()));
    }
}

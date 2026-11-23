package io.softwareecg.awb.importer.testsupport.mocks;

import io.softwareecg.awb.repository.api.Repository;
import io.softwareecg.awb.timeseries.port.RepositoryException;
import io.softwareecg.awb.timeseries.TimeSeries;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Simple CollectorSeriesImportService implementation that collects the
 * data in-memory to provide it later to the runner/tests.
 *
 * @since 24.03.2020
 */
public class SimpleCollectorSeriesImportService implements CollectorSeriesImportService {

    private final List<TimeSeries> timeSeries = new ArrayList<>();

    @Override
    public String getRepositoryId() {
            return null;
        }

    @Override
    public long resolveLastUpdate(TimeSeries seriesWithoutValues) throws RepositoryException {
        return 0;
    }

    @Override
    public void add(TimeSeries series) throws RepositoryException {
        timeSeries.add(series);
    }

    @Override
    public void add(Stream<TimeSeries> series) throws RepositoryException {
        timeSeries.addAll(series.collect(Collectors.toList()));
    }

    @Override
    public void add(Iterable<TimeSeries> series) throws RepositoryException {
        series.forEach(timeSeries::add);
    }

    @Override
    public void commit() throws RepositoryException {

    }

    @Override
    public List<TimeSeries> getCollectedSeriesData() {
        return timeSeries;
    }
}

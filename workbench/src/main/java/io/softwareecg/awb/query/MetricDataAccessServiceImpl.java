package io.softwareecg.awb.query;

import io.softwareecg.awb.query.api.MetricDataAccessService;

import io.softwareecg.awb.timeseries.dimension.Process;
import io.softwareecg.awb.timeseries.dimension.logical.Container;
import io.softwareecg.awb.timeseries.dimension.logical.Namespace;
import io.softwareecg.awb.timeseries.dimension.logical.Pod;
import io.softwareecg.awb.timeseries.dimension.logical.Service;
import io.softwareecg.awb.timeseries.dimension.physical.Host;
import io.softwareecg.awb.timeseries.dimension.physical.HostGroup;
import io.softwareecg.awb.query.model.QuerySpec;
import io.softwareecg.awb.query.model.TimeSeriesQuery;
import io.softwareecg.awb.query.model.TimeSeriesQueryResponse;
import io.softwareecg.awb.projectcontext.model.Project;
import io.softwareecg.awb.repository.api.TimeSeriesImportSource;
import io.softwareecg.awb.repository.api.Repository;
import io.softwareecg.awb.repository.api.RepositoryException;
import io.softwareecg.awb.repository.api.SeriesFilter;
import io.softwareecg.awb.repository.api.SeriesPage;
import io.softwareecg.awb.runtime.logging.EkgLogger;
import io.softwareecg.wfx.lookup.api.Lookup;
import io.softwareecg.awb.timeseries.TimeSeries;
import io.softwareecg.awb.timeseries.Value;
import io.softwareecg.awb.importer.spi.RemoteSeriesDataFetcher;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Stream;

import io.softwareecg.awb.timeseries.dimension.FilterDimension;
import io.softwareecg.awb.timeseries.dimension.Measurement;
import io.softwareecg.awb.timeseries.dimension.Metric;
import io.softwareecg.awb.timeseries.dimension.MetricGroup;
import io.softwareecg.awb.timeseries.dimension.NamedValueEntity;
import io.softwareecg.awb.timeseries.dimension.ProcessGroup;
/**
 * MetricDataAccessService default implementation that will provide functionality
 * to query and post compute time series and according facets for each filter dimension.
 *
 * @since 18.08.2018
 */
@Singleton
@SuppressWarnings("unused") // used via DI classpath scan
public class MetricDataAccessServiceImpl implements MetricDataAccessService {

    private static final Logger LOGGER = EkgLogger.get();

    private final SeriesDataFetcherRegistry fetcherRegistry;

    private Repository repository;

    private static final Map<TimeSeriesImportSource, RemoteSeriesDataFetcher> FETCHER_CACHE = new ConcurrentHashMap<>();

    //=================================================================================================================
    //  various constructors
    //=================================================================================================================

    @Inject
    public MetricDataAccessServiceImpl(@io.avaje.inject.External SeriesDataFetcherRegistry fetcherRegistry) {
        this.fetcherRegistry = fetcherRegistry;
    }

    /**
     * Default constructor that is need than instantiating this class
     * outside DI. Field will be lazily resolved on first use; under the
     * Avaje runtime Lookup is initialised before any user code reaches it.
     */
    @Deprecated
    public MetricDataAccessServiceImpl() {
        this.fetcherRegistry = Lookup.lookup(SeriesDataFetcherRegistry.class);
        // no op
    }

    /**
     * Parameterized constructor of this services that will use the given
     * repository for it's work.
     *
     * @param repository the repository to read
     */
    public MetricDataAccessServiceImpl(Repository repository) {
        this.fetcherRegistry = Lookup.lookup(SeriesDataFetcherRegistry.class);
        this.repository = repository;
    }

    //=================================================================================================================
    // API of the MetricDataAccessService interface and MetricDataAccessServiceImpl class itself
    //=================================================================================================================

    /**
     * Fetches time series data from persistence storage that matches to the specified filters in
     * the given query and post filter all values in the series that are not inside an (optional)
     * specified time interval
     *
     * @param query a query instance that defines the filter parameters for time series search
     * @return a response with 0 to n time series data
     */
    public TimeSeriesQueryResponse queryTimeSeriesData(TimeSeriesQuery query) throws RepositoryException {

        try {
            QuerySpec queryParams = query.getQueryParams();
            Project project = queryParams.getProject();

            // the API of the repository starts without a cursor
            String cursor = TimeSeriesQuery.INITIAL_CURSOR_ID.equals(query.getCursorId()) ? null : query.getCursorId();
            SeriesPage page = repository.series().find(filterOf(queryParams), cursor, query.getMaxMetricLimit());

            if (page.totalHits() > query.getMaxMetricLimit()) {
                return new TimeSeriesQueryResponse(query.getCursorId(), page.totalHits(), true);
            }

            // populate TimeSeriesQueryResponse instance with time series data (in case of remote projects) fetch
            // payload from remote data sources using a RemoteSeriesDataFetcher
            TimeSeriesQueryResponse response = new TimeSeriesQueryResponse(page.nextCursor(), page.totalHits(), false);
            final RemoteSeriesDataFetcher fetcher = retrieveFetcher(queryParams, project);

            for (TimeSeries timeSeries : page.series()) {

                // fetch data from remote data source if required
                if (project.useSplitSource() && fetcher != null) {

                    Value[] data = fetcher.fetchSeriesData(timeSeries.getRemoteSeriesKey(),
                            queryParams.getStart(), queryParams.getEnd());

                    if (data == null) {
                        // this occurs in error cases for example if remote repository isn't available
                        response.addTimeSeries(timeSeries);
                        response.setRequestAborted(true);
                        return response;
                    }

                    timeSeries.clear();
                    Stream.of(data).forEach(timeSeries::addValue);

                } else {
                    cutToInterval(timeSeries, queryParams.getStart(), queryParams.getEnd());
                }

                response.addTimeSeries(timeSeries);
            }

            return response;

        } catch (RepositoryException ex) {
            LOGGER.error("Exception raised while retrieving data from Repository.", ex);
            throw ex;
        }
    }

    @Override
    public long getAmountMeasuredPointsInProject(String projectName) {
        try {
            return StringUtils.isNotBlank(projectName) ? repository.countValuesOf(projectName) : 0;
        } catch (RepositoryException ex) {
            LOGGER.error("Exception raised while counting the values of a project.", ex);
            throw new IllegalStateException(ex);
        }
    }

    @Override
    public List<Process> getProcesses(QuerySpec query) {
        QuerySpec queryClone = new QuerySpec.Builder(query).withProcess("*").build();
        return queryFacets(queryClone, FilterDimension.PROCESS,
                new Process("*", "all"), name -> new Process(name, name));
    }

    @Override
    public List<ProcessGroup> getProcessGroups(QuerySpec query) {
        QuerySpec queryClone = new QuerySpec.Builder(query).withProcessGroup("*").build();
        return queryFacets(queryClone, FilterDimension.PROCESS_GROUP,
                new ProcessGroup("*"), ProcessGroup::new);
    }

    @Override
    public List<HostGroup> getHostGroups(QuerySpec query) {
        QuerySpec queryClone = new QuerySpec.Builder(query).withHostGroup("*").build();
        return queryFacets(queryClone,
                FilterDimension.HOST_GROUP, new HostGroup("*"), HostGroup::new);

    }

    @Override
    public List<Host> getHosts(QuerySpec query) {
        QuerySpec queryClone = new QuerySpec.Builder(query).withHost("*").build();
        return queryFacets(queryClone, FilterDimension.HOST,
                new Host("*"), Host::new);
    }

    @Override
    public List<Namespace> getNamespaces(QuerySpec query) {
        QuerySpec queryClone = new QuerySpec.Builder(query).withNamespace("*").build();
        return queryFacets(queryClone, FilterDimension.NAMESPACE,
                new Namespace("*"), Namespace::new);

    }

    @Override
    public List<Service> getServices(QuerySpec query) {
        QuerySpec queryClone = new QuerySpec.Builder(query).withService("*").build();
        return queryFacets(queryClone, FilterDimension.SERVICE,
                new Service("*"), Service::new);
    }

    @Override
    public List<Pod> getPods(QuerySpec query) {
        QuerySpec queryClone = new QuerySpec.Builder(query).withPod("*").build();
        return queryFacets(queryClone, FilterDimension.POD,
                new Pod("*"), Pod::new);
    }

    @Override
    public List<Container> getContainers(QuerySpec query) {
        QuerySpec queryClone = new QuerySpec.Builder(query).withContainer("*").build();
        return queryFacets(queryClone, FilterDimension.CONTAINER,
                new Container("*"), Container::new);
    }

    @Override
    public List<Measurement> getMeasurements(QuerySpec query) {
        QuerySpec queryClone = new QuerySpec.Builder(query).withMeasurement("*").build();
        return queryFacets(queryClone, FilterDimension.MEASUREMENT,
                new Measurement("*"), Measurement::new);
    }

    @Override
    public List<MetricGroup> getMetricGroups(QuerySpec query) {
        QuerySpec queryClone = new QuerySpec.Builder(query).withMetricGroup("*").build();
        return queryFacets(queryClone, FilterDimension.METRIC_GROUP,
                new MetricGroup("*"), MetricGroup::new);
    }

    @Override
    public List<Metric> getMetricsNames(QuerySpec query) {
        QuerySpec queryClone = new QuerySpec.Builder(query).withMetric("*").build();
        return queryFacets(queryClone, FilterDimension.METRIC_NAME,
                new Metric("*"), Metric::new);
    }

    /* ======================================== private helper ============================================= */

    private <T> List<T> queryFacets(QuerySpec queryParams, FilterDimension dimension,
                                    T wildCardEntry, Function<String, T> converter) {

        try {
            List<T> resultList = new ArrayList<>();
            resultList.add(wildCardEntry);
            for (String value : repository.series().valuesOf(dimension, filterOf(queryParams))) {
                resultList.add(converter.apply(value));
            }
            return resultList;

        } catch (RepositoryException ex) {
            LOGGER.info("Repository is offline: {}", ex.getMessage());
            LOGGER.debug("Detailed facet query failure:", ex);
            return Collections.emptyList();
        }
    }

    /**
     * Translates what the user chose; the filter of the repository does not know how it is written.
     */
    private static SeriesFilter filterOf(QuerySpec queryParams) {
        SeriesFilter filter = SeriesFilter.ofProject(queryParams.getProjectName());

        // the facet filter for the different dimensions
        addIfChosen(filter, FilterDimension.HOST_GROUP, queryParams.getHostGroup());
        addIfChosen(filter, FilterDimension.HOST, queryParams.getHost());
        addIfChosen(filter, FilterDimension.NAMESPACE, queryParams.getNamespace());
        addIfChosen(filter, FilterDimension.SERVICE, queryParams.getService());
        addIfChosen(filter, FilterDimension.POD, queryParams.getPod());
        addIfChosen(filter, FilterDimension.CONTAINER, queryParams.getContainer());
        addIfChosen(filter, FilterDimension.PROCESS, queryParams.getProcess());
        addIfChosen(filter, FilterDimension.PROCESS_GROUP, queryParams.getProcessGroup());
        addIfChosen(filter, FilterDimension.MEASUREMENT, queryParams.getMeasurement());
        addIfChosen(filter, FilterDimension.METRIC_GROUP, queryParams.getMetricGroup());

        if (StringUtils.isNotBlank(queryParams.getExcludeMetricName())) {
            filter.matchesNot(FilterDimension.METRIC_NAME, queryParams.getExcludeMetricName());
        }

        // special treatment for the metric name that supports multi selection
        if (StringUtils.isNotBlank(queryParams.getMetricName())) {
            addIfChosen(filter, FilterDimension.METRIC_NAME, queryParams.getMetric());

        } else if (!queryParams.getMetricNames().isEmpty()) {
            filter.matches(FilterDimension.METRIC_NAME, queryParams.getMetricNames());
        }

        return filter.reachesInto(queryParams.getStart(), queryParams.getEnd());
    }

    /**
     * A choice is a pattern, or {@code -(pattern)} for all but this, or {@code |(pattern|pattern)}
     * for one of several.
     */
    private static void addIfChosen(SeriesFilter filter, FilterDimension dimension, NamedValueEntity entity) {

        if (entity != null && StringUtils.isNotBlank(entity.getValueName()) && !"*".equals(entity.getValueName())) {
            String exp = entity.getValueName();
            if (exp.startsWith("-(") && exp.endsWith(")")) {
                filter.matchesNot(dimension, exp.substring(2, exp.length() - 1));

            } else if (exp.startsWith("|(") && exp.endsWith(")")) {
                List<String> patterns = Arrays.asList(exp.substring(2, exp.length() - 1).split("\\|"));
                if (!patterns.isEmpty()) {
                    filter.matches(dimension, patterns);
                }

            } else {
                filter.matches(dimension, List.of(exp));
            }
        }
    }

    private RemoteSeriesDataFetcher retrieveFetcher(QuerySpec queryParams, Project project) {

        RemoteSeriesDataFetcher fetcher;
        if (project.useSplitSource()) {

            fetcher = FETCHER_CACHE.get(queryParams.getImporterSourceRepository());

            if (fetcher != null) {
                return fetcher;
            }

            fetcher = fetcherRegistry.retrieveSeriesDataFetcher(project.getImporterId());
            if (fetcher == null) {
                throw new IllegalStateException("Unable to retrieve fetcher for importerId '"
                        + project.getImporterId() + "'");
            }

            fetcher.setSourceRepositoryDefinition(queryParams.getImporterSourceRepository());

            FETCHER_CACHE.put(queryParams.getImporterSourceRepository(), fetcher);

        } else {
            fetcher = null;
        }
        return fetcher;
    }

    /**
     * Checks if a timestamp is inside a given interval.
     *
     * @param timestamp the timestamp.
     * @param begin     the begin - if null only the end will be checked
     * @param end       @return
     */
    private static boolean insideInterval(long timestamp, long begin, long end) {

        if (begin < 0)  {
            if (end > 0) {
                return timestamp < end;
            }
        } else {
            if (end > 0) {
                return begin < timestamp && timestamp < end;
            } else {
                return begin < timestamp;
            }
        }

        return true;
    }

    /**
     * Leaves only the values inside the interval in the series.
     * Two-pass: count matching points, then copy into exact-sized arrays.
     */
    private static void cutToInterval(TimeSeries timeSeries, long begin, long end) {
        if (begin <= 0 && end <= 0) {
            return;
        }

        long[] ts = timeSeries.getTimestamps();
        double[] vals = timeSeries.getValuesArray();

        // count matching points
        int count = 0;
        for (int i = 0; i < ts.length; i++) {
            if (insideInterval(ts[i], begin, end)) {
                count++;
            }
        }

        if (count == ts.length) {
            return;
        }

        if (count == 0) {
            timeSeries.clear();
            return;
        }

        // copy matching points into exact-sized arrays
        long[] filteredTs = new long[count];
        double[] filteredVals = new double[count];
        int idx = 0;
        for (int i = 0; i < ts.length; i++) {
            if (insideInterval(ts[i], begin, end)) {
                filteredTs[idx] = ts[i];
                filteredVals[idx] = vals[i];
                idx++;
            }
        }

        timeSeries.setSortedArrays(filteredTs, filteredVals);
    }
}

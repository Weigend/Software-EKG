package io.softwareecg.awb.queryengine;

import io.softwareecg.awb.timeseries.dimension.Process;
import io.softwareecg.awb.timeseries.dimension.logical.Container;
import io.softwareecg.awb.timeseries.dimension.logical.Namespace;
import io.softwareecg.awb.timeseries.dimension.logical.Pod;
import io.softwareecg.awb.timeseries.dimension.logical.Service;
import io.softwareecg.awb.timeseries.dimension.physical.Host;
import io.softwareecg.awb.timeseries.dimension.physical.HostGroup;
import io.softwareecg.awb.queryengine.model.MetricQuery;
import io.softwareecg.awb.investigation.model.QuerySpec;
import io.softwareecg.awb.queryengine.model.TimeSeriesQuery;
import io.softwareecg.awb.queryengine.model.TimeSeriesQueryResponse;
import io.softwareecg.awb.projectcontext.model.Project;
import io.softwareecg.awb.timeseries.port.TimeSeriesRepository;
import io.softwareecg.awb.timeseries.port.query.delete.DeleteParams;
import io.softwareecg.awb.timeseries.port.query.expr.ExprFactory;
import io.softwareecg.awb.timeseries.port.query.expr.Expression;
import io.softwareecg.awb.timeseries.port.query.facet.Facet;
import io.softwareecg.awb.timeseries.port.query.facet.FacetEntry;
import io.softwareecg.awb.timeseries.port.query.facet.FacetParams;
import io.softwareecg.awb.timeseries.port.query.search.SearchParams;
import io.softwareecg.awb.timeseries.port.query.search.SearchResult;
import io.softwareecg.awb.timeseries.port.query.search.SortField;
import io.softwareecg.awb.timeseries.schema.DocumentType;
import io.softwareecg.awb.timeseries.schema.EkgSchemaField;
import io.softwareecg.awb.repository.model.BinaryTimeSeries;
import io.softwareecg.awb.timeseries.port.TimeSeriesImportSource;
import io.softwareecg.awb.timeseries.port.RepositoryException;
import io.softwareecg.awb.timeseries.codec.DateValuePairCodec;
import io.softwareecg.awb.runtime.logging.EkgLogger;
import io.softwareecg.wfx.lookup.api.Lookup;
import io.softwareecg.awb.timeseries.TimeSeries;
import io.softwareecg.awb.timeseries.Value;
import io.softwareecg.awb.importer.spi.RemoteSeriesDataFetcher;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Stream;

import static io.softwareecg.awb.timeseries.port.query.expr.ExprFactory.*;
import static io.softwareecg.awb.timeseries.schema.EkgSchemaField.*;

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

    private TimeSeriesRepository repositoryClient;

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
     * TimeSeriesRepository for it's work.
     *
     * @param client the repository client to use
     */
    public MetricDataAccessServiceImpl(TimeSeriesRepository client) {
        this.fetcherRegistry = Lookup.lookup(SeriesDataFetcherRegistry.class);
        this.repositoryClient = client;
    }

    //=================================================================================================================
    // API of the MetricDataAccessService interface and MetricDataAccessServiceImpl class itself
    //=================================================================================================================

    @Override
    public void deleteTimeSeriesByProjectName(String projectName) {

        try {
            DeleteParams deleteQuery = new DeleteParams();
            deleteQuery.addFilter(DOC_TYPE, DocumentType.TIME_SERIES.toString());
            deleteQuery.addFilter(PROJECT_NAME, projectName);
            repositoryClient.delete(deleteQuery);
            repositoryClient.commit();
        } catch (RepositoryException ex) {
            LOGGER.error("Exception raised while trying to delete time series data.", ex);
            throw new IllegalStateException(ex);
        }
    }

    /**
     * Fetches time series data from persistence storage that matches to the specified filters in
     * the given query and post filter all values in the series that are not inside an (optional)
     * specified time interval
     *
     * @param query a query instance that defines the filter parameters for time series search
     * @return a response with 0 to n time series data
     */
    @Override
    public TimeSeriesQueryResponse queryTimeSeriesData(TimeSeriesQuery query) throws RepositoryException {

        try {
            QuerySpec queryParams = query.getQueryParams();
            Project project = queryParams.getProject();

            // prepare time series query
            SearchParams searchParams = new SearchParams();
            List<Expression> filterExpressions = new ArrayList<>();
            populateFilter(filterExpressions, queryParams);

            searchParams.withFilterQueries(filterExpressions.toArray(new Expression[0]));
            searchParams.withSortFields(List.of(
                    new SortField(TS_START, SortField.SortMode.ASC),
                    new SortField(ID, SortField.SortMode.ASC)
            ));

            searchParams.withCursor(query.getCursorId());

            // fetch time series with filter query as export stream
            long t0 = System.currentTimeMillis();
            SearchResult<BinaryTimeSeries> searchResult = repositoryClient.search(BinaryTimeSeries.class, searchParams);
            long tSearch = System.currentTimeMillis() - t0;
            long totalHits = searchResult.queryStatus().getNumberOfHits();

            if (totalHits > query.getMaxMetricLimit()) {
                return new TimeSeriesQueryResponse(query.getCursorId(), totalHits, true);
            }

            long t1 = System.currentTimeMillis();
            List<BinaryTimeSeries> timeSeriesList = searchResult.rows();
            long tRows = System.currentTimeMillis() - t1;
            String nextCursor = searchResult.queryStatus().getCursor();

            if (query.getCursorId() != null && query.getCursorId().equals(nextCursor)) {
                nextCursor = null;
            }

            // populate TimeSeriesQueryResponse instance with time series data (in case of remote projects) fetch
            // payload from remote data sources using a RemoteSeriesDataFetcher
            TimeSeriesQueryResponse response = new TimeSeriesQueryResponse(nextCursor, totalHits, false);
            final RemoteSeriesDataFetcher fetcher = retrieveFetcher(queryParams, project);

            long tDecode = 0;
            long totalBytes = 0;
            for (BinaryTimeSeries binaryTimeSeries : timeSeriesList) {
                // create new TimeSeries based on public API POJO's
                TimeSeries timeSeries = mapToTimeSeries(binaryTimeSeries);

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

                    Stream.of(data).forEach(timeSeries::addValue);

                } else {
                    long td0 = System.currentTimeMillis();
                    // decode byte[] directly to primitive arrays — no Value object allocation
                    byte[] rawData = binaryTimeSeries.getData();
                    totalBytes += rawData != null ? rawData.length : 0;
                    Object[] arrays = DateValuePairCodec.decodeToPrimitiveArrays(rawData);
                    filterAndSetArrays(timeSeries, (long[]) arrays[0], (double[]) arrays[1],
                            queryParams.getStart(), queryParams.getEnd());
                    tDecode += System.currentTimeMillis() - td0;
                }

                response.addTimeSeries(timeSeries);
            }

            LOGGER.info("Fetch chunk: search={}ms, rows={}ms, decode={}ms, series={}, bytes={}MB",
                    tSearch, tRows, tDecode, timeSeriesList.size(),
                    String.format("%.1f", totalBytes / (1024.0 * 1024.0)));

            return response;

        } catch (RepositoryException ex) {
            LOGGER.error("Exception raised while retrieving data from Repository.", ex);
            throw ex;
        }
    }

    @Override
    public long getAmountMeasuredPointsInProject(String projectName) {
        try {
            if (StringUtils.isNotBlank(projectName)) {
                return repositoryClient.sumFieldValue(TS_DATA_AMOUNT_VALUES,
                        List.of(ExprFactory.exactFilter(PROJECT_NAME, projectName)));
            }

            repositoryClient.sumFieldValue(TS_DATA_AMOUNT_VALUES, new ArrayList<>());

            return 0;
        } catch (RepositoryException ex) {
            LOGGER.error("Exception raised while trying to delete time series data.", ex);
            throw new IllegalStateException(ex);
        }
    }

    @Override
    public List<Process> getProcesses(MetricQuery query) {
        QuerySpec queryClone = new QuerySpec.Builder(query.getQueryParams()).withProcess("*").build();
        return queryFacets(queryClone, TS_PROCESS_NAME,
                new Process("*", "all"), facetEntry -> new Process(facetEntry.getName(), facetEntry.getName()));
    }

    @Override
    public List<ProcessGroup> getProcessGroups(MetricQuery query) {
        QuerySpec queryClone = new QuerySpec.Builder(query.getQueryParams()).withProcessGroup("*").build();
        return queryFacets(queryClone, TS_PROCESS_GROUP_NAME,
                new ProcessGroup("*"), facetEntry -> new ProcessGroup(facetEntry.getName()));
    }

    @Override
    public List<HostGroup> getHostGroups(MetricQuery query) {
        QuerySpec queryClone = new QuerySpec.Builder(query.getQueryParams()).withHostGroup("*").build();
        return queryFacets(queryClone,
                TS_HOST_GROUP_NAME, new HostGroup("*"), facetEntry -> new HostGroup(facetEntry.getName()));

    }

    @Override
    public List<Host> getHosts(MetricQuery query) {
        QuerySpec queryClone = new QuerySpec.Builder(query.getQueryParams()).withHost("*").build();
        return queryFacets(queryClone, TS_HOST_NAME,
                new Host("*"), facetEntry -> new Host(facetEntry.getName()));
    }

    @Override
    public List<Namespace> getNamespaces(MetricQuery query) {
        QuerySpec queryClone = new QuerySpec.Builder(query.getQueryParams()).withNamespace("*").build();
        return queryFacets(query.getQueryParams(), TS_NAMESPACE_NAME,
                new Namespace("*"), facetEntry -> new Namespace(facetEntry.getName()));

    }

    @Override
    public List<Service> getServices(MetricQuery query) {
        QuerySpec queryClone = new QuerySpec.Builder(query.getQueryParams()).withService("*").build();
        return queryFacets(query.getQueryParams(), TS_SERVICE_NAME,
                new Service("*"), facetEntry -> new Service(facetEntry.getName()));
    }

    @Override
    public List<Pod> getPods(MetricQuery query) {
        QuerySpec queryClone = new QuerySpec.Builder(query.getQueryParams()).withPod("*").build();
        return queryFacets(queryClone, TS_POD_NAME,
                new Pod("*"), c -> new Pod(c.getName()));
    }

    @Override
    public List<Container> getContainers(MetricQuery query) {
        QuerySpec queryClone = new QuerySpec.Builder(query.getQueryParams()).withContainer("*").build();
        return queryFacets(queryClone, TS_CONTAINER_NAME,
                new Container("*"), facetEntry -> new Container(facetEntry.getName()));
    }

    @Override
    public List<Measurement> getMeasurements(MetricQuery query) {
        QuerySpec queryClone = new QuerySpec.Builder(query.getQueryParams()).withMeasurement("*").build();
        return queryFacets(queryClone, TS_MEASUREMENT,
                new Measurement("*"), facetEntry -> new Measurement(facetEntry.getName()));
    }

    @Override
    public List<MetricGroup> getMetricGroups(MetricQuery query) {
        QuerySpec queryClone = new QuerySpec.Builder(query.getQueryParams()).withMetricGroup("*").build();
        return queryFacets(queryClone, TS_METRIC_GROUP,
                new MetricGroup("*"), facetEntry -> new MetricGroup(facetEntry.getName()));
    }

    @Override
    public List<Metric> getMetricsNames(MetricQuery query) {
        QuerySpec queryClone = new QuerySpec.Builder(query.getQueryParams()).withMetric("*").build();
        return queryFacets(queryClone, TS_METRIC_NAME,
                new Metric("*"), facetEntry -> new Metric(facetEntry.getName()));
    }

    @Override
    public void addEntities(Stream<TimeSeries> timeSeriesStream, boolean addEntities) throws RepositoryException {

        if (timeSeriesStream == null) {
            return;
        }

        repositoryClient.add(timeSeriesStream.map(timeSeries -> {

            BinaryTimeSeries binaryTimeSeries = new BinaryTimeSeries();

            // physical metric filter attributes
            binaryTimeSeries.setHost(timeSeries.getHost());
            binaryTimeSeries.setHostGroup(timeSeries.getHostGroup());

            // logical metric filter attributes
            binaryTimeSeries.setNamespace(timeSeries.getNamespace());
            binaryTimeSeries.setService(timeSeries.getService());
            binaryTimeSeries.setPod(timeSeries.getPod());
            binaryTimeSeries.setContainer(timeSeries.getContainer());

            // common metric filter attributes
            binaryTimeSeries.setMeasurement(timeSeries.getMeasurement());
            binaryTimeSeries.setProcess(timeSeries.getProcess());
            binaryTimeSeries.setProcessGroup(timeSeries.getProcessGroup());
            binaryTimeSeries.setMetric(timeSeries.getMetricName());
            binaryTimeSeries.setMetricGroup(timeSeries.getMetricGroup());
            binaryTimeSeries.setStart(timeSeries.getStartDate());
            binaryTimeSeries.setEnd(timeSeries.getEndDate());
            binaryTimeSeries.setProjectName(timeSeries.getProject());

            // other
            binaryTimeSeries.setRemoteSeriesKey(timeSeries.getRemoteSeriesKey());
            binaryTimeSeries.setData(DateValuePairCodec.compressAndEncodeDataPlainBytes(timeSeries.getValues()));

            return binaryTimeSeries;

        }).iterator());
    }

    @Override
    public void commitOrRollback() {
        try {
            repositoryClient.commit();
        } catch (RepositoryException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * Initialize this services with a repository client that should used.
     * This is necessary than the service is instantiated via CDI and doesn't has an initial client.
     *
     * @param client the repository client to use
     */
    public void initializeService(TimeSeriesRepository client) {
        this.repositoryClient = client;
    }

    /* ======================================== private helper ============================================= */

    private <T> List<T> queryFacets(QuerySpec queryParams, EkgSchemaField facetField,
                                    T wildCardEntry, Function<FacetEntry, T> converter) {

        try {
            FacetParams facetParams = new FacetParams()
                    .withLimit(2500)
                    .withMinCount(1)
                    .withSort(queryParams.getFacetSort())
                    .withFacetFields(facetField);

            List<Expression> andExpressions = new ArrayList<>();
            populateFilter(andExpressions, queryParams);
            facetParams.withFilterQueries(and(andExpressions.toArray(new Expression[0])));

            List<Facet> facets = repositoryClient.facet(facetParams).getFacets();

            List<T> resultList = new ArrayList<>(facets.size() + 1);
            if (facets.size() > 0) {
                resultList.add(0, wildCardEntry);
            }

            for (Facet facet : facets) {
                for (FacetEntry facetEntry : facet.entries()) {
                    resultList.add(converter.apply(facetEntry));
                }
            }

            return resultList;


        } catch (RepositoryException ex) {
            LOGGER.info("Repository is offline: {}", ex.getMessage());
            LOGGER.debug("Detailed facet query failure:", ex);
            return Collections.emptyList();
        }
    }

    private TimeSeries mapToTimeSeries(BinaryTimeSeries timeSeriesEntity) {
        TimeSeries timeSeries = new TimeSeries(
                timeSeriesEntity.getProjectName(),

                timeSeriesEntity.getHostGroup(),
                timeSeriesEntity.getHost(),

                timeSeriesEntity.getNamespace(),
                timeSeriesEntity.getService(),
                timeSeriesEntity.getPod(),
                timeSeriesEntity.getContainer(),

                timeSeriesEntity.getMeasurement(),
                timeSeriesEntity.getProcessGroup(),

                timeSeriesEntity.getProcess(),
                timeSeriesEntity.getMetricGroup(),
                timeSeriesEntity.getMetricName());

        timeSeries.setRemoteSeriesKey(timeSeriesEntity.getRemoteSeriesKey());

        return timeSeries;
    }

    private void populateFilter(List<Expression> andExpressions, QuerySpec queryParams) {

        // static parts that must always match
        andExpressions.add(exactFilter(DOC_TYPE, DocumentType.TIME_SERIES.toString()));
        andExpressions.add(exactFilter(PROJECT_NAME, queryParams.getProjectName()));

        // the facet filter for the different dimensions
        addWildcardExpressionIfNotNull(andExpressions, TS_HOST_GROUP_NAME, queryParams.getHostGroup());
        addWildcardExpressionIfNotNull(andExpressions, TS_HOST_NAME, queryParams.getHost());
        addWildcardExpressionIfNotNull(andExpressions, TS_NAMESPACE_NAME, queryParams.getNamespace());
        addWildcardExpressionIfNotNull(andExpressions, TS_SERVICE_NAME, queryParams.getService());
        addWildcardExpressionIfNotNull(andExpressions, TS_POD_NAME, queryParams.getPod());
        addWildcardExpressionIfNotNull(andExpressions, TS_CONTAINER_NAME, queryParams.getContainer());
        addWildcardExpressionIfNotNull(andExpressions, TS_PROCESS_NAME, queryParams.getProcess());
        addWildcardExpressionIfNotNull(andExpressions, TS_PROCESS_GROUP_NAME, queryParams.getProcessGroup());
        addWildcardExpressionIfNotNull(andExpressions, TS_MEASUREMENT, queryParams.getMeasurement());
        addWildcardExpressionIfNotNull(andExpressions, TS_METRIC_GROUP, queryParams.getMetricGroup());

        if (StringUtils.isNotBlank(queryParams.getExcludeMetricName())) {
            andExpressions.add(ExprFactory.not(wildcardFilter(TS_METRIC_NAME, queryParams.getExcludeMetricName())));
        }

        // special treatment for the metric name that supports multi selection
        List<String> metricNames = queryParams.getMetricNames();
        if (StringUtils.isNotBlank(queryParams.getMetricName())) {
            addWildcardExpressionIfNotNull(andExpressions, TS_METRIC_NAME, queryParams.getMetric());

        } else if (metricNames.size() > 1) {
            List<Expression> orExpressions = new ArrayList<>(metricNames.size());
            metricNames.forEach(name -> orExpressions.add(wildcardFilter(TS_METRIC_NAME, name)));
            andExpressions.add(or(orExpressions.toArray(new Expression[0])));

        } else if (metricNames.size() == 1) {
            andExpressions.add(wildcardFilter(TS_METRIC_NAME, metricNames.get(0)));
        }

        // Last but not least the filter for the requested time interval: every part of a series
        // that reaches into it - it ends after the start and begins before the end. The bounds
        // are taken as they are. They used to be moved inwards by a minute each, which dropped
        // the parts at the edges and, in a window of about a minute, nearly all of them.
        if (queryParams.getStart() > 0) {
            andExpressions.add(dateTimeRangeFilter(TS_STOP, Instant.ofEpochMilli(queryParams.getStart()), null));
        }

        if (queryParams.getEnd() > 0) {
            andExpressions.add(dateTimeRangeFilter(TS_START, null, Instant.ofEpochMilli(queryParams.getEnd())));
        }
    }

    private void addWildcardExpressionIfNotNull(List<Expression> expressions, EkgSchemaField field, NamedValueEntity entity) {

        if (entity != null && StringUtils.isNotBlank(entity.getValueName()) && !"*".equals(entity.getValueName())) {
            String exp = entity.getValueName();
            if (exp.startsWith("-(") && exp.endsWith(")")) {
                expressions.add(not(wildcardFilter(field, exp.substring(2, exp.length() - 1))));

            } else if(exp.startsWith("|(") && exp.endsWith(")")) {
                String[] filterTokens = exp.substring(2, exp.length() - 1).split("\\|");

                if (filterTokens.length > 1) {
                    List<Expression> orFilterExpressions = new ArrayList<>();

                    for (String token : filterTokens) {
                        orFilterExpressions.add(wildcardFilter(field, token));
                    }

                    expressions.add(or(orFilterExpressions.toArray(new Expression[0])));

                } else if (filterTokens.length == 1) {
                    expressions.add(wildcardFilter(field, filterTokens[0]));

                }

            } else {
                expressions.add(wildcardFilter(field, entity.getValueName()));
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
     * Filters primitive arrays by time interval and sets them directly on the TimeSeries.
     * Two-pass: count matching points, then copy into exact-sized arrays.
     */
    private static void filterAndSetArrays(TimeSeries timeSeries, long[] ts, double[] vals, long begin, long end) {
        boolean needsFilter = begin > 0 || end > 0;

        if (!needsFilter) {
            timeSeries.setSortedArrays(ts, vals);
            return;
        }

        // count matching points
        int count = 0;
        for (int i = 0; i < ts.length; i++) {
            if (insideInterval(ts[i], begin, end)) {
                count++;
            }
        }

        if (count == ts.length) {
            timeSeries.setSortedArrays(ts, vals);
            return;
        }

        if (count == 0) {
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

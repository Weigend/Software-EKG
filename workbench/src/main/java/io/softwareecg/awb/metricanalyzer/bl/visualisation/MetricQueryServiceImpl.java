package io.softwareecg.awb.metricanalyzer.bl.visualisation;

import io.softwareecg.awb.metricanalyzer.bl.api.SeriesCombineMode;
import io.softwareecg.awb.metricanalyzer.bl.api.SeriesSmoothingGranularity;
import io.softwareecg.awb.metricanalyzer.bl.api.SeriesSmoothingType;
import io.softwareecg.awb.metricanalyzer.bl.tsquery.MetricDataAccessService;
import io.softwareecg.awb.metricanalyzer.bl.tsquery.query.QueryComputeParams;
import io.softwareecg.awb.metricanalyzer.bl.tsquery.query.QueryFilterParams;
import io.softwareecg.awb.metricanalyzer.bl.tsquery.query.TimeSeriesQuery;
import io.softwareecg.awb.metricanalyzer.bl.tsquery.query.TimeSeriesQueryResponse;
import io.softwareecg.awb.metricanalyzer.bl.visualisation.combine.CombinerFactory;
import io.softwareecg.awb.metricanalyzer.bl.visualisation.combine.TimeSeriesCombiner;
import io.softwareecg.awb.metricanalyzer.bl.visualisation.simplification.SimplificationService;
import io.softwareecg.awb.metricanalyzer.bl.visualisation.simplification.algorithms.ValueChangeSimplificationService;
import io.softwareecg.awb.metricanalyzer.bl.visualisation.smoothing.SmootherFactory;
import io.softwareecg.awb.metricanalyzer.bl.visualisation.smoothing.TimeSeriesSmoother;
import io.softwareecg.awb.da.api.RepositoryClient;
import io.softwareecg.awb.sdk.awbapi.repository.RepositoryException;
import io.softwareecg.wfx.platform.api.EventBus;
import io.softwareecg.wfx.platform.api.events.ProgressEvent;
import io.softwareecg.awb.sdk.core.log.EkgLogger;
import io.softwareecg.awb.sdk.core.lookup.ServiceDiscovery;
import io.softwareecg.awb.sdk.datamodel.TimeSeries;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.apache.commons.lang3.time.StopWatch;
import org.slf4j.Logger;

import java.text.NumberFormat;
import java.util.*;

import static io.softwareecg.awb.metricanalyzer.bl.api.SeriesCombineMode.CONCAT;

/**
 * The default implementation of MetricsQueryService that will fetch the time series
 * data from types and do post processing on it like smoothing and vectorization.
 *
 * @since 02.06.2018
 */
@Singleton
@SuppressWarnings("unused") // used via DI / reflection
public class MetricQueryServiceImpl implements MetricQueryService {

    /**
     * Logger to protocol errors and special events
     */
    private static final Logger LOGGER = EkgLogger.get();

    /**
     * The MetricQueryService that provides the metric time-series low-level access to types data
     */
    private MetricDataAccessService metricDataAccess;

    protected final EventBus<EventObject> eventBus;

    /**
     * An SimplificationService used for vectorisation of the retrieved time series data
     */
    private final SimplificationService simplificationService;

    /**
     * The SmootherFactory that will provide different TimeSeriesSmoother implementations that matches to the parameters
     */
    private final SmootherFactory smootherFactory;

    @Inject
    public MetricQueryServiceImpl(@io.avaje.inject.External EventBus<EventObject> eventBus,
                                  SimplificationService simplificationService,
                                  SmootherFactory smootherFactory) {
        this.eventBus = eventBus;
        this.simplificationService = simplificationService;
        this.smootherFactory = smootherFactory;
    }


    @Override
    public void initializeService(RepositoryClient client) {
        this.metricDataAccess = ServiceDiscovery.lookup(MetricDataAccessService.class, client);
    }


    //================================================================================================================
    //  MetricsQueryService API
    //================================================================================================================

    /* (non-Javadoc)
     * @see io.softwareecg.awb.metricanalyzer.bl.api.MetricsQueryService#getComputedTimeSeries(...)
     */
    public ComputedTimeSeriesResponse getComputedTimeSeries(QueryFilterParams filterParams,
                                                            QueryComputeParams computeParams, int maxMetricLimit) {

        StopWatch stopWatch = StopWatch.createStarted();

        eventBus.publish(new ProgressEvent("Querying repository...", 0.05, this));

        // query we use to fetch the data
        int transitiveSeriesLimit = filterParams.getProject().useSplitSource() ? maxMetricLimit : Integer.MAX_VALUE;
        TimeSeriesQuery query = new TimeSeriesQuery(filterParams, transitiveSeriesLimit);

        boolean seriesMergeActive = computeParams.getSeriesCombineMode() != SeriesCombineMode.NONE;
        boolean isConcatMode = computeParams.getSeriesCombineMode() == SeriesCombineMode.CONCAT;

        // the maximum number of series to display — used for early exit from the fetch loop
        int maxDisplaySeries = computeParams.getMaxDisplaySeries();

        // the result list that will returned at the end
        List<TimeSeries> result = new ArrayList<>();
        Map<String, TimeSeries> groupingMap = new HashMap<>();

        long totalResults;
        long totalPoints = 0;
        long totalPointsInProject;
        boolean seriesTruncatedEarly = false;

        try {
            String cursorId = TimeSeriesQuery.INITIAL_CURSOR_ID;
            int fetchIteration = 0;
            while (true) {

                // we use cursor to fetch data as fast as possible
                query.setCursorId(cursorId);

                // query and retrieve data
                TimeSeriesQueryResponse response = metricDataAccess.queryTimeSeriesData(query);

                totalResults = response.getTotalHits();

                if (response.isRequestAborted()) {
                    return new ComputedTimeSeriesResponse(response.getTotalHits(), maxMetricLimit);
                }

                // all chunks of the streamed data consumed?
                if (response.isConsumed() && response.getData().isEmpty()) {
                    break;
                }

                fetchIteration++;

                // progress within fetch phase: 0.05 to 0.25
                if (totalResults > 0) {
                    long fetchedSoFar = Math.min(result.size(), totalResults);
                    double fetchProgress = 0.05 + 0.20 * ((double) fetchedSoFar / totalResults);
                    eventBus.publish(new ProgressEvent("Loading data... (" + fetchedSoFar + "/" + totalResults
                            + " series, " + NumberFormat.getInstance(Locale.GERMAN).format(totalPoints) + " values)",
                            Math.min(fetchProgress, 0.25), this));
                } else {
                    eventBus.publish(new ProgressEvent("Loading data... (chunk " + fetchIteration + ")",
                            0.10, this));
                }

                // token to address the next chunk
                cursorId = response.getCursorId();

                Collection<TimeSeries> rowsChunk = response.getData();

                for (TimeSeries fetchedSeries : rowsChunk) {

                    if (Thread.currentThread().isInterrupted()) {
                        return new ComputedTimeSeriesResponse(true, maxMetricLimit); // normal termination, return empty result
                    }

                    // use unique names as metric key
                    String groupingKey = fetchedSeries.getGroupingKey(computeParams.getSeriesCombineMode() == CONCAT);
                    TimeSeries mainSeries = groupingMap.get(groupingKey);

                    if (mainSeries == null) {
                        mainSeries = fetchedSeries;
                        totalPoints += mainSeries.getPointCount();

                        if (isConcatMode) {
                            mainSeries.setMeasurement("*");
                        }

                        groupingMap.put(groupingKey, mainSeries);
                        result.add(mainSeries);

                        // early exit: stop fetching when we have enough distinct series
                        // (only when no merge/combine is active, as combining may reduce the final count)
                        if (!seriesMergeActive && maxDisplaySeries > 0 && result.size() > maxDisplaySeries) {
                            seriesTruncatedEarly = true;
                            break;
                        }
                    } else {
                        totalPoints += fetchedSeries.getPointCount();
                        mainSeries.appendArrays(fetchedSeries.getTimestamps(), fetchedSeries.getValuesArray());
                    }
                }

                if (response.isConsumed() || seriesTruncatedEarly) {
                    break;
                }
            }

            stopWatch.suspend();
            LOGGER.info("Fetch loop completed in {}ms ({} series, {} points)", stopWatch.getTime(), result.size(), totalPoints);

            // Sort each series by timestamp — needed because appendArrays does not sort
            long tSortStart = System.currentTimeMillis();
            result.forEach(TimeSeries::sortByTimestamp);
            LOGGER.info("Sort {} series by timestamp in {}ms", result.size(), System.currentTimeMillis() - tSortStart);

            long tCountStart = System.currentTimeMillis();
            eventBus.publish(new ProgressEvent("Counting total values in project...", 0.28, this));
            totalPointsInProject = metricDataAccess.getAmountMeasuredPointsInProject(filterParams.getProjectName());
            LOGGER.info("getAmountMeasuredPointsInProject took {}ms", System.currentTimeMillis() - tCountStart);


        } catch (RepositoryException e) {
            LOGGER.error("Exception raised while getting the counters from types.", e);
            return new ComputedTimeSeriesResponse(maxMetricLimit, e);
        }

        // break if too many series because we can't fetch that much data at repository level
        if (!seriesMergeActive && result.size() > maxMetricLimit ||(
                isConcatMode && groupingMap.size() > maxMetricLimit)) {
            return new ComputedTimeSeriesResponse(result.size(), maxMetricLimit);
        }

        //-----------------------------------------------------------------------------------------------------------
        // at this point the data must be fully fetched from repository because the further steps need all data
        // for analyze and compute the time series correctly
        //-----------------------------------------------------------------------------------------------------------

        eventBus.publish(new ProgressEvent("Loaded " + result.size() + " series with " + totalPoints + " values. Processing...", 0.3, this));

        // this threshold means that only value changes in the series should proceed.
        // this have to be applied before the series will merged by the combiner
        if (computeParams.getThreshold() == 0) {
            result = new ValueChangeSimplificationService().simplify(result);
        }

        // add values of all time series to a single one if requested
        result = combineTimeSeriesData(query.getQueryParams().getMetricName(), result,
                computeParams.getSeriesCombineMode());

        eventBus.publish(new ProgressEvent("Smoothing time series...", 0.5, this));

        // flatten the value vector to make it nicer to view at UI if requested
        long tSmoothStart = System.currentTimeMillis();
        smoothingTimeSeriesData(result, computeParams.getSeriesSmoothingGranularity(),
                computeParams.getSeriesSmoothingType());
        LOGGER.info("Smoothing took {}ms", System.currentTimeMillis() - tSmoothStart);

        eventBus.publish(new ProgressEvent("Simplifying time series...", 0.65, this));

        // reduce the data points per individual time series using vectorization algorithm
        result = simplifyTimeSeriesData(result, computeParams.getThreshold());

        // truncate to max display series limit — keep only the first N series, track how many were omitted
        int truncatedCount = 0;
        if (maxDisplaySeries > 0 && result.size() > maxDisplaySeries) {
            truncatedCount = result.size() - maxDisplaySeries;
            result = new ArrayList<>(result.subList(0, maxDisplaySeries));
            LOGGER.info("Truncated series list from {} to {} (omitted {} series)",
                    maxDisplaySeries + truncatedCount, maxDisplaySeries, truncatedCount);
        }

        eventBus.publish(new ProgressEvent("Rendering chart...", 0.85, this));

        NumberFormat formatter = NumberFormat.getInstance(Locale.GERMAN);

        eventBus.publish(new ProgressEvent("Processed " + formatter.format(totalResults) + " series with "
                + formatter.format(totalPoints) + " values of "+ formatter.format(totalPointsInProject)
                + " total in " + stopWatch.getTime()  + "ms.", 1.0, this));

        ComputedTimeSeriesResponse response = new ComputedTimeSeriesResponse(result, maxMetricLimit);
        response.setTruncatedSeriesCount(truncatedCount);
        return response;
    }

    //================================================================================================================
    //  private helper to delegate post processing of time series
    //================================================================================================================

    /**
     * Reduce the amount of data points in each individual time series to a maximum defined
     * by the seriesValueLimit parameter. Each series is treated independently.
     *
     * @param timeSeriesList the ingoing list of time series that should be simplified.
     * @param seriesValueLimit the maximum amount of data points per individual time series.
     * @return a modified list of time series with reduced amount of data points if necessary.
     */
    private List<TimeSeries> simplifyTimeSeriesData(List<TimeSeries> timeSeriesList, int seriesValueLimit) {
        StopWatch stopwatch = StopWatch.createStarted();
        List<TimeSeries> simplified = simplificationService.simplify(timeSeriesList, seriesValueLimit);
        LOGGER.info("Simplify {} time series in {}", timeSeriesList.size(), stopwatch);

        return simplified;
    }

    /**
     * If requested this method will delegate each time series in the given list
     * to a processor that will visualisation a smoothing of the value graph based on the
     * specified smoothing typ and granularity. This will result in a flatten graph of
     * the series in the chart.
     *
     * The implementation will modify the series instances itself, so it have to be mutable.
     *
     * @param timeSeriesList a list of time series it's data will be modified (smoothed)
     * @param smoothingGranularity the sampling rate used to visualisation the smoothing
     * @param smoothingType the type of smoothing the call want's for the time series data
     */
    private void smoothingTimeSeriesData(final List<TimeSeries> timeSeriesList,
                                                     final SeriesSmoothingGranularity smoothingGranularity,
                                                     final SeriesSmoothingType smoothingType) {
        // early exit if nothing to do
        if (smoothingType == SeriesSmoothingType.NONE || timeSeriesList.isEmpty()) {
            return;
        }

        TimeSeriesSmoother seriesSmoother = smootherFactory.resolveSmoother(smoothingType, smoothingGranularity);

        // visualisation the smoothing of each time series multi-threaded
        timeSeriesList.parallelStream().forEach(timeSeries ->
                timeSeries.setSortedValues(seriesSmoother.computeSmoothing(timeSeries.getValues())));
    }

    /**
     * Combine multiple time series to a single one. Different values in same time increments
     * (overlapping of time series) will reduce by the chosen mode.
     *
     * None overlapping time series wan't change in any way
     *
     * @param newMetricName the metric name that have to use for the returned time series
     * @param timeSeriesList a list of time series that should combined if it overlap
     * @param combineMode the algorithm used to addValues overlapping parts of the time series.
     * @return a SingletonList with a time series that contains the whole time range of each given one
     */
    private List<TimeSeries> combineTimeSeriesData(String newMetricName, List<TimeSeries> timeSeriesList,
                                                   SeriesCombineMode combineMode) {

        if (combineMode == SeriesCombineMode.NONE || combineMode == CONCAT || timeSeriesList.isEmpty()) {
            return timeSeriesList;
        }

        StopWatch stopWatch = StopWatch.createStarted();
        LOGGER.info("Begin combining time series list with {} elements.", timeSeriesList.size());
        TimeSeriesCombiner combiner = CombinerFactory.resolveCombiner(combineMode);
        List<TimeSeries> result = Collections.singletonList(combiner.combine(newMetricName, timeSeriesList));
        LOGGER.info("Finished addValues {} time series to a single one in {}.", timeSeriesList.size(), stopWatch);

        return result;
    }
}

package io.softwareecg.awb.queryengine;

import io.softwareecg.awb.investigation.model.SeriesCombineMode;
import io.softwareecg.awb.investigation.model.SeriesSmoothingGranularity;
import io.softwareecg.awb.investigation.model.SeriesSmoothingType;
import io.softwareecg.awb.queryengine.MetricDataAccessService;
import io.softwareecg.awb.investigation.model.ComputationSpec;
import io.softwareecg.awb.investigation.model.QuerySpec;
import io.softwareecg.awb.queryengine.model.TimeSeriesQuery;
import io.softwareecg.awb.queryengine.model.TimeSeriesQueryResponse;
import io.softwareecg.awb.queryengine.pipeline.SeriesTransformPipeline;
import io.softwareecg.awb.timeseries.port.TimeSeriesRepository;
import io.softwareecg.awb.timeseries.port.RepositoryException;
import io.softwareecg.wfx.platform.api.EventBus;
import io.softwareecg.wfx.platform.api.events.ProgressEvent;
import io.softwareecg.awb.runtime.logging.EkgLogger;
import io.softwareecg.awb.runtime.discovery.ServiceDiscovery;
import io.softwareecg.awb.timeseries.TimeSeries;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.apache.commons.lang3.time.StopWatch;
import org.slf4j.Logger;

import java.text.NumberFormat;
import java.util.*;

import static io.softwareecg.awb.investigation.model.SeriesCombineMode.CONCAT;

import io.softwareecg.awb.queryengine.ComputedTimeSeriesResponse;
import io.softwareecg.awb.queryengine.MetricQueryService;
/**
 * The default implementation of MetricsQueryService that will fetch the time series
 * data from types and do post processing on it like smoothing and vectorization.
 *
 * @since 02.06.2018
 */
@Singleton
@SuppressWarnings("unused") // used via DI / reflection
public class QueryRunner implements MetricQueryService {

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
    private final SeriesTransformPipeline transformPipeline;

    @Inject
    public QueryRunner(@io.avaje.inject.External EventBus<EventObject> eventBus,
                       SeriesTransformPipeline transformPipeline) {
        this.eventBus = eventBus;
        this.transformPipeline = transformPipeline;
    }


    @Override
    public void initializeService(TimeSeriesRepository client) {
        this.metricDataAccess = ServiceDiscovery.lookup(MetricDataAccessService.class, client);
    }


    //================================================================================================================
    //  MetricsQueryService API
    //================================================================================================================

    /* (non-Javadoc)
     * @see getComputedTimeSeries#getComputedTimeSeries(...)
     */
    public ComputedTimeSeriesResponse getComputedTimeSeries(QuerySpec filterParams,
                                                            ComputationSpec computeParams, int maxMetricLimit) {

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
            result = transformPipeline.keepValueChanges(result);
        }

        // add values of all time series to a single one if requested
        result = transformPipeline.combine(query.getQueryParams().getMetricName(), result,
                computeParams.getSeriesCombineMode());

        eventBus.publish(new ProgressEvent("Smoothing time series...", 0.5, this));

        // flatten the value vector to make it nicer to view at UI if requested
        long tSmoothStart = System.currentTimeMillis();
        transformPipeline.smooth(result, computeParams.getSeriesSmoothingGranularity(),
                computeParams.getSeriesSmoothingType());
        LOGGER.info("Smoothing took {}ms", System.currentTimeMillis() - tSmoothStart);

        eventBus.publish(new ProgressEvent("Simplifying time series...", 0.65, this));

        // reduce the data points per individual time series using vectorization algorithm
        result = transformPipeline.simplify(result, computeParams.getThreshold());

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

}

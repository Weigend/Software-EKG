package io.softwareecg.collector.repository.rdb.api.datamanagement;

import io.softwareecg.collector.common.LoggerFactory;
import io.softwareecg.collector.dt.TimeSeriesHeader;
import io.softwareecg.collector.repository.rdb.api.RdbProperties;
import io.softwareecg.collector.repository.rdb.api.RdbRepositoryClient;
import io.softwareecg.collector.repository.rdb.api.RdbRepositoryException;
import io.softwareecg.collector.repository.rdb.api.TimeSeries;
import io.softwareecg.collector.repository.rdb.api.datamanagement.compact.CompactInterval;
import io.softwareecg.collector.repository.rdb.api.datamanagement.compact.CompactIntervalStrategy;
import io.softwareecg.collector.repository.rdb.api.datamanagement.compact.CompactIntervalStrategyFactory;
import io.softwareecg.collector.repository.rdb.api.querymodel.delete.DeleteParams;
import io.softwareecg.collector.repository.rdb.api.querymodel.expr.ExactFilterExpression;
import io.softwareecg.collector.repository.rdb.api.querymodel.expr.ExprFactory;
import io.softwareecg.collector.repository.rdb.api.querymodel.expr.Expression;
import io.softwareecg.collector.repository.rdb.api.querymodel.search.SearchParams;
import io.softwareecg.collector.repository.rdb.api.querymodel.search.SearchResult;
import io.softwareecg.collector.repository.rdb.api.querymodel.search.SortField;
import io.softwareecg.collector.repository.rdb.api.schema.EkgSchemaField;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * The TimeSeriesManager class is responsible to manage three tasks that will commit and compact and delete time series.
 * In contrast to other places there this can be done, this manager provides to benefits. It ensures that
 * that both tasks will be executed asynchronously and not redundant (in contrast to a commit inside
 * multiple collector jobs).
 * For this the TimeSeriesManager use it's own Thread that with time based scheduling.
 *
 * @since 02.11.2018
 */
public class TimeSeriesManager {

    /**
     * A constant that defines the time in milliseconds that will
     * be the minimum time interval between two commits.
     */
    private static final long MIN_COMMIT_INTERVAL_IN_MS = RdbProperties.getMaxIntervalInMsWithoutCommit();

    /**
     * The logger instance that will use to log error
     * or debug/tracing events.
     */
    private static final Logger LOGGER = LoggerFactory.create();

    /**
     * The singleton instance of this class that has the context
     * for the whole collector application.
     */
    private static final TimeSeriesManager instance = new TimeSeriesManager();

    /**
     * An executor with a single thread that will asynchronously execute
     * the compression and commit tasks with less impact to the collectors jobs.
     */
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    /**
     * A boolean flag that defines if the daily compaction of the time series data
     * in the repository should proceed or not.
     */
    private final boolean dailyCompactIsActive = RdbProperties.dailyCompactTimeSeries();

    /**
     * The maximum age in milli seconds for time series data in the repository.
     * Older data should be deleted by the TimeSeriesManager periodically.
     */
    private final long maxDateAgeInMillis = RdbProperties.getDataMaxAgeInMillis();

    /**
     * A boolean flag that defines if the collector should use explicit commit commands after
     * flush a chunk of time series data or if Autocommit features of the database should be used instead.
     */
    private final boolean useExplicitCommit = RdbProperties.useExplicitCommit();

    /**
     * A set of all SeriesFilter beans that contains a set of filter expressions. Each SeriesFilter represents
     * a single metric and all it's meta data like the host or cloud namespace.
     * In contrast to the unregistered filters this set stores filter for metrics/time series that are still
     * updated by the running collector jobs.
     */
    private final Set<SeriesFilter> registeredSeriesFilters = new HashSet<>();

    /**
     * A set of all SeriesFilter beans that contains a set of filter expressions. Each SeriesFilter represents
     * a single metric and all it's meta data like the host or cloud namespace.
     * In contrast to the registered filters this set stores filter for metrics/time series that are no longer
     * maintained by the collector jobs. But independent of that the series may exists as multiple records in
     * the database and need also to compact.
     */
    private final Set<SeriesFilter> unregisteredSeriesFilters = new HashSet<>();

    /**
     * A lock that will used to ensure that only one thread updates the
     * registeredSeriesFilters / unregisteredSeriesFilters data structure at time to prevent inconsistency.
     */
    private static final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    /**
     * The remote repository client (for Solr or ElasticSearch) that will use to
     * write and query data to and from the repository.
     */
    private RdbRepositoryClient client;

    /**
     * A boolean flag that will set to true than a TimeSeriesManager client requests an explicit commit
     * for the time series data written before.
     * If this flag is true the async worker will commit the data if necessary.
     */
    protected boolean commitRequested = false;

    /**
     * A timestamp that defines the date/time than the last commit was executed by this
     * TimeSeresManager instance.
     */
    protected long lastCommitTime = 0;

    /**
     * The EKG project the collector uses for it's work with remote repositories
     * (defines the context each time series belongs to)
     */
    protected String ekgProjectName;

    /**
     * An service that computes a time interval (slice) this TimeSeriesManager uses as bounds
     * to identify a groups of time series records that should be compact (merged) into a single instance.
     * This will do afterwards to optimize the amount of records in the EKG repository that adding
     * records frequently.
     */
    protected CompactIntervalStrategy compactIntervalStrategy = CompactIntervalStrategyFactory.getCompactStrategy();

    //=================================================================================================================
    // constructor
    //=================================================================================================================

    /**
     * Constructs a new instance of TimeSeriesManager and sets the
     * execution time for next series compression to 2:00AM of the following day.
     */
    private TimeSeriesManager() {
        setupSeriesDataWorker();
    }


    //=================================================================================================================
    // TimeSeriesManager public API
    //=================================================================================================================

    /**
     * Returns the singleton instance of the TimeSeriesManager
     * that is responsible for the whole collector application.
     *
     * @return the TimeSeriesManager singleton instance
     */
    public static TimeSeriesManager getInstance() {
        return instance;
    }

    /**
     * Register/set the remote repository client the singleton
     * TimeSeriesManager will use to do it's job.
     * Only one type of client at the time can be used.
     *
     * @param client an RdbRepositoryClient instance that should use by the manager
     */
    public void workWithClient(RdbRepositoryClient client) {
        this.client = client;
    }

    /**
     * Requests an commit that will be executed asynchronously if hard commits
     * are enabled in the configuration of this collector instance.
     *
     * This method can called by multiple threads (collector jobs) but will result in a
     * single commit controlled with it's exact execution time by the TimeSeriesManager.
     */
    public void requestCommit() {
        if (useExplicitCommit) {
            LOGGER.info("Retrieve an commit command for the remote repository.");
            commitRequested = true;
        }
    }

    /**
     * Register an MeasurementContext instance with one or more time series
     * that should managed by this TimeSeriesManager instance.
     *
     * @param measurementContext a measurement context that describes one or more metrics that will measured
     */
    public void registerMeasurementContext(MeasurementContext measurementContext) {
        if (!dailyCompactIsActive) {
            return;
        }

        this.ekgProjectName = measurementContext.getHeader().getEkgProjectName();

        SeriesFilter filter = new SeriesFilter(measurementContext.getMeasurement(), measurementContext.getHeader());

        try {
            lock.writeLock().lock();

            for (String metric : measurementContext.getMetricLabels()) {
                if (StringUtils.isBlank(metric)) {
                    continue;
                }

                registeredSeriesFilters.add(filter.spawnWithMetric(metric));
            }

        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Marks registered time series for concrete metrics as as no longer maintained.
     * This will cause that the manager will compact the time series for one last time (at next morning)
     * and than remove it from the managed series.
     *
     * @param measurementContext a measurement context that describes one or more metrics that will measured
     */
    public void deregisterMeasurementContext(MeasurementContext measurementContext) {
        if (!dailyCompactIsActive) {
            return;
        }

        SeriesFilter filter = new SeriesFilter(measurementContext.getMeasurement(), measurementContext.getHeader());

        try {
            lock.writeLock().lock();

            for (String metric : measurementContext.getMetricLabels()) {
                if (StringUtils.isBlank(metric)) {
                    continue;
                }

                SeriesFilter lookupFilter = filter.spawnWithMetric(metric);

                if (!registeredSeriesFilters.contains(lookupFilter)) {
                    return;
                }

                registeredSeriesFilters.remove(lookupFilter);
                unregisteredSeriesFilters.add(lookupFilter);
            }

        } finally {
            lock.writeLock().unlock();
        }
    }

    //=================================================================================================================
    // internal business logic to manage the time series in the repository
    //=================================================================================================================

    /**
     * Compact / merge all series of a same group (same metric and dimensions) of a compute interval to single (long) series.
     *
     * @param compactInterval a CompactInterval bean that defines the last compute time interval
     *                       (not the current one) with series to compact
     * @throws RdbRepositoryException thrown than an error occur than trying to fetch series data from the repository
     */
    private void compactAllSeries(CompactInterval compactInterval) throws RdbRepositoryException {

        Date rangeBegin = compactInterval.getRangeBeginDatetime();
        Date rangeEnd = compactInterval.getRangeEndDatetime();

        Expression startInsideRangeExp = ExprFactory.dateTimeRangeFilter(EkgSchemaField.TS_START, rangeBegin, rangeEnd);
        Expression stopInsideRangeExp = ExprFactory.dateTimeRangeFilter(EkgSchemaField.TS_STOP, rangeBegin, rangeEnd);

        Expression seriesIntersectsIntervalExp = ExprFactory.or(
                startInsideRangeExp,
                stopInsideRangeExp,
                ExprFactory.and(
                        ExprFactory.dateTimeRangeFilter(EkgSchemaField.TS_START, null, rangeBegin),
                        ExprFactory.dateTimeRangeFilter(EkgSchemaField.TS_STOP, rangeEnd, null)
                ));

        Set<SeriesFilter> filterToProceed;

        // ------ START critical path -------
        lock.writeLock().lock();

        try {
            // search for all time series that are registered before the previous "single-doc-storage-interval" and
            // can merged/compact now.
            filterToProceed = lookupSeriesToCompact(rangeEnd.getTime());

        } finally {
            lock.writeLock().unlock();
        }
        // ------ END critical path -------

        List<TimeSeries> mergedSeriesToPersist = new ArrayList<>();

        Set<String> docGroupKeysToDelete = new HashSet<>();

        // now compact all series that are stored yesterday in the repository by this collector instance
        for (SeriesFilter seriesFilter : filterToProceed) {
            CompactResult cres = compactSingleSeries(seriesFilter, seriesIntersectsIntervalExp);

            if (cres == null) {
                continue;
            }

            mergedSeriesToPersist.add(cres.mergedSeries);
            docGroupKeysToDelete.addAll(cres.docIdsToDelete);
        }

        client.add(mergedSeriesToPersist);
        deleteOldSeries(docGroupKeysToDelete, seriesIntersectsIntervalExp);
    }

    /**
     * Compact a single time series that means all database records of a time series that
     * matches to the given filter will be loaded and merged together to a single one.
     *
     * @param seriesFilter a SeriesFilter bean that provides filter expressions for the time series meta data
     * @param seriesIntersectsIntervalExp an complex filter expression used to find series records that intersects
     *                                    with the time slice defined by the CompactIntervalStrategy
     */
    private CompactResult compactSingleSeries(SeriesFilter seriesFilter, Expression seriesIntersectsIntervalExp) {

        SearchParams searchParams = new SearchParams();
        searchParams.withFilterQueries(
                seriesIntersectsIntervalExp, // only records that intersect with the defined time slice
                ExprFactory.exactFilter(EkgSchemaField.TS_IS_MERGED, false), // only non compact records
                ExprFactory.exactFilter(EkgSchemaField.PROJECT_NAME, ekgProjectName), // only records of this project
                ExprFactory.exactFilter(EkgSchemaField.TS_GROUP_KEY_HASH, seriesFilter.groupKeyHash) // only records of the same metric and metric-metadata
        );

        searchParams.withSortField(EkgSchemaField.TS_START, SortField.SortMode.ASC);

        try {
            // find records to compact
            SearchResult<TimeSeries> timeSeriesSearchResult = client.search(TimeSeries.class, searchParams);

            if (timeSeriesSearchResult.queryStatus().getNumberOfHits() == 0) {
                LOGGER.info("Unable to compact the series for SeriesFilter {} because no one could resolved.", seriesFilter);
                return null;
            }

            List<TimeSeries> seriesList = timeSeriesSearchResult.rows();

            // the first one will be the master. All others will merged with this one
            TimeSeries mergedSeries = seriesList.get(0);

            // data structure we use to collect the none compact records we will delete later after persist the merged one
            CompactResult compactResult = new CompactResult(mergedSeries);
            compactResult.docIdsToDelete.add(String.valueOf(seriesList.get(0).getGroupKeyHash()));

            // merge all records with the master
            for (int index = 1; index < seriesList.size(); index++) {
                compactResult.docIdsToDelete.add(String.valueOf(seriesList.get(index).getGroupKeyHash()));
                mergedSeries.setData(TimeSeriesValueHelper.mergeData(mergedSeries, seriesList.get(index)));
            }

            // the modified record gets a new id and updated time/size attributes
            mergedSeries.setId(UUID.randomUUID().toString());
            mergedSeries.setEnd(seriesList.get(seriesList.size() - 1).getEnd());
            mergedSeries.setImportDatetime(new Date());
            mergedSeries.setMerged(true);

            return compactResult;

        } catch (RdbRepositoryException e) {
            LOGGER.error("", e);
            throw new IllegalStateException(e);
        }
    }

    /**
     * Deletes all 'none compacted' time series records in the specified time slice from the EKG repository
     * that are specified by the collection of groupHashKeys that are collected before at the merge process.
     * The filter on the time slice is necessary to prevent the deletion of records of similar group written
     * before or after the slice. This records are out of scope of the optimizer process.
     *
     * @param docGroupKeysToDelete a collection of key's that identify time series records we want to delete
     * @param seriesIntersectsIntervalExp an expression that can use to identify records that intersect with the
     *                                    time interval that that is the scope of the current optimize run
     *
     * @throws RdbRepositoryException thrown than an error occurs during the deletion query
     */
    private void deleteOldSeries(Collection<String> docGroupKeysToDelete, Expression seriesIntersectsIntervalExp)
            throws RdbRepositoryException {

        if (docGroupKeysToDelete.isEmpty()) {
            return;
        }

        Expression mergedFlagFilter = ExprFactory.exactFilter(EkgSchemaField.TS_IS_MERGED, false);
        Expression keyHashFilters = ExprFactory.or(docGroupKeysToDelete.stream()
                                            .map(key -> ExprFactory.exactFilter(EkgSchemaField.TS_GROUP_KEY_HASH, key))
                                            .toArray(Expression[]::new));

        DeleteParams deleteParams = new DeleteParams();
        deleteParams.addFilter(mergedFlagFilter);
        deleteParams.addFilter(keyHashFilters);
        deleteParams.addFilter(seriesIntersectsIntervalExp);
        deleteParams.addFilter(ExprFactory.exactFilter(EkgSchemaField.PROJECT_NAME, ekgProjectName));

        client.delete(deleteParams);
    }

    /**
     * Searches for all {@link SeriesFilter} definitions that store filter expressions for
     * time series that were registered in the previous compute time interval that persists
     * frequently without merging/compact it.
     * Also not compacted series that are older but not compact will be caught.
     *
     * The search filter automatically removes matching SeriesFilter beans from the unregistered
     * set of SeriesFilters.
     *
     * @param intervalChangeTimestamp the timestamp that the last compute interval changes to the new one
     * @return a set of SeriesFilter bean with metadata filter expression for all time series that should be compact
     */
    private Set<SeriesFilter> lookupSeriesToCompact(long intervalChangeTimestamp) {
        Set<SeriesFilter> filterToProceed;
        filterToProceed = new HashSet<>();

        // proceed filters expression that were registered in the last compute time interval but that will now longer updated
        for (SeriesFilter seriesFilter : unregisteredSeriesFilters) {
            if (registeredSeriesFilters.contains(seriesFilter)
                    || seriesFilter.seriesFilterCreationTime > intervalChangeTimestamp) {
                continue;
            }

            filterToProceed.add(seriesFilter);
        }

        // remove that filters from the storage
        for (SeriesFilter seriesFilter : filterToProceed) {
            unregisteredSeriesFilters.remove(seriesFilter);
        }

        // filters expression that were registered  last compute time interval and are still in use
        filterToProceed.addAll(registeredSeriesFilters);

        // return the list of all metrics that should be compact
        return filterToProceed;
    }

    /**
     * Sends a commit request to the EKG repository that will result in
     * a hard commit that makes all changes visible
     */
    private void commitData() {

        if (lastCommitTime + MIN_COMMIT_INTERVAL_IN_MS > System.currentTimeMillis()) {
            return;
        }

        LOGGER.info("Send commit command to remote database.");
        try {
            client.commit();
            lastCommitTime = System.currentTimeMillis();
            commitRequested = false;
        } catch (RdbRepositoryException e) {
            LOGGER.error("Unable to commit the written data as result of an exception in the persistence layer.", e);
        }
    }

    private void setupSeriesDataWorker() {

        executorService.submit(() -> {

            while (!Thread.currentThread().isInterrupted()) {
                try {

                    if (System.currentTimeMillis() > compactIntervalStrategy.getNextExecutionTime()) {
                        CompactInterval interval = compactIntervalStrategy.getTimeIntervalToCompact();
                        LOGGER.info("Compact time series for all known metrics for the interval [{} to {}] defined by the " +
                                        "CompactIntervalStrategy '{}'",
                                interval.getRangeBeginDatetime(), interval.getRangeEndDatetime(), compactIntervalStrategy);
                        compactAllSeries(interval);

                        LOGGER.info("Compact job for all time series completed. Next execution time will be at {}",
                                new Date(compactIntervalStrategy.getNextExecutionTime()));
                    }

                    if (commitRequested) {
                        commitData();
                    }

                    Thread.sleep(1000);

                } catch (Throwable e) {
                    if (e instanceof InterruptedException) {
                        LOGGER.info("SeriesDataWorker is interrupted and will no long compress or commit time series data!");
                        return;
                    }

                    LOGGER.error("", e);
                }
            }
        });
    }

    //=================================================================================================================
    // anonymous class that will use intern as data structure
    //=================================================================================================================

    private class SeriesFilter {

        private Set<ExactFilterExpression> seriesMetaFilterExpressions = new HashSet<>();

        private TimeSeriesHeader timeSeriesHeader;

        private final String headerKey;

        private String groupKeyHash;

        private String measurement;

        private String singleMetricName;

        private final long seriesFilterCreationTime = System.currentTimeMillis();

        //=============================================================================================================
        // two constructors that will used for green field instance creation and to copy/spawn instances
        //=============================================================================================================

        private SeriesFilter(String measurement, TimeSeriesHeader header) {
            this.timeSeriesHeader = header;
            this.measurement = measurement;

            addExpressionIfNotNull(EkgSchemaField.PROJECT_NAME, header.getEkgProjectName());

            // logical attributes
            addExpressionIfNotNull(EkgSchemaField.TS_NAMESPACE_NAME, header.getNamespace());
            addExpressionIfNotNull(EkgSchemaField.TS_SERVICE_NAME, header.getService());
            addExpressionIfNotNull(EkgSchemaField.TS_POD_NAME, header.getPodName());
            addExpressionIfNotNull(EkgSchemaField.TS_CONTAINER_NAME, header.getContainerId());

            // physical attributes
            addExpressionIfNotNull(EkgSchemaField.TS_HOST_GROUP_NAME, header.getHostGroupId());
            addExpressionIfNotNull(EkgSchemaField.TS_HOST_NAME, header.getHostName());

            // common metric attributes
            addExpressionIfNotNull(EkgSchemaField.TS_PROCESS_GROUP_NAME, header.getProcessGroupId());
            addExpressionIfNotNull(EkgSchemaField.TS_PROCESS_NAME, header.getProcessLabel());
            addExpressionIfNotNull(EkgSchemaField.TS_METRIC_GROUP, header.getMetricGroupId());

            headerKey = header.getCombinedKey();
        }

        private SeriesFilter(Set<ExactFilterExpression> seriesMetaFilterExpressions, String headerKey) {
            this.seriesMetaFilterExpressions = seriesMetaFilterExpressions;
            this.headerKey = headerKey;
        }

        //=============================================================================================================
        //  API of the SeriesFilter bean
        //=============================================================================================================

        public SeriesFilter spawnWithMetric(String metric) {
            SeriesFilter spawnedFilter = new SeriesFilter(seriesMetaFilterExpressions, headerKey);
            spawnedFilter.timeSeriesHeader = timeSeriesHeader;
            spawnedFilter.measurement = measurement;
            spawnedFilter.singleMetricName = metric;
            spawnedFilter.groupKeyHash = String.valueOf(TimeSeries.calculateGroupHashKey(
                    timeSeriesHeader.getEkgProjectName(),
                    timeSeriesHeader.getHostGroupId(),
                    timeSeriesHeader.getHostName(),
                    timeSeriesHeader.getNamespace(),
                    timeSeriesHeader.getService(),
                    timeSeriesHeader.getPodName(),
                    timeSeriesHeader.getContainerId(),
                    timeSeriesHeader.getProcessGroupId(),
                    timeSeriesHeader.getProcessLabel(),
                    "",
                    timeSeriesHeader.getMetricGroupId(),
                    metric
            ));

            return spawnedFilter;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            SeriesFilter that = (SeriesFilter) o;

            if (!Objects.equals(headerKey, that.headerKey)) return false;
            return Objects.equals(singleMetricName, that.singleMetricName);
        }

        @Override
        public int hashCode() {
            int result = 31 * (headerKey != null ? headerKey.hashCode() : 0);
            result = 31 * result + (singleMetricName != null ? singleMetricName.hashCode() : 0);
            return result;
        }

        //=============================================================================================================
        // internal helper
        //=============================================================================================================

        private void addExpressionIfNotNull(EkgSchemaField field, String filterValue) {

            if (StringUtils.isBlank(filterValue)) {
                return;
            }

            seriesMetaFilterExpressions.add(new ExactFilterExpression(field, List.of(filterValue)));
        }
    }


    private class CompactResult {

        private final TimeSeries mergedSeries;

        private final Set<String> docIdsToDelete = new HashSet<>();

        public CompactResult(TimeSeries series) {
            this.mergedSeries = series;
        }
    }
}

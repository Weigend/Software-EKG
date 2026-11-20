package io.softwareecg.collector.repository.rdb.api.datamanagement;

import io.softwareecg.collector.common.LoggerFactory;
import io.softwareecg.collector.common.Validate;
import io.softwareecg.collector.dt.TimeSeriesHeader;
import io.softwareecg.collector.repository.StaticTimeSeriesRepository;
import io.softwareecg.collector.repository.rdb.api.*;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;

import java.time.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * The RemoteRepository is a proxy class that implements the required business logic of the
 * {@link StaticTimeSeriesRepository} and {@link FlexTimeSeriesStorage} interface on top of the
 * RdbRepositoryClient low-level API. This class also implements some more functionality like
 * clean-up task for the time series in the repository.
 * This repository is based on a RdbRepositoryClient that will used as interface to the physical
 * remote repository like Apache Solr or ElasticSearch.
 *
 * @since 30.10.2018
 */
public class RemoteRepository implements StaticTimeSeriesRepository, FlexTimeSeriesStorage {

    private static final long ONE_DAY_IN_MILLIS = 24 * 60 * 60 * 1000;

    /**
     * The logger instance that will use to log error
     * or debug/tracing events.
     */
    private static final Logger LOGGER = LoggerFactory.create();

    /**
     * The remote repository client (for Solr or ElasticSearch) that will use to
     * write and query data to and from the repository.
     */
    private final RdbRepositoryClient client;

    /**
     * The time in milliseconds after the IN-memory data should flushed
     * to the remote database.
     */
    private final long commitIntervalInMillis;

    /**
     * The timestamp that represents the time than the next commit
     * of the written data should be executed.
     */
    private long nextCommitTimestamp;

    /**
     * The amount of update events (value updates of the time series) that
     * can proceed without writing the time series data to the repository.
     */
    private final int maxUpdateEventsWithoutCommit;

    /**
     * The name of the repository. This will be the collection or core at
     * Apache Solr or the index name at ElasticSearch
     */
    private final String repositoryName;

    /**
     * A container that store the meta data of the time series that will
     * written to this repository than the {@link StaticTimeSeriesRepository} API is in use.
     */
    private MeasurementContext measurementContext = null;

    /**
     * A map that store the all time series that are written to this repository
     * and can be updated in the further lifecycle.
     * The key of the map is combination of all series filter labels and the metric name itself.
     */
    private final Map<String, TimeSeries> timeSeriesMap = new HashMap<>();

    /**
     * A simple thread safe counter that will be used to count the update events
     * for all time series together.
     */
    private final AtomicInteger amountUpdateEvents = new AtomicInteger(0);

    /**
     * The timestamp of the midnight from the current to the next day
     * that will be a moment where all stored in-memory data will be flushed.
     */
    private long timestampBeginOfNextDay;

    //================================================================================================================
    // constructor
    //================================================================================================================

    /**
     * Constructs a new instance of RemoteRepository pre-configured with a database client and some
     * settings that will control the commit behaviour.
     *
     * @param repositoryName the name of the repository (collection / index name) the data should written to
     * @param client the remote repository client that acts as proxy interface to the remote database
     * @param commitIntervalInMs The time in milliseconds after the in-memory data should flushed to the remote database.
     * @param maxUpdateEventsWithoutCommit the amount of updates that can proceed without writing the data thought the db
     */
    public RemoteRepository(String repositoryName, RdbRepositoryClient client, long commitIntervalInMs,
                            int maxUpdateEventsWithoutCommit) {
        this.client = client;
        this.commitIntervalInMillis = commitIntervalInMs;
        this.maxUpdateEventsWithoutCommit = maxUpdateEventsWithoutCommit;
        this.repositoryName = repositoryName;
        this.nextCommitTimestamp = System.currentTimeMillis() + commitIntervalInMs;

        LocalTime midnight = LocalTime.MIDNIGHT;
        LocalDate today = LocalDate.now(ZoneId.of("UTC"));
        timestampBeginOfNextDay = LocalDateTime.of(today, midnight).toInstant(ZoneOffset.UTC).toEpochMilli();
    }

    //================================================================================================================
    // API of the RemoteRepository class
    //================================================================================================================

    public void ensureRepositoryExists(RdbRepositoryAdmin repositoryAdmin) {

        if (StringUtils.isBlank(repositoryName)) {
            LOGGER.warn("Unable to create Solr client as result of missing configuration value 'repositoryName'.");
        }

        try {
            boolean collectionExists = repositoryAdmin.listRepositories()
                    .stream().anyMatch(props -> repositoryName.equals(props.getCollectionName()));

            if (collectionExists) {
                return;
            }

            if (!repositoryAdmin.createEkgRepository(repositoryName)) {
                LOGGER.error("The creation of a new remote repository index failed.");
            }

        } catch (RdbRepositoryException e) {
            throw new IllegalStateException("Unable to create repository '" + repositoryName
                    + "' in remote database as result of an RdbException.", e);
        }
    }

    @Override
    public void createMeasurement(TimeSeriesHeader timeSeriesHeader, String[] metricLabels) {

        if (measurementContext != null) {
            flush();
        }

        measurementContext = new MeasurementContext(timeSeriesHeader, metricLabels);
        TimeSeriesManager.getInstance().registerMeasurementContext(measurementContext);
    }

    @Override
    public void addValueSetToMeasurement(Date captureTime, Double[] metricValues) {

        if (Validate.nullCheckArgs(captureTime, metricValues)) {
            LOGGER.warn("The given arguments 'Date: {} and Double[]: {}' contains a null value.", captureTime, metricValues);
            return;
        }

        if (measurementContext == null) {
            throw new IllegalStateException("Retrieve measurement values, but no measurement context was registered before!");
        }

        if (metricValues.length != measurementContext.getMetricLabels().length) {
            throw new IllegalArgumentException("Retrieve " + metricValues.length + " metric values, but the registered " +
                    "metric label has the amount of " + measurementContext.getMetricLabels().length);
        }

        if (dayHasChanged()) {
            addAllTimeSeriesToRemoteDB();
            nextCommitTimestamp = System.currentTimeMillis() + commitIntervalInMillis;
            timestampBeginOfNextDay += ONE_DAY_IN_MILLIS;
            amountUpdateEvents.set(0);
        } else {
            amountUpdateEvents.incrementAndGet();
        }

        long timestamp = captureTime.getTime();
        for (int index = 0; index < metricValues.length; index++) {
            String metricLabel = measurementContext.getMetricLabels()[index];
            String seriesKey = measurementContext.getHeader().getCombinedKey() + metricLabel;

            TimeSeries timeSeries = timeSeriesMap.getOrDefault(seriesKey,
                    new TimeSeries(measurementContext.getMeasurement(), measurementContext.getHeader(), metricLabel));
            double metricValue = metricValues[index] != null ? metricValues[index] : -1;

            // only add empty values to time series that already exists
            if (!timeSeriesMap.containsKey(seriesKey) &&  metricValue == -1) {
                LOGGER.debug("do not add empty value to non-existing time series '{}'", metricLabel);
            } else {
                if (timeSeries.getData().length == 0) {
                    timeSeries.setStart(captureTime);
                }

                timeSeries.setEnd(captureTime);

                timeSeries.setData(TimeSeriesValueHelper.addToByteArray(timeSeries.getData(), timestamp, metricValue));
                timeSeriesMap.put(seriesKey, timeSeries);
            }
        }

        if (timeSeriesMap.size() > 0) {
            commitIfNecessary();
        }
    }

    @Override
    public void flush() {
        addAllTimeSeriesToRemoteDB();
    }

    @Override
    public void addTimeSeries(TimeSeries timeSeries) {
        amountUpdateEvents.incrementAndGet();
    }

    @Override
    public void cleanUp() {
        TimeSeriesManager.getInstance().deregisterMeasurementContext(measurementContext);
    }

    //=================================================================================================================
    //
    //=================================================================================================================

    private boolean dayHasChanged() {
        return System.currentTimeMillis() > timestampBeginOfNextDay;
    }

    /**
     * Add all time series to SoLR if the commit time interval is reached and then commit
     * if Solr/ElasticSearch has new data
     */
    private void commitIfNecessary() {
        if (timeIntervalIsReached() || maxAmountOfSeriesUpdateEventsReached()) {
            addAllTimeSeriesToRemoteDB();
        }
    }

    private boolean maxAmountOfSeriesUpdateEventsReached() {
        if (amountUpdateEvents.get() >= maxUpdateEventsWithoutCommit) {
            amountUpdateEvents.set(0);
            return true;
        }

        return false;
    }

    /**
     * @return true if current time is greater than nextCommitDate, otherwise false
     */
    private boolean timeIntervalIsReached() {
        long current = System.currentTimeMillis();
        if (current >= nextCommitTimestamp) {
            nextCommitTimestamp = System.currentTimeMillis() + commitIntervalInMillis;
            LOGGER.debug("A timed commit is required. Next commit time will be at '{}'", new Date(nextCommitTimestamp));
            return true;
        }

        return false;
    }

    /**
     * Adds all time series with a min size greater zero to the
     * remote database like Solr or ElasticSearch.
     */
    private void addAllTimeSeriesToRemoteDB() {

        Collection<String> toDelete = new ArrayList<>(timeSeriesMap.size());
        Collection<TimeSeries> seriesToPersist = new ArrayList<>();

        // Update list of time series that should be deleted from the storage map. Time
        // series that have not yet been added to Solr are added and then deleted if
        // the "add" operation has been successful.
        for (Map.Entry<String, TimeSeries> entry : timeSeriesMap.entrySet()) {
            if (entry.getValue() == null || entry.getValue().getData().length == 0) {
                LOGGER.warn("empty values in timeseries for key '" + entry.getKey() + "' - deleting without storing!");
                toDelete.add(entry.getKey());

            } else {
                seriesToPersist.add(entry.getValue());
                toDelete.add(entry.getKey());
            }
        }

        if (seriesToPersist.size() == 0) {
            return;
        }

        try {
            client.add(seriesToPersist);

            TimeSeriesManager.getInstance().requestCommit();

            removeTimeSeriesFromMap(toDelete);

        } catch (RdbRepositoryException e) {
            LOGGER.error("Error occurred trying to add time series to remote database.", e);
        }
    }

    /**
     * Removes multiple time series with the given keys from the map
     *
     * @param keys - the keys of the time series
     */
    private void removeTimeSeriesFromMap(Collection<String> keys) {
        LOGGER.debug("Removing time series '{}' from map.", StringUtils.join(keys, ","));
        for (String key : keys) {
            timeSeriesMap.remove(key);
        }
    }

}

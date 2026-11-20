//______________________________________________________________________________
//
//          Project:    Software EKG
//______________________________________________________________________________
//
//         Author:      Weigend AM 2009 - 2014
//______________________________________________________________________________
//
// Notice: This piece of software was created, designed and implemented by
// experienced craftsmen and innovators in Munich, Germany.
// Changes should be done with respect to the original design.
//______________________________________________________________________________
package io.softwareecg.collector.repository.rdb.api;

import io.softwareecg.collector.common.CollectorProperties;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.TimeZone;

/**
 *
 */
public final class RdbProperties extends CollectorProperties {

    /**
     * Our instance.
     */
    @SuppressWarnings("MismatchedQueryAndUpdateOfCollection") // updated via private constructor
    private static final RdbProperties PROPERTIES = new RdbProperties();

    private static final String COLLECTOR_PROPERTY_PATH_KEY = "collectorRepositoryPropertiesPath";

    private static final String DEFAULT_PROPERTY_NAME = "remote-repository-config.properties";

    /**
     * Private constructor - we should move that to an abstract class
     */
    private RdbProperties() {
        super(COLLECTOR_PROPERTY_PATH_KEY, DEFAULT_PROPERTY_NAME);
    }

    //=================================================================================================================
    // connection properties
    //=================================================================================================================

    /**
     * A boolean flag that defines if the daily compaction of the time series data
     * in the repository should proceed or not.
     * If true the collector application will merge all time series that belong together
     * (have the same metadata except the start/end time range and measurement value) and written by this collector
     * instance together to a single one.
     *
     * @return a boolean flag that defines if the daily compaction of the time series data should be proceed
     */
    public static boolean dailyCompactTimeSeries() {
        return Boolean.valueOf(PROPERTIES.getProperty("io.softwareecg.collector.repository.rdb.compactTimeSeries", "true"));
    }

    public static String getCompactIntervalStrategy() {
        return PROPERTIES.getProperty("io.softwareecg.collector.repository.rdb.compactIntervalStrategy", "DailyCompactStrategy");
    }

    /**
     * Returns a boolean flag that defines if the repository (index/collection) the the remote database like
     * Apache Solr should created if not already exists.
     *
     * @return a boolean flag that defines if not existing repositories should create at runtime.
     */
    public static boolean autoCreateNoneExistingRepository() {
        return Boolean.valueOf(PROPERTIES.getProperty("io.softwareecg.collector.repository.rdb.autoCreateNoExistingRepository", "false"));
    }

    /**
     * Returns the name of the repository (ElasticSearch index / Solr Collection / Solr core) that have to use
     * by the collector to store the time series data. This can also be the alias (used at Solr).
     *
     * @return the name of the time series repository
     */
    public static String getRepositoryName() {
        return PROPERTIES.getProperty("io.softwareecg.collector.repository.rdb.repositoryName", "ekgdata");
    }


    /**
     * Returns a boolean flag that defines if the remote database should use via cluster mode or not.
     * For example using Solr this will cause that the repository client will use Zookeeper to resolve an address
     * of a Solr Cloud node to use instead using directly a specified Solr instance.
     *
     * @return boolean flag that defines if the remote database should used as cloud/cluster database or not
     */
    public static boolean isClusterMode() {
        return Boolean.valueOf(PROPERTIES.getProperty("io.softwareecg.collector.repository.rdb.isClusterMode", "false"));
    }

    /**
     * Returns the connection string that can used to resolve/reach the remote database cluster manager.
     * The connections string can be just hostname or hostname:port or any other that is database specific.
     * For example at Apache Solr this should be "hostname:port" and points to the Zookeeper instance
     *
     * @return the connection string to the cluster manager of the remote database
     */
    public static String getClusterManagerConnectionString() {
        return PROPERTIES.getProperty("io.softwareecg.collector.repository.rdb.clusterManager", "localhost:9881");
    }

    /**
     * Returns the connection url to the remote database. The url should point to the query service of
     * the database without the index name itself.
     * For example the url of the Apache Solr default service handler will be 'http://localhost:8983/solr'
     * and the complete url including the index 'http://localhost:8983/solr/indexName'.
     *
     * @return the remote database service handler url without the index
     */
    public static String getDatabaseUrl() {
        return PROPERTIES.getProperty("io.softwareecg.collector.repository.rdb.databaseUrl", "http://localhost:8983/solr");
    }

    /**
     * Returns the username used to authenticate the service calls to the database.
     *
     * @return the user need to authenticate at the database server
     */
    public static String getUser() {
        return PROPERTIES.getProperty("io.softwareecg.collector.repository.rdb.user", "admin");
    }

    /**
     * Returns the password used to authenticate the service calls to the database.
     *
     * @return the password need to authenticate at the database server
     */
    public static String getUserPassword() {
        return PROPERTIES.getProperty("io.softwareecg.collector.repository.rdb.password", "password");
    }


    //=================================================================================================================
    // database client behaviour
    //=================================================================================================================


    /**
     * @return the amount of events in a time series before it is committed to solr
     */
    public static int getCommitMaxUpdateEvents() {
        return Integer.parseInt(PROPERTIES.getProperty("io.softwareecg.collector.repository.rdb.commitMaxUpdateEvents", "100"));
    }

    /**
     * @return the time interval in hh:mm:ss format
     */
    public static String getStorageCommitInterval() {
        return String.valueOf(PROPERTIES.getProperty("io.softwareecg.collector.repository.rdb.commitTimeInterval", "01:00:00"));
    }

    public static long getMaxIntervalInMsWithoutCommit() {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("hh:mm:ss");
            sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
            return sdf.parse(getStorageCommitInterval()).getTime();
        } catch (ParseException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * @return Expression that determines when data is data is deleted, e.g. 1, Default is 40
     */
    public static int getDataMaxAgeInDays() {
        return Integer.parseInt(PROPERTIES.getProperty("io.softwareecg.collector.repository.rdb.dataMaxAgeInDays", "40"));
    }

    /**
     * Returns the maximum age time series before it will deleted automatically.
     * Every value less or equal zero should interpreted as infinity.
     *
     * @return the maximum age of time series data in milli seconds
     */
    public static long getDataMaxAgeInMillis() {
        return getDataMaxAgeInDays() * 1000L * 60L * 60L * 24L;
    }

    /**
     * @return true if the collector should do a hard commit, otherwise false
     */
    public static boolean useExplicitCommit() {
        return Boolean.valueOf(PROPERTIES.getProperty("io.softwareecg.collector.repository.rdb.explicitCommit", "true"));
    }

}

package io.softwareecg.collector.source.logevents;

import io.softwareecg.collector.common.CollectorProperties;


/**
 * Property value provider that serves config values for
 * the parsing of log events.
 *
 * @since 30.10.2018
 */
public class EventConfigProperties extends CollectorProperties {

    public static final EventConfigProperties PROPERTIES = new EventConfigProperties();

    private EventConfigProperties() {
        super("collectorLogEventPropertiesPath", "log-event-config.properties");
    }

    /**
     * @return the start pattern for a multi log line
     */
    public static String getLogLineStartPattern() {
        return PROPERTIES.getProperty("io.softwareecg.collector.storage.solr.logReader.LineStartPattern", "\\d{2}-\\d{2}-\\d{4}\\s\\d{2}:\\d{2}:\\d{2},\\d{3}");
    }

    /**
     * @return the registered events
     */
    public static String getSolrStorageRegisteredEvents() {
        return PROPERTIES.getProperty("io.softwareecg.collector.storage.solr.types.events", "");

    }

    /**
     * @param name the event type
     * @return the log value format for this event type, default is numeric
     */
    public static String getEventLogValueFormat(String name) {
        return throwIllegalArgumentExceptionOrValue(String.format("io.softwareecg.collector.storage.solr.types.events.%s.logRegex.ValueFormat", name));
    }

    /**
     * @param name the event type
     * @return the value type
     */
    public static String getEventLogValueType(String name) {
        return throwIllegalArgumentExceptionOrValue(String.format("io.softwareecg.collector.storage.solr.types.events.%s.logRegex.ValueType", name));
    }

    /**
     * @param name the event type
     * @return the log regex pattern
     */
    public static String getEventLogPattern(String name) {
        return throwIllegalArgumentExceptionOrValue(String.format("io.softwareecg.collector.storage.solr.types.events.%s.logRegex", name));

    }

    /**
     * @param name the event type
     * @return the log date pattern to parse the timestamp
     */
    public static String getLogDatePattern(String name) {
        return throwIllegalArgumentExceptionOrValue(String.format("io.softwareecg.collector.storage.solr.types.events.%s.logRegex.DateFormat", name));
    }

    /**
     * @param name the event type
     * @return the group of this event
     */
    public static String getEventLogGroup(String name) {
        return String.valueOf(PROPERTIES.getProperty(String.format("io.softwareecg.collector.storage.solr.types.events.%s.metricGroup", name), "Log-Events"));
    }


    /**
     * @param name the event type
     * @return the end pattern for this interval type
     */
    public static String getIntervalEndLogPattern(String name) {
        return throwIllegalArgumentExceptionOrValue(String.format("io.softwareecg.collector.storage.solr.types.intervals.%s.logRegex.end", name));
    }

    /**
     * @param name the event type
     * @return the end pattern for this interval type
     */
    public static String getIntervalStartLogPattern(String name) {
        return throwIllegalArgumentExceptionOrValue(String.format("io.softwareecg.collector.storage.solr.types.intervals.%s.logRegex.start", name));
    }

    /**
     * @param name the event type
     * @return the date pattern for the given interval type
     */
    public static String getIntervalLogDatePattern(String name) {
        return throwIllegalArgumentExceptionOrValue(String.format("io.softwareecg.collector.storage.solr.types.intervals.%s.logRegex.DateFormat", name));
    }

    /**
     * Returns a boolean flag for the given combination of host, process and counter group if the the reader
     * for this source should read multiline or single line. The default behaviour returns <code>true</code> if the property is not overridden.
     *
     * @param host         - the host
     * @param process      - the process
     * @param counterGroup - the counter group
     * @return true if multiline reader should be used, otherwise false
     */
    public static boolean getMultiLineStrategy(String host, String process, String counterGroup) {
        String key = String.format("%s.%s.%s", host, process, counterGroup);
        String propertyKey = String.format("io.softwareecg.collector.storage.solr.logReader.%s.MultiLine", key);
        return Boolean.valueOf(PROPERTIES.getProperty(propertyKey, "false"));
    }


    /**
     * Returns the key or throws an exception if the property is null
     *
     * @param key -the property key
     * @return the value
     */
    private static String throwIllegalArgumentExceptionOrValue(String key) {
        String value = PROPERTIES.getProperty(key);

        if (value == null) {
            throw new IllegalArgumentException("The property " + key + " is not defined.");
        }

        return value;
    }

}

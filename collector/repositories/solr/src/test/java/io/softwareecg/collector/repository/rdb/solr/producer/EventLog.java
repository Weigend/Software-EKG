/**
 * ______________________________________________________________________________
 *
 * Project: Software EKG
 * ______________________________________________________________________________
 *
 * created by: f.lautenschlager
 * creation date: 25.02.14 16:34
 * description: 
 * ______________________________________________________________________________
 *
 * Copyright: (c) Weigend AM, all rights reserved
 * ______________________________________________________________________________
 */
package io.softwareecg.collector.repository.rdb.solr.producer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Uniform logging API for events. The log output has a canonical format, which can be parsed by the
 * Software EKG analysis infrastructure to produce time series.
 * <p/>
 * Timestamps are recorded explicitly in the events, to remain independent from the log format specified in the logging
 * configuration. In particular, it is also possible to RECORD events that happened before the log entry is written.
 *
 */
public class EventLog {


    public static void main(String[] args) {
        int i = 0;

        do {
            Map<String, String> properties = new HashMap<>();
            properties.put("THREAD", Thread.currentThread().getName());
            long startMillis = System.currentTimeMillis();
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                //BLA
            }

            long endMillis = System.currentTimeMillis();

            EventLog.logInterval("io.softwareecg.LogIntervalEvent", startMillis, endMillis, properties);

            properties.put("REQUEST-ID", "651");

            EventLog.logEvent("Exception", properties);
            i++;
        } while (i < 1000);

    }


    public static final String TIMESTAMP_PATTERN = "dd-MM-yyyy HH:mm:ss,SSS";

    private static final Logger LOG = LoggerFactory.getLogger(EventLog.class);

    public static void logEvent(String eventName, Map<String, String> properties) {
        logEvent(eventName, System.currentTimeMillis(), properties);
    }

    public static void logEvent(String eventName, long timestamp, Map<String, String> properties) {
        if (LOG.isInfoEnabled()) {
            // E.g. PERF [.] MyEvent [2014-02-20 12:34:56,123] {prop1: "value1", prop2: "value2"}
            LOG.info("PERF [.] {} [{}] {}",
                    eventName, formatMillis(System.currentTimeMillis()), formatProperties(properties));
        }
    }

    public static void logInterval(String eventName, long startMillis, long endMillis, Map<String, String> properties) {
        if (LOG.isInfoEnabled()) {
            // E.g. PERF [.] MyEvent [2014-02-20 12:34:56,123;2014-02-20 14:12:42,532] {prop1: "value1", prop2: "value2"}
            LOG.info("PERF [I] {} [{};{};{}] {}",
                    eventName, formatMillis(startMillis), formatMillis(endMillis), endMillis - startMillis, formatProperties(properties));
        }
    }

    private static String formatMillis(long millis) {
        return new SimpleDateFormat(TIMESTAMP_PATTERN).format(new Date(millis));
    }

    private static String formatProperties(Map<String, String> properties) {
        return ""; // TODO Format key-value pairs in a json-like format
    }
}

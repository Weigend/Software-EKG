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
package io.softwareecg.collector.repository.json;

import io.softwareecg.collector.common.CollectorProperties;
import io.softwareecg.collector.dt.TimeSeriesHeader;
import io.softwareecg.collector.repository.StaticTimeSeriesRepository;
import org.apache.commons.lang3.time.FastDateFormat;
import org.apache.log4j.Category;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.RollingFileAppender;
import org.apache.log4j.spi.LoggingEvent;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Date;
import java.util.TimeZone;

import static java.lang.String.format;

/**
 * Log4J JSON Storage. Be careful on this storage with massive metric collection or high intense measure intervals. Each
 * resulted measured value results in a output event.
 *
 */
public class Json4jRepository implements StaticTimeSeriesRepository {

    /**
     * The class logger
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(Json4jRepository.class);
    private static final String PARAM_MAX_BACKUP_INDEX = "Json4jRepository.MaxBackupIndex";
    private static final String PARAM_MAX_FILESIZE = "Json4jRepository.MaxFileSize";
    private static final String DEFAULT_MAX_FILESIZE = "10MB";
    private static final String DEFAULT_MAX_BACKUP_INDEX = "1";
    private static final TimeZone UTC = TimeZone.getTimeZone("UTC");
    private static final FastDateFormat ISO_DATETIME_TIME_ZONE_FORMAT_WITH_MILLIS = FastDateFormat.getInstance("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", UTC);
    private static final int LOG_EVENT_INTERVAL = 10000;

    private final String nodeGroupId;
    private final String process;
    private final String host;

    private final String metricGroupId;

    /**
     * Defines the appender and file name
     */
    private final String logFileName;

    /**
     * The current log4j appender
     */
    private RollingFileAppender appender;

    /**
     * The json logger. Object type to avoid sonar errors.
     */
    private Category jsonLogger;

    /**
     * The count.
     */
    private int count = 0;
    /**
     * Our file type
     */
    private static final String JSON_FILE_SUFFIX = ".json";

    /**
     * The properties
     */
    private final CollectorProperties props = CollectorProperties.getProperties();

    /**
     * The metric names
     */
    private String[] counterDescriptions;

    /**
     * Public constructor.
     *
     * @param logFileName    the logfilename without extension
     * @param host           the host.
     * @param process        the process.
     * @param metricGroupId  the group of counters.
     * @param nodeGroupId    the host group.
     */
    public Json4jRepository(String logFileName, String host, String process, String metricGroupId, String nodeGroupId) {
        this.logFileName = logFileName;
        this.host = host;
        this.process = process;
        this.metricGroupId = metricGroupId;
        this.nodeGroupId = nodeGroupId;
    }

    /* (non-Javadoc)
     * @see io.softwareecg.collector.storage.StaticTimeSeriesRepository#createMeasurement(TimeSeriesHeader, String[])
     */
    @Override
    public void createMeasurement(TimeSeriesHeader timeSeriesHeader, String[] metricLabels) {
        try {
            counterDescriptions = metricLabels.clone();
            createAndSetLogAppender();
        }
        catch (IOException e) {
            LOGGER.warn(format("Could not setup a RollingFileAppender for %s", logFileName), e);
        }
    }

    private void createAndSetLogAppender() throws IOException {
        int maxBackupIndex = Integer.valueOf(props.getProperty(PARAM_MAX_BACKUP_INDEX, DEFAULT_MAX_BACKUP_INDEX));
        String maxFileSize = String.valueOf(props.getProperty(PARAM_MAX_FILESIZE, DEFAULT_MAX_FILESIZE));

        String loggerName = logFileName + JSON_FILE_SUFFIX;
        jsonLogger = org.apache.log4j.Logger.getLogger(loggerName);

        appender = new RollingFileAppender(new PatternLayout(), loggerName);
        appender.setMaxBackupIndex(maxBackupIndex);
        appender.setMaxFileSize(maxFileSize);

        jsonLogger.addAppender(appender);
        jsonLogger.setAdditivity(false);
    }

    /* (non-Javadoc)
     * @see io.softwareecg.collector.storage.StaticTimeSeriesRepository#addValueSetToMeasurement(Date, Double[])
     */
    @SuppressWarnings("unchecked")
    @Override
    public void addValueSetToMeasurement(Date date, Double[] metricValues) {
        for (int j = 0; j < metricValues.length; j++) {
            if (metricValues[j] != null && metricValues[j] != -1d) {

                JSONObject jsonEvent = createDefaultJsonEvent(date);
                jsonEvent.put("metric_name", counterDescriptions[j]);
                jsonEvent.put("metric_value", metricValues[j]);

                String line = jsonEvent.toJSONString();
                if (LOGGER.isInfoEnabled() && ++count % LOG_EVENT_INTERVAL == 0) {
                    LOGGER.info(LOG_EVENT_INTERVAL + " docs written. Last doc: " + line);
                }

                appender.doAppend(new LoggingEvent(null, jsonLogger, null, line, null));
            }
        }
    }

    /**
     * Nothing to do: every document is written to the file when it is added.
     * <p>
     * A job that collects from a source with a history of its own flushes after every cycle. This
     * must not begin a new file - with the limit of files kept, only the documents of the last
     * cycles would be left.
     */
    @Override
    public void flush() {
        // the appender writes immediately
    }

    /**
     * The storage is given up, at the end of the job or because its metrics changed: what is
     * written under the same name afterwards begins a new file.
     */
    @Override
    public void cleanUp() {
        if (appender != null) {
            appender.rollOver();
        }
    }

    @SuppressWarnings("unchecked")
    private JSONObject createDefaultJsonEvent(Date date) {
        JSONObject jsonEvent = new JSONObject();
        jsonEvent.put("@timestamp", dateFormat(date.getTime()));
        jsonEvent.put("host", host);
        jsonEvent.put("process", process);
        jsonEvent.put("metricGroupId", metricGroupId);
        jsonEvent.put("nodeGroupId", nodeGroupId);
        return jsonEvent;
    }

    private static String dateFormat(long timestamp) {
        return ISO_DATETIME_TIME_ZONE_FORMAT_WITH_MILLIS.format(timestamp);
    }
}

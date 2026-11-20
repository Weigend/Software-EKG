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
package io.softwareecg.collector.repository.csv;

import io.softwareecg.collector.common.CollectorProperties;
import io.softwareecg.collector.dt.TimeSeriesHeader;
import io.softwareecg.collector.repository.StaticTimeSeriesRepository;
import io.softwareecg.collector.repository.common.CommonConstants;
import org.apache.log4j.Category;
import org.apache.log4j.Logger;
import org.apache.log4j.RollingFileAppender;
import org.apache.log4j.spi.LoggingEvent;

import java.io.IOException;
import java.text.NumberFormat;
import java.util.Date;

/**
 * Log4J CSV Storage.
 */
public class Csv4jRepository implements StaticTimeSeriesRepository {

    private static final String MAX_BACKUP_INDEX_CONF_KEY = "Csv4jRepository.MaxBackupIndex";

    private static final String MAX_FILESIZE_CONF_KEY = "Csv4jRepository.MaxFileSize";

    /**
     * The class logger
     */
    private static final Logger LOGGER = Logger.getLogger(Csv4jRepository.class);

    /**
     * Defines the appender and file name
     */
    private final String logFileName;

    /**
     * The current log4j appender
     */
    private RollingFileAppender appender;

    /**
     * The csv logger. Object type to avoid sonar errors.
     */
    private Category csvLogger;

    /**
     * The count.
     */
    private int count = 0;
    /**
     * Our file type
     */
    private static final String CSV = ".csv";

    /**
     * The delimiter between the csv fields.
     */
    private static final char CSV_DELIMITER = ';';

    /**
     * The properties
     */
    private final CollectorProperties props = CollectorProperties.getProperties();

    private final String measurement;


    /**
     * Public Constructor
     *
     * @param measurement the measurement that identifies the current 'session'
     * @param logFileName defines the log file name and appender name
     */
    public Csv4jRepository(String measurement, String logFileName) {
        this.logFileName = logFileName;
        this.measurement = measurement;
    }


    /* (non-Javadoc)
     * @see io.softwareecg.collector.storage.StaticTimeSeriesStorage#createMeasurement(TimeSeriesHeader, String[])
     */
    @Override
    public void createMeasurement(TimeSeriesHeader timeSeriesHeader, String[] metricLabels) {
        try {
            String loggerName = logFileName + CSV;
            csvLogger = Logger.getLogger(loggerName);

            appender = new RollingFileAppender(new CsvLog4jLayout(measurement, timeSeriesHeader, metricLabels.clone()), loggerName);
            //appender.setLayout(new CsvLog4jLayout(metricLabels.clone()));

            int maxBackupIndex = Integer.valueOf(props.getProperty(MAX_BACKUP_INDEX_CONF_KEY, "3"));
            appender.setMaxBackupIndex(maxBackupIndex);

            String maxFileSize = String.valueOf(props.getProperty(MAX_FILESIZE_CONF_KEY, "30MB"));
            appender.setMaxFileSize(maxFileSize);

            csvLogger.addAppender(appender);
            csvLogger.setAdditivity(false);

        } catch (IOException e) {
            LOGGER.warn("Could not setup a RollingFileAppender for " + logFileName, e);
        }
    }

    /* (non-Javadoc)
     * @see io.softwareecg.collector.storage.StaticTimeSeriesStorage#addValueSetToMeasurement(Date, Double[])
     */
    @Override
    public void addValueSetToMeasurement(Date date, Double[] metricValues) {
        StringBuilder line = new StringBuilder();

        NumberFormat numberFormat = CommonConstants.getDecimalFormat();
        line.append(date.getTime()).append(CSV_DELIMITER);

        // write one column in the CSV for each metric value
        for (Double metricValue : metricValues) {
            if (metricValue != null) {
                line.append(numberFormat.format((double) metricValue)).append(CSV_DELIMITER);
            }
        }

        // remove the last colon (CSV_DELIMITER)
        line.deleteCharAt(line.length() - 1);

        if (LOGGER.isInfoEnabled() && ++count % 10000 == 0) {
            LOGGER.info("10000 Rows written. Last row: " + line);
        }

        // write new line with CSV data using log4j logger framework
        appender.doAppend(new LoggingEvent(null, csvLogger, null, line.toString(), null));
    }

    @Override
    public void flush() {
        appender.rollOver();
    }

}

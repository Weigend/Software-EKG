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

import io.softwareecg.collector.dt.TimeSeriesHeader;
import io.softwareecg.collector.repository.common.CSVUtil;
import org.apache.log4j.PatternLayout;

/**
 * Layout for log4j.
 */
public class CsvLog4jLayout extends PatternLayout {

    /**
     * A template for a JSON structure that stores all metadata of the time series
     * like the process or the name of the pod.
     * The template also defines a static version number that should increase than the
     * format for the JSON changes in future.
     */
    private static final String JSON_META_DATA_TEMPLATE = "{" +
            "\"logFormatVersion\":6.1, \"metadata\":{" +
            "\"ekgProjectName\":\"#EKG_PROJECT_NAME#\", " +
            "\"hostGroupName\":\"#HOST_GROUP#\", " +
            "\"hostName\":\"#HOST#\", " +
            "\"namespace\":\"#NAMESPACE#\", " +
            "\"service\":\"#SERVICE#\", " +
            "\"pod\":\"#POD#\", " +
            "\"container\":\"#CONTAINER#\", " +
            "\"process\":\"#PROCESS#\", " +
            "\"measurement\":\"#MEASUREMENT#\", " +
            "\"metricGroup\":\"#METRIC_GROUP#\" " +
            "}}";

    /**
     * An array with all metric names that are measured in the
     * context of the process, node and optional cloud platform details
     * defined in the metadata further down.
     */
    private final String[] metricNames;

    /**
     * A JSON structure that stores all metadata of the time series
     * like the process or the name of the pod.
     */
    private String headerMetaData;

    /**
     * Constructs a new instance of CsvLog4jLayout that will
     * write out the given metadata and metric names write the
     * CSV headers in the current specified EKG Collector log format.
     *
     * @param measurement the measurement that identifies the current 'session'
     * @param timeSeriesHeader an instance of {@link TimeSeriesHeader} with the metadata of the time series
     * @param metricNames an array with all metric names that are measured
     */
    public CsvLog4jLayout(String measurement, TimeSeriesHeader timeSeriesHeader, String[] metricNames) {
        this.metricNames = metricNames.clone();
        setupHeaderMetadata(measurement, timeSeriesHeader);
    }

    /**
     * Returns the header for the log file.
     *
     * @return csv-ekg header
     */
    @Override
    public String getHeader() {
        return getLegend();
    }

    /**
     * Prints the legend.
     */
    private String getLegend() {
        StringBuilder legend = new StringBuilder();
        legend.append(headerMetaData);
        legend.append(";");
        for (String counter : metricNames) {
            legend.append(CSVUtil.escapeCsvColumnName(counter));
            legend.append(';');
        }
        legend.append(System.getProperty("line.separator"));

        return legend.toString();
    }

    /**
     * Returns an empty pattern used by log4j
     *
     * @return empty string
     */
    @Override
    public String getConversionPattern() {
        return "";
    }

    private void setupHeaderMetadata(String measurement, TimeSeriesHeader timeSeriesHeader) {
        this.headerMetaData = JSON_META_DATA_TEMPLATE
                .replace("#EKG_PROJECT_NAME#", extractNullSaveEscapedValue(timeSeriesHeader.getEkgProjectName()))
                .replace("#HOST_GROUP#", extractNullSaveEscapedValue(timeSeriesHeader.getHostGroupId()))
                .replace("#HOST#", extractNullSaveEscapedValue(timeSeriesHeader.getHostName()))
                .replace("#NAMESPACE#", extractNullSaveEscapedValue(timeSeriesHeader.getNamespace()))
                .replace("#SERVICE#", extractNullSaveEscapedValue(timeSeriesHeader.getService()))
                .replace("#POD#", extractNullSaveEscapedValue(timeSeriesHeader.getPodName()))
                .replace("#CONTAINER#", extractNullSaveEscapedValue(timeSeriesHeader.getContainerId()))
                .replace("#PROCESS#", extractNullSaveEscapedValue(timeSeriesHeader.getProcessLabel()))
                .replace("#MEASUREMENT#", extractNullSaveEscapedValue(measurement))
                .replace("#METRIC_GROUP#", extractNullSaveEscapedValue(timeSeriesHeader.getMetricGroupId()));
    }

    private String extractNullSaveEscapedValue(String value) {
        return (value == null ? "" : value).replaceAll("\"", "\\\\\"");
    }
}

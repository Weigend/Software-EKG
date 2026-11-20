package io.softwareecg.collector.repository.rdb.api.datamanagement;

import io.softwareecg.collector.dt.TimeSeriesHeader;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * This class is a simple container that stores
 * various metadata that define together the context of
 * the measurement of a set of metrics fetched from the same
 * node and process.
 *
 * @since 02.11.2018
 */
public class MeasurementContext {

    private final TimeSeriesHeader header;

    private final String[] metricLabels;

    /**
     * the name of the measurement all further proceed time series
     * data will assigned to
     */
    private final String measurement = createMeasurement();

    /**
     * Constructs a new instance of MeasurementContext
     * initialized with the given context data.
     *
     * @param header a TimeSeriesHeader instance with the metadata (dimension values) valid for each of the metrics
     * @param metricLabels an array of metrics that will collected in this context
     */
    public MeasurementContext(TimeSeriesHeader header, String[] metricLabels) {
        this.metricLabels = metricLabels;
        this.header = header;
    }

    public TimeSeriesHeader getHeader() {
        return header;
    }

    public String[] getMetricLabels() {
        return metricLabels;
    }

    public String getMeasurement() {
        return measurement;
    }

    /**
     * @return the name of the measurement as YYYY-mm-DD (2014-02-24)
     */
    private static String createMeasurement() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        return dateFormat.format(new Date());
    }
}

package io.softwareecg.awb.importer.adapter.prometheus.client.model.api.query;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import static io.softwareecg.awb.importer.adapter.prometheus.client.model.api.query.TimeInterval.Unit;

/**
 * todo enter comment here
 *
 * @since 13.07.2018
 */
public class RangeQuery {

    private static final double MONTH_IN_MILLIS  = 2_629_743_833.0; // month
    private static final double WEEK_IN_MILLIS   = 604_800_000.0; // week
    private static final double DAY_IN_MILLIS    = 24 * 3600_000.0; // hour
    private static final double HOUR_IN_MILLIS   = 3600_000.0; // hour
    private static final double MINUTE_IN_MILLIS = 60_000.0; // minute


    private static final Pattern METRIC_VALIDATION_PATTERN = Pattern.compile("[a-zA-Z_:][a-zA-Z0-9_:]+");

    private static final Pattern LABEL_VALIDATION_PATTERN = Pattern.compile("[a-zA-Z_][a-zA-Z0-9_]+");

    private static final TimeInterval DEFAULT_INTERVAL = new TimeInterval(10, TimeInterval.Unit.MINUTE);

    private String metricName;

    private final Map<String, String> metricLabels = new HashMap<>();

    // "2018-07-10T00:00:00.000Z"
    private Instant referenceDatetime = Instant.now();

    private TimeInterval seriesInterval = DEFAULT_INTERVAL;

    public String getQueryString()  {
        return "?query=" + metricName + getLabelsFilterParam() + getTimeIntervalParam() + "&time=" + getRangeEndDate();
    }

    public String getMetricName() {
        return metricName;
    }

    public String getRangeEndDate() {
        return referenceDatetime.toString();
    }

    public String getTimeIntervalParam() {
        String unitStr;
        switch (seriesInterval.getUnit()) {
            case MINUTE: unitStr = "m"; break;
            case DAY:    unitStr = "d"; break;
            case WEEK:   unitStr = "w"; break;
            case YEAR:   unitStr = "y"; break;
            default:     unitStr = "m"; break;
        }
        return "[" + seriesInterval.getAmount() + unitStr + "]";
    }

    public void addLabelFilterParam(String labelName, String labelValue) {
        metricLabels.put(labelName, labelValue);
    }

    @Override
    public String toString() {
        return "RangeQuery{" +
                "metricName='" + metricName + '\'' +
                ", metricLabels=" + metricLabels +
                ", referenceDatetime=" + referenceDatetime +
                ", seriesInterval=" + seriesInterval +
                '}';
    }

    public String getLabelsFilterParam() {
        StringBuilder labelParamBuilder = new StringBuilder();

        if (metricLabels != null && metricLabels.size() > 0) {

            labelParamBuilder.append("{");

            for (Map.Entry<String, String> labelEntry : metricLabels.entrySet()) {

                labelParamBuilder.append(labelEntry.getKey());
                labelParamBuilder.append("=\"");
                labelParamBuilder.append(labelEntry.getValue());
                labelParamBuilder.append("\", ");
            }

            labelParamBuilder.delete(labelParamBuilder.length() - 2, labelParamBuilder.length() - 1);

            labelParamBuilder.append("}");
        }

        return URLEncoder.encode(labelParamBuilder.toString(), StandardCharsets.UTF_8);


    }


    public static class Builder {

        private final RangeQuery query = new RangeQuery();

        public static Builder newRangeQuery() {
            return new Builder();
        }

        /**
         *
         * @param metric the name of the metric the
         * @return the builder instance for fluent programming
         */
        public Builder forMetric(String metric) {

            if (!METRIC_VALIDATION_PATTERN.matcher(metric).matches()) {
                throw new IllegalArgumentException("metric name contains invalid characters");
            }

            this.query.metricName = metric;

            return this;
        }

        /**
         *
         * @param labelName the name of the label that will use to narrow the metric
         * @param labelValue the value of the label that will use to narrow the metric
         * @return the builder instance for fluent programming
         */
        public Builder addLabelFilter(String labelName, String labelValue) {

            if (!LABEL_VALIDATION_PATTERN.matcher(labelName).matches()) {
              throw new IllegalArgumentException("metric label name '" + labelName + "' contains invalid characters");
            }

            if (!LABEL_VALIDATION_PATTERN.matcher(labelValue).matches()) {
              throw new IllegalArgumentException("metric label value '" + labelValue + "' contains invalid characters");
            }

            this.query.metricLabels.put(labelName, labelValue);
            return this;
        }

        /**
         *
         * @param interval the time range the time-series backward from the referenceDatetime
         * @return the builder instance for fluent programming
         */
        public Builder withTimeRange(TimeInterval interval) {
            this.query.seriesInterval = interval;
            return this;
        }

        public Builder withStartEndTimeStamp(long start, long end) {

            if (start < 0 || end < 0) {
                return this;
            }

            this.query.referenceDatetime = Instant.ofEpochMilli(end);

            double interval = end - start;

            if (interval > MONTH_IN_MILLIS) {
                this.query.seriesInterval  = new TimeInterval((int)Math.ceil(interval / WEEK_IN_MILLIS), Unit.WEEK);

            } else if (interval > WEEK_IN_MILLIS) {
                this.query.seriesInterval  = new TimeInterval((int)Math.ceil(interval / DAY_IN_MILLIS), Unit.DAY);

            } else {
                this.query.seriesInterval  = new TimeInterval((int)Math.ceil(interval / MINUTE_IN_MILLIS), Unit.MINUTE);
            }

            return this;
        }


        /**
         *
         * @param dateTime the instant that describes the timestamp the fetched series interval will end
         * @return the builder instance for fluent programming
         */
        public Builder withEnd(Instant dateTime) {
            this.query.referenceDatetime = dateTime;
            return this;
        }

        public RangeQuery build() {
            return query;
        }
    }
}

//______________________________________________________________________________
//
//                  Project:    Software EKG
//______________________________________________________________________________
//
//                   Author:    Weigend AM 2009 - 2017
//______________________________________________________________________________
//
// Notice: This piece of software was created, designed and implemented by
// experienced craftsmen and innovators in Munich, Germany.
// Changes should be done with respect to the original design.
//______________________________________________________________________________
package io.softwareecg.awb.bookmark.store;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.softwareecg.awb.query.api.QuerySpec;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

import java.io.IOException;
import java.util.List;
import java.util.Stack;

/**
 * Represents a bookmark that stores a search query for metrics.
 *
 */
public final class MetricQueryBookmark extends QueryBookmark<QuerySpec> {

    private String metric;

    private String exclude;

    private boolean regex;

    private boolean multiMetricMode;

    private boolean expertMode;

    private String rawQuery;

    private String hostGroup;

    private String namespace;

    private String service;

    private String pod;

    private String container;

    private String processGroup;

    private Stack<MetricQueryBookmark> subBookmarks;

    private String chartType;

    public String getExclude() {
        return exclude;
    }

    public boolean isRegex() {
        return regex;
    }

    public String getMetric() {
        return metric;
    }

    public boolean isMultiMetricMode() {
        return multiMetricMode;
    }

    public boolean isExpertMode() {
        return expertMode;
    }


    public String getRawQuery() {
        return rawQuery;
    }

    public String getHostGroup() {
        return hostGroup;
    }

    public String getNamespace() {
        return namespace;
    }

    public String getService() {
        return service;
    }

    public String getPod() {
        return pod;
    }

    public String getContainer() {
        return container;
    }

    public String getProcessGroup() {
        return processGroup;
    }

    public String getChartType() {
        return chartType;
    }

    /**
     * Get the Bookmark as regular {@link QuerySpec}.
     *
     * @return The {@link QuerySpec} for the Bookmark.
     */
    @Override
    public QuerySpec asQueryContext() {
        QuerySpec.Builder builder = new QuerySpec.Builder()
                .withMetric(metric)
                .withExclude(exclude)
                .withMultiMetricMode(multiMetricMode)
                .withExpertMode(expertMode)
                .withRawQuery(rawQuery)
                .withHostGroup(hostGroup)
                .withNamespace(namespace)
                .withService(service)
                .withPod(pod)
                .withContainer(container)
                .withProcessGroup(processGroup);

        buildQueryContext(builder);
        return builder.build();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        MetricQueryBookmark that = (MetricQueryBookmark) o;

        return new EqualsBuilder()
                .appendSuper(super.equals(o))
                .append(isRegex(), that.isRegex())
                .append(isMultiMetricMode(), that.isMultiMetricMode())
                .append(getRawQuery(), that.getRawQuery())
                .append(isExpertMode(), that.isExpertMode())
                .append(getMetric(), that.getMetric())
                .append(getExclude(), that.getExclude())
                .append(getHostGroup(), that.getHostGroup())
                .append(getNamespace(), that.getNamespace())
                .append(getService(), that.getService())
                .append(getPod(), that.getPod())
                .append(getContainer(), that.getContainer())
                .append(getProcessGroup(), that.getProcessGroup())
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
                .appendSuper(super.hashCode())
                .append(getMetric())
                .append(getExclude())
                .append(isRegex())
                .append(isMultiMetricMode())
                .append(isExpertMode())
                .append(getHostGroup())
                .append(getNamespace())
                .append(getService())
                .append(getPod())
                .append(getContainer())
                .append(getProcessGroup())
                .toHashCode();
    }


    /**
     * Returns a Stack of Sub bookmarks
     *
     * @return a Stack of Sub bookmarks
     */
    public Stack<MetricQueryBookmark> getSubBookmarks() {
        return subBookmarks == null ? new Stack<>() : subBookmarks;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("metric", metric)
                .append("exclude", exclude)
                .append("regex", regex)
                .append("multiMetricMode", multiMetricMode)
                .append("expertMode", expertMode)
                .append("rawQuery", rawQuery)
                .append("hostGroup", hostGroup)
                .append("namespace", namespace)
                .append("service", service)
                .append("pod", pod)
                .append("container", container)
                .append("processGroup", processGroup)
                .append("subBookmarks", subBookmarks)
                .toString();
    }

    // =========================================================================
    // JSON Serialization
    // =========================================================================

    /**
     * Internal DTO for JSON serialization of query-specific fields.
     * Metadata fields (id, name, bookmarkGroupId, project) are stored
     * in dedicated DB columns and are NOT part of the JSON.
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonIgnoreProperties(ignoreUnknown = true)
    static class QueryData {
        public String host;
        public String group;
        public String measurement;
        public String process;
        public Long start;
        public Long stop;
        public Long importDate;
        public String metric;
        public String exclude;
        public boolean regex;
        public boolean multiMetricMode;
        public boolean expertMode;
        public String rawQuery;
        public String hostGroup;
        public String namespace;
        public String service;
        public String pod;
        public String container;
        public String processGroup;
        public String chartType;
        public List<QueryData> subBookmarks;
    }

    private static final ObjectMapper MAPPER = new ObjectMapper();

    /**
     * Serializes the query-specific fields of this bookmark to a JSON string.
     *
     * @return JSON string containing all query parameters
     */
    public String toJson() {
        QueryData data = new QueryData();
        data.host = getHost();
        data.group = getGroup();
        data.measurement = getMeasurement();
        data.process = getProcess();
        long s = getStart();
        data.start = s > 0 ? s : null;
        long e = getStop();
        data.stop = e > 0 ? e : null;
        long i = getImportDate();
        data.importDate = i > 0 ? i : null;
        data.metric = metric;
        data.exclude = exclude;
        data.regex = regex;
        data.multiMetricMode = multiMetricMode;
        data.expertMode = expertMode;
        data.rawQuery = rawQuery;
        data.hostGroup = hostGroup;
        data.namespace = namespace;
        data.service = service;
        data.pod = pod;
        data.container = container;
        data.processGroup = processGroup;
        data.chartType = chartType;
        if (subBookmarks != null && !subBookmarks.isEmpty()) {
            data.subBookmarks = subBookmarks.stream().map(sub -> {
                QueryData sd = new QueryData();
                sd.host = sub.getHost();
                sd.group = sub.getGroup();
                sd.measurement = sub.getMeasurement();
                sd.process = sub.getProcess();
                long subStart = sub.getStart();
                sd.start = subStart > 0 ? subStart : null;
                long subStop = sub.getStop();
                sd.stop = subStop > 0 ? subStop : null;
                long subImport = sub.getImportDate();
                sd.importDate = subImport > 0 ? subImport : null;
                sd.metric = sub.getMetric();
                sd.exclude = sub.getExclude();
                sd.regex = sub.isRegex();
                sd.multiMetricMode = sub.isMultiMetricMode();
                sd.expertMode = sub.isExpertMode();
                sd.rawQuery = sub.getRawQuery();
                sd.hostGroup = sub.getHostGroup();
                sd.namespace = sub.getNamespace();
                sd.service = sub.getService();
                sd.pod = sub.getPod();
                sd.container = sub.getContainer();
                sd.processGroup = sub.getProcessGroup();
                return sd;
            }).toList();
        }

        try {
            return MAPPER.writeValueAsString(data);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to serialize MetricQueryBookmark to JSON", ex);
        }
    }

    /**
     * Deserializes a MetricQueryBookmark from a JSON string.
     * Metadata fields (id, name, bookmarkGroupId, project) must be set separately.
     *
     * @param json the JSON string
     * @return a Builder pre-filled with the deserialized query parameters
     */
    public static Builder fromJson(String json) {
        if (json == null || json.isBlank()) {
            return new Builder();
        }
        try {
            QueryData data = MAPPER.readValue(json, QueryData.class);
            Builder builder = new Builder();
            builder.withHost(data.host);
            builder.withGroup(data.group);
            builder.withMeasurement(data.measurement);
            builder.withProcess(data.process);
            builder.withStart(data.start != null ? data.start : -1);
            builder.withStop(data.stop != null ? data.stop : -1);
            if (data.importDate != null) {
                builder.withImportDate(data.importDate);
            }
            builder.withMetric(data.metric);
            builder.withExclude(data.exclude);
            builder.withRegex(data.regex);
            builder.withMultiMetricMode(data.multiMetricMode);
            builder.withExpertMode(data.expertMode);
            builder.withRawQuery(data.rawQuery);
            builder.withHostGroup(data.hostGroup);
            builder.withNamespace(data.namespace);
            builder.withService(data.service);
            builder.withPod(data.pod);
            builder.withContainer(data.container);
            builder.withProcessGroup(data.processGroup);
            builder.withChartType(data.chartType);
            if (data.subBookmarks != null && !data.subBookmarks.isEmpty()) {
                Stack<MetricQueryBookmark> subs = new Stack<>();
                for (QueryData sd : data.subBookmarks) {
                    Builder subBuilder = new Builder();
                    subBuilder.withHost(sd.host);
                    subBuilder.withGroup(sd.group);
                    subBuilder.withMeasurement(sd.measurement);
                    subBuilder.withProcess(sd.process);
                    subBuilder.withStart(sd.start != null ? sd.start : -1);
                    subBuilder.withStop(sd.stop != null ? sd.stop : -1);
                    if (sd.importDate != null) subBuilder.withImportDate(sd.importDate);
                    subBuilder.withMetric(sd.metric);
                    subBuilder.withExclude(sd.exclude);
                    subBuilder.withRegex(sd.regex);
                    subBuilder.withMultiMetricMode(sd.multiMetricMode);
                    subBuilder.withExpertMode(sd.expertMode);
                    subBuilder.withRawQuery(sd.rawQuery);
                    subBuilder.withHostGroup(sd.hostGroup);
                    subBuilder.withNamespace(sd.namespace);
                    subBuilder.withService(sd.service);
                    subBuilder.withPod(sd.pod);
                    subBuilder.withContainer(sd.container);
                    subBuilder.withProcessGroup(sd.processGroup);
                    subs.push(subBuilder.build());
                }
                builder.withMetricQueryBookmarks(subs);
            }
            return builder;
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to deserialize MetricQueryBookmark from JSON", ex);
        }
    }

    /**
     * Builder to create a new {@link MetricQueryBookmark}.
     */
    public static class Builder extends QueryBookmark.Builder<MetricQueryBookmark> {
        private String exclude;
        private String metric;
        private boolean regex;
        private boolean multiMetricMode;
        private boolean expertMode;
        private String rawQuery;
        private String hostGroup;
        private String namespace;
        private String service;
        private String pod;
        private String container;
        private String processGroup;
        private String chartType;
        private Stack<MetricQueryBookmark> subBookmarks;

        /**
         * Init a new {@link MetricQueryBookmark.Builder} without any predefined values.
         */
        public Builder() {
        }

        /**
         * Init a new {@link MetricQueryBookmark.Builder MetricQueryBookmark builder}
         * and take the given log query to predefine the values.
         *
         * @param context the predefined values
         */
        public Builder(QuerySpec context) {
            super(context);
            this.exclude = context.getExcludeMetricName();
            this.multiMetricMode = context.isMultiMetricMode();
            this.expertMode = context.isExpertMode();
            this.rawQuery = context.getRawQuery();
            this.metric = context.getMetricName();
            this.hostGroup = context.getHostGroupName();
            this.namespace = context.getNamespaceName();
            this.service = context.getServiceName();
            this.pod = context.getPodName();
            this.container = context.getContainerName();
            this.processGroup = context.getProcessGroupName();
        }

        /**
         * Create a builder from an existing bookmark.
         *
         * @param bookmark the initial values for the builder.
         */
        public Builder(MetricQueryBookmark bookmark) {
            super(bookmark);
            this.exclude = bookmark.getExclude();
            this.regex = bookmark.isRegex();
            this.multiMetricMode = bookmark.isMultiMetricMode();
            this.expertMode = bookmark.isExpertMode();
            this.metric = bookmark.getMetric();
            this.hostGroup = bookmark.getHostGroup();
            this.namespace = bookmark.getNamespace();
            this.service = bookmark.getService();
            this.pod = bookmark.getPod();
            this.container = bookmark.getContainer();
            this.processGroup = bookmark.getProcessGroup();
        }

        /**
         * Add exclude to builder.
         *
         * @param exclude the exclude name
         * @return this for fluent interface
         */
        public Builder withExclude(String exclude) {
            this.exclude = exclude;
            return this;
        }

        /**
         * Add metric to builder.
         *
         * @param metric the metric name
         * @return this for fluent interface
         */
        public Builder withMetric(String metric) {
            this.metric = metric;
            return this;
        }

        /**
         * Add Query bookmark metrics to builder.
         *
         * @param metrics the Query bookmark metric
         * @return this for fluent interface
         */
        public Builder withMetricQueryBookmarks(Stack<MetricQueryBookmark> metrics) {
            this.subBookmarks = metrics;
            return this;
        }

        /**
         * Clears the metrics values.
         *
         * @return this for fluent interface
         */
        public Builder clearMetrics() {
            metric = null;
            return this;
        }

        /**
         * Add regex to builder.
         *
         * @param regex the regex name
         * @return this for fluent interface
         */
        public Builder withRegex(boolean regex) {
            this.regex = regex;
            return this;
        }

        /**
         * Add multiMetricMode to builder.
         *
         * @param multiMetricMode the multiMetricMode name
         * @return this for fluent interface
         */
        public Builder withMultiMetricMode(boolean multiMetricMode) {
            this.multiMetricMode = multiMetricMode;
            return this;
        }

        /**
         * Add expertMode to builder.
         *
         * @param expertMode the expertMode name
         * @return this for fluent interface
         */
        public Builder withExpertMode(boolean expertMode) {
            this.expertMode = expertMode;
            return this;
        }

        /**
         * Add rawQuery to builder.
         *
         * @param rawQuery the rawQuery
         * @return this for fluent interface
         */
        public Builder withRawQuery(String rawQuery) {
            this.rawQuery = rawQuery;
            return this;
        }

        public Builder withHostGroup(String hostGroup) {
            this.hostGroup = hostGroup;
            return this;
        }

        public Builder withNamespace(String namespace) {
            this.namespace = namespace;
            return this;
        }

        public Builder withService(String service) {
            this.service = service;
            return this;
        }

        public Builder withPod(String pod) {
            this.pod = pod;
            return this;
        }

        public Builder withContainer(String container) {
            this.container = container;
            return this;
        }

        public Builder withProcessGroup(String processGroup) {
            this.processGroup = processGroup;
            return this;
        }

        public Builder withChartType(String chartType) {
            this.chartType = chartType;
            return this;
        }

        /**
         * Instantiates a new bookmark with the given bookmark values.
         *
         * @return The new bookmark
         */
        public MetricQueryBookmark build() {
            MetricQueryBookmark bookmark = new MetricQueryBookmark();
            build(bookmark);
            bookmark.exclude = this.exclude;
            bookmark.regex = this.regex;
            bookmark.multiMetricMode = this.multiMetricMode;
            bookmark.expertMode = this.expertMode;
            bookmark.rawQuery = this.rawQuery;
            bookmark.subBookmarks = this.subBookmarks;
            bookmark.metric = this.metric;
            bookmark.hostGroup = this.hostGroup;
            bookmark.namespace = this.namespace;
            bookmark.service = this.service;
            bookmark.pod = this.pod;
            bookmark.container = this.container;
            bookmark.processGroup = this.processGroup;
            bookmark.chartType = this.chartType;

            return bookmark;
        }


    }
}

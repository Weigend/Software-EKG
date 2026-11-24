package io.softwareecg.awb.aichat.tools;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.softwareecg.awb.query.api.ComputedTimeSeriesResponse;
import io.softwareecg.awb.query.api.MetricDataAccessService;
import io.softwareecg.awb.query.api.MetricQueryService;
import io.softwareecg.awb.query.api.ComputationSpec;
import io.softwareecg.awb.timeseries.dimension.NamedValueEntity;
import io.softwareecg.awb.query.api.QuerySpec;
import io.softwareecg.awb.timeseries.TimeSeries;
import io.softwareecg.awb.timeseries.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.DoubleSummaryStatistics;
import java.util.List;
import java.util.function.Supplier;

/**
 * Tool that queries time series data from the AWB repository.
 * Returns a token-efficient JSON summary with statistics and sampled values.
 */
public class QueryTimeSeriesTool implements AiTool {

    private static final Logger LOG = LoggerFactory.getLogger(QueryTimeSeriesTool.class);
    private static final int DEFAULT_MAX_POINTS = 100;
    private static final int HARD_MAX_POINTS = 200;
    private static final int MAX_SERIES_RETURNED = 50;
    private static final int QUERY_LIMIT = Integer.MAX_VALUE;

    /** The threshold of a query that leaves all values. */
    private static final int ALL_VALUES = -1;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Supplier<MetricDataAccessService> metricServiceSupplier;
    private final Supplier<MetricQueryService> queryServiceSupplier;
    private final ChartContextProvider contextProvider;
    private final Supplier<ZoneId> timeZone;

    /**
     * @param timeZone the time zone the times of the answer are given in: that of the chart,
     *                 so that the times the assistant names are those of the axis beside it
     */
    public QueryTimeSeriesTool(Supplier<MetricDataAccessService> metricServiceSupplier,
                               Supplier<MetricQueryService> queryServiceSupplier,
                               ChartContextProvider contextProvider, Supplier<ZoneId> timeZone) {
        this.metricServiceSupplier = metricServiceSupplier;
        this.queryServiceSupplier = queryServiceSupplier;
        this.contextProvider = contextProvider;
        this.timeZone = timeZone;
    }

    @Override
    public String getName() {
        return "query_timeseries";
    }

    @Override
    public String getDescription() {
        return "Query time series data from the monitoring repository. Returns metrics with statistics " +
                "and sampled values. Use specific host and metric filters to narrow results. " +
                "Supports wildcard patterns with '*' for metric names. " +
                "IMPORTANT: Only use exact metric names from list_available_data results or from " +
                "previous query_timeseries responses. Never guess or derive metric names from general knowledge.";
    }

    @Override
    public JsonNode getParametersSchema() {
        ObjectNode schema = objectMapper.createObjectNode();
        schema.put("type", "object");
        ObjectNode props = schema.putObject("properties");

        props.putObject("project").put("type", "string")
                .put("description", "Project name (required)");
        props.putObject("host").put("type", "string")
                .put("description", "Host name filter (optional)");
        props.putObject("hostgroup").put("type", "string")
                .put("description", "Host group filter (optional)");
        props.putObject("metric").put("type", "string")
                .put("description", "Metric name or pattern with * wildcards (optional)");
        props.putObject("metricgroup").put("type", "string")
                .put("description", "Metric group filter (optional, auto-resolved from metric if omitted)");
        props.putObject("measurement").put("type", "string")
                .put("description", "Measurement filter (optional)");
        props.putObject("process").put("type", "string")
                .put("description", "Process name filter (optional)");
        props.putObject("start").put("type", "number")
                .put("description", "Start time as epoch milliseconds. Only use if provided by chart context. Omit to get all data.");
        props.putObject("end").put("type", "number")
                .put("description", "End time as epoch milliseconds. Only use if provided by chart context. Omit to get all data.");
        props.putObject("max_points").put("type", "integer")
                .put("description", "Maximum data points per series for downsampling (default 100, max 200). Do not set higher than 200.");
        props.putObject("include_values").put("type", "boolean")
                .put("description", "If true, include the raw time series values array. Default false — only stats (min/max/avg) are returned. Set to true only when you need to see the actual data curve.");

        ArrayNode required = schema.putArray("required");
        required.add("project");

        return schema;
    }

    @Override
    public String execute(String argumentsJson) {
        try {
            JsonNode args = objectMapper.readTree(argumentsJson);

            String project = hasText(args, "project") ? args.get("project").asText() : null;
            if (project == null) {
                return "{\"error\": \"Missing required parameter 'project'. Use get_current_chart_context to find the active project, or ask the user.\"}";
            }
            int maxPoints = args.has("max_points") ? args.get("max_points").asInt() : DEFAULT_MAX_POINTS;
            maxPoints = Math.min(maxPoints, HARD_MAX_POINTS);
            boolean includeValues = args.has("include_values") && args.get("include_values").asBoolean();

            QuerySpec.Builder<QuerySpec> builder = new QuerySpec.Builder<>();
            builder.withProject(project);

            if (hasText(args, "host")) builder.withHost(args.get("host").asText());
            if (hasText(args, "hostgroup")) builder.withHostGroup(args.get("hostgroup").asText());
            if (hasText(args, "metric")) builder.withMetric(args.get("metric").asText());
            if (hasText(args, "metricgroup")) builder.withMetricGroup(args.get("metricgroup").asText());
            if (hasText(args, "measurement")) builder.withMeasurement(args.get("measurement").asText());
            if (hasText(args, "process")) builder.withProcess(args.get("process").asText());

            // Auto-resolve metricgroup when metric is set but metricgroup is not.
            // In EKG/Solr, ts_metricGroup and ts_metricName are separate indexed fields.
            // The AI typically only knows the metric name from list_available_data facets,
            // but the time series query needs both to match documents.
            MetricDataAccessService service = metricServiceSupplier.get();
            MetricQueryService queries = queryServiceSupplier.get();
            if (service == null || queries == null) {
                return "{\"error\": \"No repository connection available. The user must open a project first.\"}";
            }

            if (hasText(args, "metric") && !hasText(args, "metricgroup")) {
                try {
                    QuerySpec resolveParams = builder.build();
                    List<String> groups = service.getMetricGroups(resolveParams).stream()
                            .map(NamedValueEntity::getValueName)
                            .filter(name -> !"*".equals(name))
                            .toList();
                    if (groups.size() == 1) {
                        builder.withMetricGroup(groups.get(0));
                        LOG.info("Auto-resolved metricgroup '{}' for metric '{}'",
                                groups.get(0), args.get("metric").asText());
                    } else if (groups.size() > 1) {
                        LOG.info("Multiple metricgroups found for metric '{}': {}",
                                args.get("metric").asText(), groups);
                    }
                } catch (Exception e) {
                    LOG.warn("Failed to auto-resolve metricgroup: {}", e.getMessage());
                }
            }

            // Guard against AI fabricating start/end timestamps:
            // Only apply time filters if the chart context actually has them.
            boolean contextHasTimeRange = contextHasTimeRange();
            if (args.has("start")) {
                if (contextHasTimeRange) {
                    builder.withStart(args.get("start").asLong());
                } else {
                    LOG.warn("Ignoring AI-provided 'start' parameter — chart context has no time range");
                }
            }
            if (args.has("end")) {
                if (contextHasTimeRange) {
                    builder.withStop(args.get("end").asLong());
                } else {
                    LOG.warn("Ignoring AI-provided 'end' parameter — chart context has no time range");
                }
            }

            QuerySpec params = builder.build();
            ComputationSpec compute = new ComputationSpec();
            compute.setThreshold(ALL_VALUES);
            compute.setMaxDisplaySeries(MAX_SERIES_RETURNED);
            ComputedTimeSeriesResponse response = queries.getComputedTimeSeries(params, compute, QUERY_LIMIT);

            if (response.hasErrorPayload()) {
                throw new IllegalStateException(response.getOccurredError().getMessage(), response.getOccurredError());
            }
            if (response.isRequestAborted()) {
                return "{\"error\": \"The data could not be loaded: the source of the project cannot be reached, " +
                        "or the query was cancelled.\"}";
            }

            if (response.getTimeSeries().isEmpty() && hasText(args, "metric")) {
                String metricName = args.get("metric").asText();
                return "{\"total_series_found\": 0, \"series\": [], " +
                        "\"hint\": \"No data found for metric '" + metricName.replace("\"", "'") + "'. " +
                        "This metric name may not exist in the repository. " +
                        "Use list_available_data with facet 'metrics' to discover actual metric names. " +
                        "Do NOT guess metric names from general knowledge.\"}";
            }

            return formatResponse(response, queries, maxPoints, includeValues);

        } catch (Exception e) {
            LOG.error("Error executing query_timeseries tool", e);
            return "{\"error\": \"" + e.getMessage().replace("\"", "'") + "\"}";
        }
    }

    private String formatResponse(ComputedTimeSeriesResponse response, MetricQueryService queries,
                                  int maxPoints, boolean includeValues) {
        try {
            ObjectNode result = objectMapper.createObjectNode();
            ZoneId zone = timeZone.get();
            List<TimeSeries> seriesList = response.getTimeSeries();
            boolean truncated = response.hasSeriesTruncated();
            if (truncated) {
                // the query ends when there are enough of them, so nobody knows how many there are
                result.put("total_series_found", "more than " + seriesList.size());
            } else {
                result.put("total_series_found", seriesList.size());
            }
            result.put("time_zone", zone.getId());

            result.put("series_returned", seriesList.size());
            if (truncated) {
                result.put("truncated", true);
                result.put("hint", "Only the first " + seriesList.size() + " series shown. Use more specific filters.");
            }

            ArrayNode seriesArray = result.putArray("series");
            for (TimeSeries ts : seriesList) {
                ObjectNode seriesNode = seriesArray.addObject();
                seriesNode.put("host", ts.getHost());
                seriesNode.put("metric", ts.getMetricName());
                seriesNode.put("measurement", ts.getMeasurement());
                seriesNode.put("process", ts.getProcess());

                long[] timestamps = ts.getTimestamps();
                seriesNode.put("points", timestamps.length);

                if (timestamps.length > 0) {
                    seriesNode.put("start_time", formatEpoch(timestamps[0], zone));
                    seriesNode.put("end_time", formatEpoch(timestamps[timestamps.length - 1], zone));

                    // of all values: those that show the course are not typical of them
                    DoubleSummaryStatistics stats = Arrays.stream(ts.getValuesArray()).summaryStatistics();

                    ObjectNode statsNode = seriesNode.putObject("stats");
                    statsNode.put("min", round(stats.getMin()));
                    statsNode.put("max", round(stats.getMax()));
                    statsNode.put("avg", round(stats.getAverage()));
                    statsNode.put("count", stats.getCount());
                }
            }

            if (includeValues) {
                // preserves peaks and valleys
                queries.reduce(seriesList, maxPoints);
                for (int i = 0; i < seriesList.size(); i++) {
                    addValues((ObjectNode) seriesArray.get(i), seriesList.get(i), zone);
                }
            }

            return objectMapper.writeValueAsString(result);
        } catch (Exception e) {
            return "{\"error\": \"Failed to format response: " + e.getMessage() + "\"}";
        }
    }

    private static void addValues(ObjectNode seriesNode, TimeSeries series, ZoneId zone) {
        if (series.getPointCount() == 0) {
            return;
        }
        ArrayNode valuesArray = seriesNode.putArray("values");
        for (Value v : series.getValues()) {
            ArrayNode point = valuesArray.addArray();
            point.add(formatEpoch(v.getTimestamp(), zone));
            point.add(round(v.getValue()));
        }
    }

    private static final DateTimeFormatter DATE_AND_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * A time as the chart shows it. The zone of the computer was used once; the chart has a
     * zone of its own, and the times of the assistant were hours away from those of its axis.
     */
    static String formatEpoch(long epochMs, ZoneId zone) {
        return DATE_AND_TIME.withZone(zone).format(Instant.ofEpochMilli(epochMs));
    }

    private static double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    /**
     * Checks whether the current chart context actually has a time range (start/end).
     * Used to prevent the AI from fabricating timestamps that aren't in the UI.
     */
    private boolean contextHasTimeRange() {
        try {
            String contextJson = contextProvider.getCurrentContextJson();
            if (contextJson == null) return false;
            JsonNode ctx = objectMapper.readTree(contextJson);
            return ctx.has("start") || ctx.has("end");
        } catch (Exception e) {
            LOG.warn("Could not check chart context for time range: {}", e.getMessage());
            return false;
        }
    }

    private static boolean hasText(JsonNode node, String field) {
        return node.has(field) && !node.get(field).isNull() && !node.get(field).asText().isEmpty();
    }
}

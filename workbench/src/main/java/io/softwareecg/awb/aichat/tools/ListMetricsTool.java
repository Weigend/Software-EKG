package io.softwareecg.awb.aichat.tools;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.softwareecg.awb.queryengine.MetricDataAccessService;
import io.softwareecg.awb.timeseries.dimension.logical.Container;
import io.softwareecg.awb.timeseries.dimension.logical.Namespace;
import io.softwareecg.awb.timeseries.dimension.logical.Pod;
import io.softwareecg.awb.timeseries.dimension.logical.Service;
import io.softwareecg.awb.timeseries.dimension.physical.Host;
import io.softwareecg.awb.timeseries.dimension.physical.HostGroup;
import io.softwareecg.awb.queryengine.model.MetricQuery;
import io.softwareecg.awb.investigation.model.QuerySpec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.function.Supplier;

import io.softwareecg.awb.timeseries.dimension.NamedValueEntity;
/**
 * Tool that lists available data facets (hosts, groups, metrics, etc.)
 * from the AWB repository using facet queries.
 */
public class ListMetricsTool implements AiTool {

    private static final Logger LOG = LoggerFactory.getLogger(ListMetricsTool.class);

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Supplier<MetricDataAccessService> metricServiceSupplier;

    public ListMetricsTool(Supplier<MetricDataAccessService> metricServiceSupplier) {
        this.metricServiceSupplier = metricServiceSupplier;
    }

    @Override
    public String getName() {
        return "list_available_data";
    }

    @Override
    public String getDescription() {
        return "List available data in a project by facet. " +
                "Physical hierarchy: hostgroups → hosts → measurements → processes → processgroups → metricgroups → metrics. " +
                "Cloud-native hierarchy: namespaces → services → pods → containers → metrics. " +
                "Use the 'facet' parameter to choose what to list. Optionally filter by other dimensions to narrow results.";
    }

    @Override
    public JsonNode getParametersSchema() {
        ObjectNode schema = objectMapper.createObjectNode();
        schema.put("type", "object");
        ObjectNode props = schema.putObject("properties");

        ObjectNode facetProp = props.putObject("facet");
        facetProp.put("type", "string");
        facetProp.put("description",
                "What to list. Physical: 'hosts', 'hostgroups', 'processes', 'processgroups', " +
                "'measurements', 'metricgroups', 'metrics'. " +
                "Cloud-native: 'namespaces', 'services', 'pods', 'containers'.");
        ArrayNode facetEnum = facetProp.putArray("enum");
        // physical / classic
        facetEnum.add("hostgroups");
        facetEnum.add("hosts");
        facetEnum.add("measurements");
        facetEnum.add("processes");
        facetEnum.add("processgroups");
        facetEnum.add("metricgroups");
        facetEnum.add("metrics");
        // cloud-native / logical
        facetEnum.add("namespaces");
        facetEnum.add("services");
        facetEnum.add("pods");
        facetEnum.add("containers");

        props.putObject("project").put("type", "string")
                .put("description", "Project name (required)");
        props.putObject("host").put("type", "string")
                .put("description", "Filter by host (optional)");
        props.putObject("hostgroup").put("type", "string")
                .put("description", "Filter by host group (optional)");
        props.putObject("measurement").put("type", "string")
                .put("description", "Filter by measurement (optional)");
        props.putObject("process").put("type", "string")
                .put("description", "Filter by process (optional)");
        props.putObject("processgroup").put("type", "string")
                .put("description", "Filter by process group (optional)");
        props.putObject("metricgroup").put("type", "string")
                .put("description", "Filter by metric group (optional)");
        props.putObject("namespace").put("type", "string")
                .put("description", "Filter by namespace (optional, cloud-native)");
        props.putObject("service").put("type", "string")
                .put("description", "Filter by service (optional, cloud-native)");
        props.putObject("pod").put("type", "string")
                .put("description", "Filter by pod (optional, cloud-native)");
        props.putObject("container").put("type", "string")
                .put("description", "Filter by container (optional, cloud-native)");

        ArrayNode required = schema.putArray("required");
        required.add("project");
        required.add("facet");

        return schema;
    }

    @Override
    public String execute(String argumentsJson) {
        try {
            JsonNode args = objectMapper.readTree(argumentsJson);

            String project = safeText(args, "project");
            String facet = safeText(args, "facet");

            if (project == null || project.isBlank()) {
                return "{\"error\": \"Missing required parameter 'project'. Use get_current_chart_context to find the active project, or ask the user.\"}";
            }
            if (facet == null || facet.isBlank()) {
                return "{\"error\": \"Missing required parameter 'facet'. Must be one of: hosts, hostgroups, measurements, processes, processgroups, metricgroups, metrics, namespaces, services, pods, containers.\"}";
            }

            QuerySpec.Builder<QuerySpec> builder = new QuerySpec.Builder<>();
            builder.withProject(project);

            // Physical filter dimensions
            applyFilter(args, "host", builder::withHost);
            applyFilter(args, "hostgroup", builder::withHostGroup);
            applyFilter(args, "measurement", builder::withMeasurement);
            applyFilter(args, "process", builder::withProcess);
            applyFilter(args, "processgroup", builder::withProcessGroup);
            applyFilter(args, "metricgroup", builder::withMetricGroup);
            // Cloud-native filter dimensions
            applyFilter(args, "namespace", builder::withNamespace);
            applyFilter(args, "service", builder::withService);
            applyFilter(args, "pod", builder::withPod);
            applyFilter(args, "container", builder::withContainer);

            MetricDataAccessService service = metricServiceSupplier.get();
            if (service == null) {
                return "{\"error\": \"No repository connection available. The user must open a project first.\"}";
            }

            QuerySpec params = builder.build();
            MetricQuery query = new MetricQuery(params);

            ObjectNode result = objectMapper.createObjectNode();
            result.put("facet", facet);
            result.put("project", project);

            ArrayNode items = result.putArray("items");

            switch (facet) {
                // Physical / classic hierarchy
                case "hostgroups" -> addItems(items, result, service.getHostGroups(query));
                case "hosts" -> addItems(items, result, service.getHosts(query));
                case "measurements" -> addItems(items, result, service.getMeasurements(query));
                case "processes" -> addItems(items, result, service.getProcesses(query));
                case "processgroups" -> addItems(items, result, service.getProcessGroups(query));
                case "metricgroups" -> addItems(items, result, service.getMetricGroups(query));
                case "metrics" -> addItems(items, result, service.getMetricsNames(query));
                // Cloud-native / logical hierarchy
                case "namespaces" -> addItems(items, result, service.getNamespaces(query));
                case "services" -> addItems(items, result, service.getServices(query));
                case "pods" -> addItems(items, result, service.getPods(query));
                case "containers" -> addItems(items, result, service.getContainers(query));
                default -> throw new IllegalArgumentException("Unknown facet: " + facet);
            }

            return objectMapper.writeValueAsString(result);

        } catch (Exception e) {
            LOG.error("Error executing list_available_data tool", e);
            return "{\"error\": \"" + e.getMessage().replace("\"", "'") + "\"}";
        }
    }

    private static final int MAX_ITEMS = 200;

    private static void addItems(ArrayNode items, ObjectNode result, List<? extends NamedValueEntity> entities) {
        // The service always prepends a '*' wildcard entry (for UI dropdowns) — filter it out for the AI
        List<String> names = entities.stream()
                .map(NamedValueEntity::getValueName)
                .filter(name -> !"*".equals(name))
                .toList();
        result.put("total_count", names.size());
        if (names.size() > MAX_ITEMS) {
            result.put("truncated", true);
            result.put("returned_count", MAX_ITEMS);
            result.put("hint", "Too many results (" + names.size() + "). Only first " + MAX_ITEMS
                    + " shown. Use additional filters (host, metricgroup, measurement, process) to narrow down.");
            names = names.subList(0, MAX_ITEMS);
        }
        names.forEach(items::add);
    }

    private static void applyFilter(JsonNode args, String field, java.util.function.Consumer<String> setter) {
        String value = safeText(args, field);
        if (value != null) setter.accept(value);
    }

    private static String safeText(JsonNode node, String field) {
        JsonNode value = node.get(field);
        if (value == null || value.isNull()) return null;
        String text = value.asText();
        return text.isBlank() ? null : text;
    }
}

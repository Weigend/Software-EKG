package io.softwareecg.awb.aichat.tools;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Tool that returns the current chart context — what the user is currently
 * viewing (selected project, host, metrics, time range, etc.).
 * This gives the AI awareness of the user's current focus.
 */
public class GetCurrentContextTool implements AiTool {

    private static final Logger LOG = LoggerFactory.getLogger(GetCurrentContextTool.class);

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ChartContextProvider contextProvider;

    public GetCurrentContextTool(ChartContextProvider contextProvider) {
        this.contextProvider = contextProvider;
    }

    @Override
    public String getName() {
        return "get_current_chart_context";
    }

    @Override
    public String getDescription() {
        return "Get the currently selected parameters in the chart view — project, host, metric, " +
                "time range, measurement, and other active filters. Use this to understand what " +
                "the user is currently looking at before asking follow-up questions.";
    }

    @Override
    public JsonNode getParametersSchema() {
        ObjectNode schema = objectMapper.createObjectNode();
        schema.put("type", "object");
        schema.putObject("properties");
        return schema;
    }

    @Override
    public String execute(String argumentsJson) {
        try {
            String contextJson = contextProvider.getCurrentContextJson();
            if (contextJson == null) {
                return "{\"info\": \"No chart is currently active. Ask the user which project and host to analyze.\"}";
            }
            return contextJson;
        } catch (Exception e) {
            LOG.error("Error getting current chart context", e);
            return "{\"error\": \"" + e.getMessage().replace("\"", "'") + "\"}";
        }
    }
}

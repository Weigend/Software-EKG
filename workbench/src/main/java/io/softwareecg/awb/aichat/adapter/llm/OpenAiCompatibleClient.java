package io.softwareecg.awb.aichat.adapter.llm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.softwareecg.awb.aichat.tools.AiTool;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * HTTP client for OpenAI-compatible chat completion APIs.
 * Works with OpenAI, Ollama, LM Studio, vLLM, and any endpoint
 * that implements the /v1/chat/completions format.
 */
public class OpenAiCompatibleClient {

    private static final Logger LOG = LoggerFactory.getLogger(OpenAiCompatibleClient.class);

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final AiBackendConfig config;
    private CloseableHttpClient httpClient;

    public OpenAiCompatibleClient(AiBackendConfig config) {
        this.config = config;
        initHttpClient();
    }

    private void initHttpClient() {
        int timeoutMs = config.getTimeoutSeconds() * 1000;
        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectTimeout(10_000)
                .setConnectionRequestTimeout(timeoutMs)
                .setSocketTimeout(timeoutMs)
                .build();

        httpClient = HttpClientBuilder.create()
                .setDefaultRequestConfig(requestConfig)
                .build();
    }

    /**
     * Send a chat completion request with tools.
     *
     * @param messages conversation history
     * @param tools    available tools (may be empty)
     * @return the AI response
     */
    public ChatResponse chat(List<ChatMessage> messages, List<AiTool> tools) throws IOException {
        ObjectNode requestBody = buildRequestBody(messages, tools);

        HttpPost post = new HttpPost(config.getApiEndpoint() + "/chat/completions");
        post.setHeader("Content-Type", "application/json");
        if (config.getApiKey() != null && !config.getApiKey().isEmpty()) {
            post.setHeader("Authorization", "Bearer " + config.getApiKey());
        }
        post.setEntity(new StringEntity(objectMapper.writeValueAsString(requestBody), StandardCharsets.UTF_8));

        LOG.debug("Sending chat request to {} with {} messages", config.getApiEndpoint(), messages.size());

        try (CloseableHttpResponse response = httpClient.execute(post)) {
            int statusCode = response.getStatusLine().getStatusCode();
            String responseBody = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);

            if (statusCode >= 400) {
                LOG.error("AI API error ({}): {}", statusCode, responseBody);
                throw new IOException("AI API returned status " + statusCode + ": " + responseBody);
            }

            return parseResponse(responseBody);
        }
    }

    private ObjectNode buildRequestBody(List<ChatMessage> messages, List<AiTool> tools) {
        ObjectNode body = objectMapper.createObjectNode();
        body.put("model", config.getModel());
        body.put("max_tokens", config.getMaxTokens());

        if (config.isPromptCaching()) {
            // Automatic caching: the backend caches everything up to the end of this
            // request and reads it back on the next one. That only pays off while the
            // start of the request stays byte-identical - tools, system prompt and
            // earlier messages must not change between requests.
            body.putObject("cache_control").put("type", "ephemeral");
        }

        // messages array
        ArrayNode messagesArray = body.putArray("messages");
        for (ChatMessage msg : messages) {
            ObjectNode msgNode = messagesArray.addObject();
            msgNode.put("role", msg.getRole());

            if (msg.getContent() != null) {
                msgNode.put("content", msg.getContent());
            }

            if (msg.getToolCallId() != null) {
                msgNode.put("tool_call_id", msg.getToolCallId());
            }

            if (msg.getReasoningDetails() != null) {
                msgNode.set("reasoning_details", msg.getReasoningDetails());
            }

            if (msg.hasToolCalls()) {
                ArrayNode toolCallsArray = msgNode.putArray("tool_calls");
                for (ToolCall tc : msg.getToolCalls()) {
                    ObjectNode tcNode = toolCallsArray.addObject();
                    tcNode.put("id", tc.getId());
                    tcNode.put("type", "function");
                    ObjectNode fnNode = tcNode.putObject("function");
                    fnNode.put("name", tc.getFunctionName());
                    fnNode.put("arguments", tc.getArguments());
                }
            }
        }

        // tools array
        if (tools != null && !tools.isEmpty()) {
            ArrayNode toolsArray = body.putArray("tools");
            for (AiTool tool : tools) {
                ObjectNode toolNode = toolsArray.addObject();
                toolNode.put("type", "function");
                ObjectNode fnNode = toolNode.putObject("function");
                fnNode.put("name", tool.getName());
                fnNode.put("description", tool.getDescription());
                fnNode.set("parameters", tool.getParametersSchema());
            }
        }

        return body;
    }

    private ChatResponse parseResponse(String responseBody) throws IOException {
        JsonNode root = objectMapper.readTree(responseBody);
        JsonNode choices = root.get("choices");
        if (choices == null || choices.isEmpty()) {
            throw new IOException("No choices in AI response: " + responseBody);
        }

        JsonNode choice = choices.get(0);
        String finishReason = choice.has("finish_reason") ? choice.get("finish_reason").asText() : "stop";
        JsonNode messageNode = choice.get("message");

        String role = messageNode.get("role").asText();
        String content = messageNode.has("content") && !messageNode.get("content").isNull()
                ? messageNode.get("content").asText()
                : null;

        // parse tool calls if present
        List<ToolCall> toolCalls = null;
        if (messageNode.has("tool_calls") && messageNode.get("tool_calls").isArray()) {
            toolCalls = new ArrayList<>();
            for (JsonNode tcNode : messageNode.get("tool_calls")) {
                String id = tcNode.get("id").asText();
                JsonNode fnNode = tcNode.get("function");
                String name = fnNode.get("name").asText();
                String arguments = fnNode.get("arguments").asText();
                toolCalls.add(new ToolCall(id, name, arguments));
            }
        }

        ChatMessage message;
        if (toolCalls != null && !toolCalls.isEmpty()) {
            message = ChatMessage.assistantWithToolCalls(content, toolCalls);
        } else {
            message = ChatMessage.assistant(content != null ? content : "");
        }

        JsonNode reasoningDetails = messageNode.get("reasoning_details");
        if (reasoningDetails != null && reasoningDetails.isArray() && !reasoningDetails.isEmpty()) {
            message = message.withReasoningDetails(reasoningDetails);
        }

        logUsage(root.get("usage"));

        return new ChatResponse(message, finishReason);
    }

    /**
     * Log what the request cost in tokens. "cached" is the part of the prompt read
     * from the cache (billed at a fraction), "cache write" the part stored for the
     * next request. Both stay 0 if the backend does not cache.
     */
    private void logUsage(JsonNode usage) {
        if (usage == null || usage.isNull()) {
            return;
        }
        JsonNode details = usage.path("prompt_tokens_details");
        LOG.info("AI usage: {} prompt tokens ({} cached, {} cache write), {} completion tokens",
                usage.path("prompt_tokens").asLong(),
                details.path("cached_tokens").asLong(),
                details.path("cache_write_tokens").asLong(),
                usage.path("completion_tokens").asLong());
    }

    public void close() {
        try {
            if (httpClient != null) {
                httpClient.close();
            }
        } catch (IOException e) {
            LOG.warn("Error closing HTTP client", e);
        }
    }

    public String getModelName() {
        return config.getModel();
    }
}

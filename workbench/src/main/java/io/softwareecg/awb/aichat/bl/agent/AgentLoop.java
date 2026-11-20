package io.softwareecg.awb.aichat.bl.agent;

import io.softwareecg.awb.aichat.bl.backend.*;
import io.softwareecg.awb.aichat.bl.tools.AiTool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * ReAct-style agent loop that orchestrates conversation between the user,
 * the AI model, and the available tools.
 * <p>
 * Flow: User message → AI response → (tool call → tool result → AI response)* → final answer
 * <p>
 * The loop runs synchronously on the calling thread.
 * UI-Code wrappt den Loop bei Bedarf in einen JavaFX Task (siehe
 * io.softwareecg.awb.aichat.ui.agent.JavaFxAgentTask in awb-aichat-ui).
 */
public class AgentLoop {

    private static final Logger LOG = LoggerFactory.getLogger(AgentLoop.class);
    private static final int DEFAULT_MAX_ITERATIONS = 10;

    /**
     * Approximate maximum character budget for the conversation history.
     * When exceeded, older tool results are summarized to free up space.
     * 1 token ≈ 4 characters; 100K chars ≈ 25K tokens — leaves headroom for the model's context window.
     */
    private static final int MAX_HISTORY_CHARS = 100_000;

    /**
     * Size a compaction shrinks the history to. Well below {@link #MAX_HISTORY_CHARS},
     * so that compaction runs once in a while instead of before every request:
     * each run rewrites old messages, and the backend's prompt cache is lost from
     * the first rewritten message on.
     */
    private static final int COMPACTION_TARGET_CHARS = 60_000;

    /** Tool results longer than this are candidates for summarization. */
    private static final int TOOL_RESULT_SUMMARY_THRESHOLD = 500;

    private final OpenAiCompatibleClient client;
    private final List<AiTool> tools;
    private final Map<String, AiTool> toolsByName;
    private final String systemPrompt;
    private final int maxIterations;

    private final List<ChatMessage> conversationHistory = new ArrayList<>();

    public AgentLoop(OpenAiCompatibleClient client, List<AiTool> tools, String systemPrompt) {
        this(client, tools, systemPrompt, DEFAULT_MAX_ITERATIONS);
    }

    public AgentLoop(OpenAiCompatibleClient client, List<AiTool> tools,
                     String systemPrompt, int maxIterations) {
        this.client = client;
        this.tools = tools;
        this.toolsByName = tools.stream().collect(Collectors.toMap(AiTool::getName, t -> t));
        this.systemPrompt = systemPrompt;
        this.maxIterations = maxIterations;

        conversationHistory.add(ChatMessage.system(systemPrompt));
    }

    /**
     * Process a user message through the agent loop.
     * Executes tool calls iteratively until the AI provides a final text answer
     * or the maximum number of iterations is reached.
     *
     * @param userMessage the user's question
     * @param callback    callback for UI updates (may be null)
     * @return the final assistant text response
     */
    public String processUserMessage(String userMessage, AgentCallback callback) {
        conversationHistory.add(ChatMessage.user(userMessage));

        for (int iteration = 0; iteration < maxIterations; iteration++) {
            LOG.debug("Agent loop iteration {}/{}", iteration + 1, maxIterations);

            compactHistory();

            ChatResponse response;
            try {
                response = client.chat(conversationHistory, tools);
            } catch (Exception e) {
                String errorMsg = "Error communicating with AI backend: " + e.getMessage();
                LOG.error(errorMsg, e);
                if (callback != null) callback.onError(errorMsg, e);
                return errorMsg;
            }

            ChatMessage assistantMessage = response.getMessage();
            conversationHistory.add(assistantMessage);

            if (!response.requiresToolExecution()) {
                // Final text response
                String text = assistantMessage.getContent() != null
                        ? assistantMessage.getContent()
                        : "";
                if (callback != null) {
                    callback.onAssistantMessage(text);
                    callback.onComplete();
                }
                return text;
            }

            // AI wants to call tools — execute each one
            if (assistantMessage.getContent() != null && !assistantMessage.getContent().isEmpty()) {
                if (callback != null) callback.onAssistantMessage(assistantMessage.getContent());
            }

            for (ToolCall toolCall : assistantMessage.getToolCalls()) {
                String toolResult = executeToolCall(toolCall, callback);
                conversationHistory.add(ChatMessage.toolResult(toolCall.getId(), toolResult));
            }
        }

        // Max iterations reached
        String fallback = "I've reached the maximum number of analysis steps (" + maxIterations
                + "). Here's what I found so far based on the data I've gathered.";
        if (callback != null) {
            callback.onAssistantMessage(fallback);
            callback.onComplete();
        }
        return fallback;
    }

    private String executeToolCall(ToolCall toolCall, AgentCallback callback) {
        String name = toolCall.getFunctionName();
        String arguments = toolCall.getArguments();

        LOG.info("Executing tool: {} with args: {}", name, arguments);
        if (callback != null) callback.onToolCall(name, arguments);

        AiTool tool = toolsByName.get(name);
        if (tool == null) {
            String error = "{\"error\": \"Unknown tool: " + name + "\"}";
            LOG.warn("Unknown tool requested: {}", name);
            if (callback != null) callback.onToolResult(name, error);
            return error;
        }

        try {
            String result = tool.execute(arguments);
            LOG.debug("Tool {} returned {} chars", name, result.length());
            if (callback != null) callback.onToolResult(name, result);
            return result;
        } catch (Exception e) {
            String error = "{\"error\": \"Tool execution failed: " + e.getMessage().replace("\"", "'") + "\"}";
            LOG.error("Tool {} failed", name, e);
            if (callback != null) callback.onToolResult(name, error);
            return error;
        }
    }

    /** Get the full conversation history (for debugging or persistence). */
    public List<ChatMessage> getConversationHistory() {
        return List.copyOf(conversationHistory);
    }

    /** Clear conversation history and start fresh (keeps system prompt). */
    public void reset() {
        conversationHistory.clear();
        conversationHistory.add(ChatMessage.system(systemPrompt));
    }

    /**
     * Compacts the conversation history when it exceeds {@link #MAX_HISTORY_CHARS},
     * down to {@link #COMPACTION_TARGET_CHARS}.
     * Strategy: iterate from oldest to newest and replace large tool-result messages
     * with a short summary containing only the first line / size info.
     * System, user, and the most recent 4 messages are never touched.
     */
    private void compactHistory() {
        int totalChars = conversationHistory.stream()
                .mapToInt(m -> m.getContent() != null ? m.getContent().length() : 0)
                .sum();

        if (totalChars <= MAX_HISTORY_CHARS) return;

        LOG.info("Conversation history exceeds {} chars (current: {}), compacting old tool results",
                MAX_HISTORY_CHARS, totalChars);

        // Never touch the last 4 messages (current tool call/result pairs)
        int protectedTail = Math.min(4, conversationHistory.size());
        int compactableEnd = conversationHistory.size() - protectedTail;

        for (int i = 1; i < compactableEnd && totalChars > COMPACTION_TARGET_CHARS; i++) {
            ChatMessage msg = conversationHistory.get(i);
            if (!"tool".equals(msg.getRole())) continue;

            String content = msg.getContent();
            if (content == null || content.length() <= TOOL_RESULT_SUMMARY_THRESHOLD) continue;

            // Replace with a compact summary
            String summary = "[Compacted tool result, original " + content.length() + " chars]";
            conversationHistory.set(i, ChatMessage.toolResult(msg.getToolCallId(), summary));
            totalChars -= (content.length() - summary.length());
            LOG.debug("Compacted tool result at index {} ({} -> {} chars)", i, content.length(), summary.length());
        }

        // The model's reasoning is signed against the messages that preceded it. Now
        // that those have changed, the backend may reject it - so it goes, all of it.
        conversationHistory.replaceAll(m -> m.getReasoningDetails() != null ? m.withReasoningDetails(null) : m);

        LOG.info("After compaction: {} chars", totalChars);
    }
}

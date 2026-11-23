package io.softwareecg.awb.aichat.adapter.llm;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.List;

/**
 * Represents a single message in a chat conversation.
 * Supports roles: system, user, assistant, tool.
 */
public class ChatMessage {

    private final String role;
    private final String content;
    private final List<ToolCall> toolCalls;
    private final String toolCallId;
    private final JsonNode reasoningDetails;

    private ChatMessage(String role, String content, List<ToolCall> toolCalls, String toolCallId) {
        this(role, content, toolCalls, toolCallId, null);
    }

    private ChatMessage(String role, String content, List<ToolCall> toolCalls, String toolCallId,
                        JsonNode reasoningDetails) {
        this.role = role;
        this.content = content;
        this.toolCalls = toolCalls;
        this.toolCallId = toolCallId;
        this.reasoningDetails = reasoningDetails;
    }

    public static ChatMessage system(String content) {
        return new ChatMessage("system", content, null, null);
    }

    public static ChatMessage user(String content) {
        return new ChatMessage("user", content, null, null);
    }

    public static ChatMessage assistant(String content) {
        return new ChatMessage("assistant", content, null, null);
    }

    public static ChatMessage assistantWithToolCalls(String content, List<ToolCall> toolCalls) {
        return new ChatMessage("assistant", content, toolCalls, null);
    }

    public static ChatMessage toolResult(String toolCallId, String content) {
        return new ChatMessage("tool", content, null, toolCallId);
    }

    public String getRole() {
        return role;
    }

    public String getContent() {
        return content;
    }

    public List<ToolCall> getToolCalls() {
        return toolCalls;
    }

    public String getToolCallId() {
        return toolCallId;
    }

    /**
     * The model's reasoning as the backend returned it ({@code reasoning_details}),
     * or {@code null}. It is opaque: it goes back to the backend exactly as it
     * came, so that the model keeps its train of thought between tool calls.
     */
    public JsonNode getReasoningDetails() {
        return reasoningDetails;
    }

    /** This message with the given reasoning attached; {@code null} removes it. */
    public ChatMessage withReasoningDetails(JsonNode reasoningDetails) {
        return new ChatMessage(role, content, toolCalls, toolCallId, reasoningDetails);
    }

    public boolean hasToolCalls() {
        return toolCalls != null && !toolCalls.isEmpty();
    }

    @Override
    public String toString() {
        return role + ": " + (content != null ? content.substring(0, Math.min(content.length(), 100)) : "[tool_calls]");
    }
}

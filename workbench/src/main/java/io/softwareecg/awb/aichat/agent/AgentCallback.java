package io.softwareecg.awb.aichat.agent;

/**
 * Callback interface for the AI agent loop.
 * Allows the UI to react to agent events (new text, tool calls, errors).
 * All callbacks are invoked from the agent's background thread —
 * UI implementations must use Platform.runLater() as needed.
 */
public interface AgentCallback {

    /** Called when the AI produces text (may be called multiple times per turn). */
    void onAssistantMessage(String text);

    /** Called when the AI invokes a tool. */
    void onToolCall(String toolName, String arguments);

    /** Called when a tool returns its result. */
    void onToolResult(String toolName, String result);

    /** Called when the agent loop finishes (final answer delivered). */
    void onComplete();

    /** Called when an error occurs. */
    void onError(String message, Throwable cause);
}

package io.softwareecg.awb.aichat.bl.backend;

/**
 * Response from the AI backend containing either a text message
 * or a message with tool calls that need to be executed.
 */
public class ChatResponse {

    private final ChatMessage message;
    private final String finishReason;

    public ChatResponse(ChatMessage message, String finishReason) {
        this.message = message;
        this.finishReason = finishReason;
    }

    public ChatMessage getMessage() {
        return message;
    }

    public String getFinishReason() {
        return finishReason;
    }

    public boolean requiresToolExecution() {
        return "tool_calls".equals(finishReason) || message.hasToolCalls();
    }
}

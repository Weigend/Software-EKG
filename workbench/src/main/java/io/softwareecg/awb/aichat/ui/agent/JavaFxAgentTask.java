package io.softwareecg.awb.aichat.ui.agent;

import io.softwareecg.awb.aichat.agent.AgentCallback;
import io.softwareecg.awb.aichat.agent.AgentLoop;
import javafx.concurrent.Task;

/**
 * JavaFX Task wrapper for the AgentLoop. Lives in der UI-Schicht; die
 * eigentliche Agent-Logik bleibt JavaFX-frei in awb-aichat-bl.
 */
public class JavaFxAgentTask extends Task<String> {

    private final AgentLoop agentLoop;
    private final String userMessage;
    private final AgentCallback callback;

    public JavaFxAgentTask(AgentLoop agentLoop, String userMessage, AgentCallback callback) {
        this.agentLoop = agentLoop;
        this.userMessage = userMessage;
        this.callback = callback;
    }

    @Override
    protected String call() {
        return agentLoop.processUserMessage(userMessage, callback);
    }
}

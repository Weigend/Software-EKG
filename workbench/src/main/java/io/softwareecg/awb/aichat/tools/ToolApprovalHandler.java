package io.softwareecg.awb.aichat.tools;

/**
 * Callback interface for tools that require explicit user approval before execution.
 * Implementations must be thread-safe — the handler is called from the agent's background
 * thread and typically needs to show a UI dialog on the JavaFX application thread.
 */
@FunctionalInterface
public interface ToolApprovalHandler {

    /**
     * Request user approval for a tool action.
     * Blocks the calling thread until the user responds.
     *
     * @param toolName    the tool requesting approval
     * @param description human-readable description of the action (e.g. "Execute 'df -h' on host web01")
     * @return {@code true} if the user approved, {@code false} if denied
     */
    boolean requestApproval(String toolName, String description);
}

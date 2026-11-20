package io.softwareecg.awb.aichat.bl.tools;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Defines a tool that the AI agent can invoke to access AWB data.
 * Each tool has a name, description, JSON schema for parameters,
 * and an execute method that performs the actual data access.
 */
public interface AiTool {

    String getName();

    String getDescription();

    /**
     * JSON Schema describing the tool's parameters.
     * Must be a valid JSON Schema object node.
     */
    JsonNode getParametersSchema();

    /**
     * Execute the tool with the given JSON arguments string.
     *
     * @param argumentsJson JSON string with tool arguments
     * @return result as a string (typically JSON) to feed back to the AI
     */
    String execute(String argumentsJson);
}

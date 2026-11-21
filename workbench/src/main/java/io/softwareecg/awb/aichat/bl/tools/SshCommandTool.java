package io.softwareecg.awb.aichat.bl.tools;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.softwareecg.awb.common.ssh.SshClient;
import io.softwareecg.awb.common.ssh.SshResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

/**
 * AI tool that executes SSH commands on monitored hosts.
 * Every execution requires explicit user approval via a confirmation dialog.
 * <p>
 * Uses the "cloudadmin" user with key-based authentication (default SSH key).
 */
public class SshCommandTool implements AiTool {

    private static final Logger LOG = LoggerFactory.getLogger(SshCommandTool.class);

    private static final String SSH_USER = "cloudadmin";
    private static final int SSH_PORT = 22;
    private static final int MAX_OUTPUT_CHARS = 8000;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ToolApprovalHandler approvalHandler;

    public SshCommandTool(ToolApprovalHandler approvalHandler) {
        this.approvalHandler = approvalHandler;
    }

    @Override
    public String getName() {
        return "ssh_execute";
    }

    @Override
    public String getDescription() {
        return "Execute a shell command on a monitored host via SSH. " +
                "The user will be asked for approval before the command is executed. " +
                "Use this to inspect system state (e.g. disk usage, running processes, log files, " +
                "service status) on the servers being monitored. " +
                "Only use read-only, non-destructive commands.";
    }

    @Override
    public JsonNode getParametersSchema() {
        ObjectNode schema = objectMapper.createObjectNode();
        schema.put("type", "object");
        ObjectNode props = schema.putObject("properties");

        props.putObject("host").put("type", "string")
                .put("description", "The hostname or IP address of the target server to connect to via SSH.");
        props.putObject("command").put("type", "string")
                .put("description", "The shell command to execute on the remote host. " +
                        "Use only non-destructive, read-only commands (e.g. df, top -bn1, cat, ps, systemctl status).");

        ArrayNode required = schema.putArray("required");
        required.add("host");
        required.add("command");

        return schema;
    }

    @Override
    public String execute(String argumentsJson) {
        try {
            JsonNode args = objectMapper.readTree(argumentsJson);

            String host = hasText(args, "host") ? args.get("host").asText() : null;
            String command = hasText(args, "command") ? args.get("command").asText() : null;

            if (host == null || host.isBlank()) {
                return "{\"error\": \"Missing required parameter 'host'. Specify the hostname or IP of the target server.\"}";
            }
            if (command == null || command.isBlank()) {
                return "{\"error\": \"Missing required parameter 'command'. Specify the shell command to execute.\"}";
            }

            // Request user approval before executing
            String approvalDescription = "SSH-Kommando auf " + host + ":\n" + command;
            boolean approved = approvalHandler.requestApproval(getName(), approvalDescription);
            if (!approved) {
                LOG.info("User denied SSH command execution: {} on {}", command, host);
                return "{\"status\": \"denied\", \"message\": \"The user denied execution of this command.\"}";
            }

            LOG.info("Executing SSH command on {}: {}", host, command);
            return executeCommand(host, command);

        } catch (Exception e) {
            LOG.error("Error in ssh_execute tool", e);
            return "{\"error\": \"" + e.getMessage().replace("\"", "'") + "\"}";
        }
    }

    private String executeCommand(String host, String command) {
        File defaultKey = new File(System.getProperty("user.home"), ".ssh/id_rsa");

        try (SshClient client = new SshClient(host, SSH_PORT, SSH_USER, defaultKey, null)) {
            client.connect();
            SshResult sshResult = client.execute(command);

            String stdout = sshResult.stdout();
            String stderr = sshResult.stderr();

            ObjectNode result = objectMapper.createObjectNode();
            result.put("status", "ok");
            result.put("host", host);
            result.put("command", command);

            if (stdout != null && !stdout.isBlank()) {
                result.put("stdout", truncate(stdout.trim()));
            }
            if (stderr != null && !stderr.isBlank()) {
                result.put("stderr", truncate(stderr.trim()));
            }
            if ((stdout == null || stdout.isBlank()) && (stderr == null || stderr.isBlank())) {
                result.put("stdout", "(no output)");
            }

            return objectMapper.writeValueAsString(result);

        } catch (Exception e) {
            LOG.error("SSH command execution failed on {}: {}", host, e.getMessage(), e);
            try {
                ObjectNode error = objectMapper.createObjectNode();
                error.put("error", "SSH execution failed: " + e.getMessage());
                error.put("host", host);
                error.put("command", command);
                return objectMapper.writeValueAsString(error);
            } catch (Exception jsonEx) {
                return "{\"error\": \"SSH execution failed: " + e.getMessage().replace("\"", "'") + "\"}";
            }
        }
    }

    private static String truncate(String text) {
        if (text.length() <= MAX_OUTPUT_CHARS) {
            return text;
        }
        return text.substring(0, MAX_OUTPUT_CHARS) + "\n... (output truncated after " + MAX_OUTPUT_CHARS + " characters)";
    }

    private static boolean hasText(JsonNode node, String field) {
        return node.has(field) && !node.get(field).isNull() && !node.get(field).asText().isEmpty();
    }
}

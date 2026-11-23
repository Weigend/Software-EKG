package io.softwareecg.awb.aichat.agent;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import io.softwareecg.awb.aichat.adapter.llm.AiBackendConfig;
import io.softwareecg.awb.aichat.adapter.llm.OpenAiCompatibleClient;
import io.softwareecg.awb.aichat.tools.AiTool;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Runs the agent loop against a local stand-in for the chat completions
 * endpoint and looks at the requests it receives: what the loop sends decides
 * whether the backend can cache the prompt and whether the model keeps its
 * reasoning between tool calls.
 */
public class AgentLoopBackendTest {

    private static final ObjectMapper JSON = new ObjectMapper();

    private static final String REASONING =
            "[{\"type\":\"reasoning.text\",\"text\":\"Load the data first.\",\"signature\":\"sig-1\"}]";

    private static final String TOOL_CALL_RESPONSE = "{\"choices\":[{\"finish_reason\":\"tool_calls\",\"message\":{"
            + "\"role\":\"assistant\",\"content\":null,\"reasoning_details\":" + REASONING + ","
            + "\"tool_calls\":[{\"id\":\"call-%d\",\"type\":\"function\","
            + "\"function\":{\"name\":\"load\",\"arguments\":\"{}\"}}]}}],"
            + "\"usage\":{\"prompt_tokens\":100,\"completion_tokens\":10,"
            + "\"prompt_tokens_details\":{\"cached_tokens\":0,\"cache_write_tokens\":90}}}";

    private static final String FINAL_RESPONSE = "{\"choices\":[{\"finish_reason\":\"stop\",\"message\":{"
            + "\"role\":\"assistant\",\"content\":\"done\"}}]}";

    private final List<JsonNode> requests = new ArrayList<>();
    private final Deque<String> responses = new ArrayDeque<>();
    private HttpServer server;
    private OpenAiCompatibleClient client;

    @Before
    public void startServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/v1/chat/completions", exchange -> {
            requests.add(JSON.readTree(exchange.getRequestBody()));
            byte[] body = responses.removeFirst().getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, body.length);
            try (OutputStream out = exchange.getResponseBody()) {
                out.write(body);
            }
        });
        server.start();
    }

    @After
    public void stopServer() {
        if (client != null) {
            client.close();
        }
        server.stop(0);
    }

    private AgentLoop agentLoop(AiBackendConfig config, String toolResult) {
        config.setApiEndpoint("http://127.0.0.1:" + server.getAddress().getPort() + "/v1");
        client = new OpenAiCompatibleClient(config);
        return new AgentLoop(client, List.of(new FixedResultTool(toolResult)), "You analyse metrics.");
    }

    @Test
    public void claudeOnOpenRouterAsksForPromptCaching() {
        responses.add(FINAL_RESPONSE);

        agentLoop(AiBackendConfig.openRouter("key", "anthropic/claude-sonnet-5.5"), "{}")
                .processUserMessage("hello", null);

        assertEquals("ephemeral", requests.get(0).path("cache_control").path("type").asText());
    }

    @Test
    public void otherBackendsAreNotSentTheCachingField() {
        responses.add(FINAL_RESPONSE);
        responses.add(FINAL_RESPONSE);

        agentLoop(AiBackendConfig.openRouter("key", "openai/gpt-4o"), "{}").processUserMessage("hello", null);
        client.close();
        agentLoop(AiBackendConfig.ollama("llama3.1"), "{}").processUserMessage("hello", null);

        assertFalse(requests.get(0).has("cache_control"));
        assertFalse(requests.get(1).has("cache_control"));
    }

    @Test
    public void reasoningGoesBackUnchangedWithTheToolResult() throws IOException {
        responses.add(String.format(TOOL_CALL_RESPONSE, 1));
        responses.add(FINAL_RESPONSE);

        String answer = agentLoop(AiBackendConfig.openRouter("key", "anthropic/claude-sonnet-5.5"), "{}")
                .processUserMessage("how is the cpu?", null);

        assertEquals("done", answer);
        JsonNode assistantTurn = requests.get(1).path("messages").get(2);
        assertEquals("assistant", assistantTurn.path("role").asText());
        assertEquals(JSON.readTree(REASONING), assistantTurn.path("reasoning_details"));
    }

    /**
     * The cache holds a prefix of the request. Each request of a conversation
     * must therefore start with exactly what the previous one consisted of.
     */
    @Test
    public void eachRequestStartsWithThePreviousOne() {
        responses.add(String.format(TOOL_CALL_RESPONSE, 1));
        responses.add(String.format(TOOL_CALL_RESPONSE, 2));
        responses.add(FINAL_RESPONSE);

        agentLoop(AiBackendConfig.openRouter("key", "anthropic/claude-sonnet-5.5"), "{\"value\":1}")
                .processUserMessage("how is the cpu?", null);

        assertEquals(3, requests.size());
        for (int i = 1; i < requests.size(); i++) {
            JsonNode previous = requests.get(i - 1);
            JsonNode current = requests.get(i);
            assertEquals(previous.path("tools"), current.path("tools"));
            for (int m = 0; m < previous.path("messages").size(); m++) {
                assertEquals("message " + m + " of request " + i,
                        previous.path("messages").get(m), current.path("messages").get(m));
            }
        }
    }

    /**
     * Compaction rewrites old tool results. Reasoning produced before that is
     * bound to the messages as they were, so none of it may be sent afterwards.
     */
    @Test
    public void compactionShrinksWellBelowTheLimitAndDropsReasoning() {
        String largeResult = "x".repeat(25_000);
        for (int i = 1; i <= 4; i++) {
            responses.add(String.format(TOOL_CALL_RESPONSE, i));
        }
        responses.add(FINAL_RESPONSE);

        agentLoop(AiBackendConfig.openRouter("key", "anthropic/claude-sonnet-5.5"), largeResult)
                .processUserMessage("how is the cpu?", null);

        JsonNode beforeCompaction = requests.get(3).path("messages");
        assertTrue(beforeCompaction.get(2).has("reasoning_details"));

        // Four results of 25,000 characters and the prompts exceed the limit of 100,000.
        JsonNode afterCompaction = requests.get(4).path("messages");
        int chars = 0;
        for (JsonNode message : afterCompaction) {
            chars += message.path("content").asText("").length();
            assertNull(message.get("reasoning_details"));
        }
        assertTrue("history after compaction: " + chars + " chars", chars <= 60_000);
    }

    /** A tool that takes no arguments and always returns the same text. */
    private static final class FixedResultTool implements AiTool {

        private final String result;

        private FixedResultTool(String result) {
            this.result = result;
        }

        @Override
        public String getName() {
            return "load";
        }

        @Override
        public String getDescription() {
            return "Loads the data.";
        }

        @Override
        public JsonNode getParametersSchema() {
            return JSON.createObjectNode().put("type", "object");
        }

        @Override
        public String execute(String argumentsJson) {
            return result;
        }
    }
}

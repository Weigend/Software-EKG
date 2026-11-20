package io.softwareecg.awb.aichat.bl.backend;

/**
 * Configuration for an AI backend connection.
 * Supports OpenAI, Anthropic, Ollama, and any OpenAI-compatible endpoint.
 */
public class AiBackendConfig {

    private String apiEndpoint;
    private String apiKey;
    private String model;
    private int timeoutSeconds;
    private int maxTokens;
    private boolean promptCaching;

    public AiBackendConfig() {
        this.apiEndpoint = "http://localhost:11434/v1";
        this.model = "llama3.1";
        this.apiKey = "";
        this.timeoutSeconds = 120;
        this.maxTokens = 4096;
    }

    public static AiBackendConfig ollama(String model) {
        AiBackendConfig config = new AiBackendConfig();
        config.apiEndpoint = "http://localhost:11434/v1";
        config.model = model;
        return config;
    }

    public static AiBackendConfig openai(String apiKey, String model) {
        AiBackendConfig config = new AiBackendConfig();
        config.apiEndpoint = "https://api.openai.com/v1";
        config.apiKey = apiKey;
        config.model = model;
        return config;
    }

    public static AiBackendConfig anthropic(String apiKey, String model) {
        AiBackendConfig config = new AiBackendConfig();
        config.apiEndpoint = "https://api.anthropic.com/v1";
        config.apiKey = apiKey;
        config.model = model;
        return config;
    }

    /**
     * OpenRouter.ai — OpenAI-compatible proxy for Claude, GPT-4o, and other models.
     * Models: "anthropic/claude-sonnet-5.5", "openai/gpt-4o", "google/gemini-2.0-flash", etc.
     * <p>
     * Prompt caching is switched on for Claude models: they bill a cached prompt
     * prefix at a tenth of the input price, but only when the request asks for it.
     */
    public static AiBackendConfig openRouter(String apiKey, String model) {
        AiBackendConfig config = new AiBackendConfig();
        config.apiEndpoint = "https://openrouter.ai/api/v1";
        config.apiKey = apiKey;
        config.model = model;
        config.promptCaching = model != null && model.startsWith("anthropic/");
        return config;
    }

    public String getApiEndpoint() {
        return apiEndpoint;
    }

    public void setApiEndpoint(String apiEndpoint) {
        this.apiEndpoint = apiEndpoint;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public int getTimeoutSeconds() {
        return timeoutSeconds;
    }

    public void setTimeoutSeconds(int timeoutSeconds) {
        this.timeoutSeconds = timeoutSeconds;
    }

    public int getMaxTokens() {
        return maxTokens;
    }

    public void setMaxTokens(int maxTokens) {
        this.maxTokens = maxTokens;
    }

    /**
     * Whether requests ask the backend to cache the prompt prefix (tools, system
     * prompt and the conversation so far) by sending a top-level
     * {@code cache_control}. Only for backends that know the field: OpenRouter
     * with Claude models. Off by default.
     */
    public boolean isPromptCaching() {
        return promptCaching;
    }

    public void setPromptCaching(boolean promptCaching) {
        this.promptCaching = promptCaching;
    }
}

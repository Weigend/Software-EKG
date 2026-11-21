package io.softwareecg.awb.aichat.ui;

import io.softwareecg.awb.aichat.bl.backend.AiBackendConfig;
import io.softwareecg.awb.common.ui.view.EkgView;
import io.softwareecg.awb.common.module.EkgModule;
import io.softwareecg.awb.common.module.ModuleException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * Module that registers the AI Chat sidebar panel in the AWB.
 */
public class AiChatModule implements EkgModule {

    private static final Logger LOG = LoggerFactory.getLogger(AiChatModule.class);

    private EkgView<AiChatController> chatView;

    @Override
    public void preload() throws ModuleException {
        try {
            chatView = new EkgView.Builder<AiChatController>()
                    .withId("ekg:ai:chat")
                    .withTitle("AI Assistant")
                    .withPos(EkgView.Position.RIGHT)
                    .withFile(AiChatController.class.getResource("AiChatPanel.fxml"))
                    .withViewAreaSize(0.20)
                    .build();

            // Initialize the agent — choose one:
            // Local Ollama: AiBackendConfig.ollama("qwen3:8b-q4_K_M")
            // OpenRouter:   AiBackendConfig.openRouter(System.getenv("OPENROUTER_API_KEY"), "anthropic/claude-sonnet-5.5")
            // OpenAI:       AiBackendConfig.openai(System.getenv("OPENAI_API_KEY"), "gpt-4o")
            AiBackendConfig config = AiBackendConfig.openRouter(
                    System.getenv("OPENROUTER_API_KEY"),
                    "anthropic/claude-sonnet-5.5");
            // Claude Sonnet 5.5 always thinks before it answers, and the thinking
            // counts against max_tokens together with the visible answer.
            config.setMaxTokens(16_000);

            chatView.getController().initAgent(config);

            LOG.info("AI Chat module loaded successfully");
        } catch (IOException e) {
            throw new ModuleException(e);
        }
    }

    @Override
    public void start() {
        // View registration with WindowManager happens in the application module
    }

    @Override
    public void stop() {
        // cleanup if needed
    }

    public EkgView<AiChatController> getChatView() {
        return chatView;
    }
}

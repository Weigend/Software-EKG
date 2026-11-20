package io.softwareecg.awb.aichat.ui;

import io.softwareecg.awb.common.ui.view.ViewMapper;
import io.softwareecg.awb.sdk.core.log.EkgLogger;
import io.softwareecg.wfx.lookup.api.Lookup;
import io.softwareecg.wfx.platform.api.Module;
import io.softwareecg.wfx.windowmanager.api.FXMLView;
import io.softwareecg.wfx.windowmanager.api.Position;
import io.softwareecg.wfx.windowmanager.api.WindowManager;
import jakarta.inject.Singleton;
import org.slf4j.Logger;

/**
 * Application-level wrapper that registers the AI Chat panel
 * with the WFX WindowManager on the RIGHT side.
 */
@Singleton
@SuppressWarnings("unused")
public class EkgAiChatModule extends AiChatModule implements Module {

    private static final Logger LOGGER = EkgLogger.get();

    @Override
    public void preload() {
        try {
            super.preload();
        } catch (Exception e) {
            LOGGER.warn("AI Chat module failed to preload: {}", e.getMessage());
        }
    }

    @Override
    public void start() {
        if (getChatView() == null) {
            LOGGER.warn("AI Chat view not available, skipping registration");
            return;
        }

        WindowManager windowManager = Lookup.lookup(WindowManager.class);
        FXMLView<AiChatController> view = new ViewMapper<AiChatController>()
                .convert(getChatView(), Position.RIGHT);
        windowManager.register(view);
        LOGGER.info("AI Chat panel registered with WindowManager");
    }

    @Override
    public void stop() {
        super.stop();
    }
}

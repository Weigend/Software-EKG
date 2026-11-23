package io.softwareecg.awb.explorer.ui;

import io.softwareecg.awb.common.ui.explorer.ExplorerController;
import io.softwareecg.awb.common.ui.view.ViewMapper;
import io.softwareecg.awb.runtime.module.ModuleException;
import io.softwareecg.awb.sdk.core.log.EkgLogger;
import io.softwareecg.wfx.lookup.api.Lookup;
import io.softwareecg.wfx.platform.api.Module;
import io.softwareecg.wfx.windowmanager.api.FXMLView;
import io.softwareecg.wfx.windowmanager.api.Position;
import io.softwareecg.wfx.windowmanager.api.WindowManager;
import jakarta.annotation.Priority;
import jakarta.inject.Singleton;
import org.slf4j.Logger;

/**
 * Application-level wrapper that registers the Explorer panel with WFX.
 */
@Singleton
@Priority(100)
@SuppressWarnings("unused")
public class EkgExplorerModule extends ExplorerModule implements Module {

    private static final Logger LOGGER = EkgLogger.get();

    @Override
    public void preload() {
        try {
            super.preload();
        } catch (ModuleException e) {
            LOGGER.info(e.getMessage());
        }
    }

    @Override
    public void start() {
        WindowManager windowManager = Lookup.lookup(WindowManager.class);
        FXMLView<ExplorerController> view = new ViewMapper<ExplorerController>()
                .convert(explorerView, Position.LEFT);
        windowManager.register(view);
    }
}

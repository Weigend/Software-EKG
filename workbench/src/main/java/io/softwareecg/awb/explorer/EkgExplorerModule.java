package io.softwareecg.awb.explorer;

import io.softwareecg.awb.repository.api.AddRepositoryRequest;
import io.softwareecg.awb.uitoolkit.tree.ExplorerController;
import io.softwareecg.awb.uitoolkit.tree.RootItem;
import io.softwareecg.awb.uitoolkit.view.ViewMapper;
import io.softwareecg.awb.runtime.module.ModuleException;
import io.softwareecg.awb.runtime.logging.EkgLogger;
import io.softwareecg.wfx.lookup.api.Lookup;
import io.softwareecg.wfx.platform.api.EventBus;
import io.softwareecg.wfx.platform.api.Module;
import io.softwareecg.wfx.windowmanager.api.ApplicationWindow;
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
    @SuppressWarnings("unchecked")
    public void start() {
        WindowManager windowManager = Lookup.lookup(WindowManager.class);
        FXMLView<ExplorerController> view = new ViewMapper<ExplorerController>()
                .convert(explorerView, Position.LEFT);
        windowManager.register(view);

        // what the dialog does is the business of the repositories
        Lookup.lookup(RootItem.class).addContextMenuEntry("Add EKG Repository", e ->
                Lookup.lookup(EventBus.class).publish(new AddRepositoryRequest(this,
                        Lookup.lookup(ApplicationWindow.class).getStage())));
    }
}

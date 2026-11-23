package io.softwareecg.awb.repository.ui;

import io.softwareecg.awb.application.api.EkgApplicationController;
import io.softwareecg.awb.common.ui.explorer.ExplorerController;
import io.softwareecg.awb.common.ui.explorer.RootItem;
import io.softwareecg.awb.runtime.module.EkgModule;
import io.softwareecg.awb.repository.ui.admin.EkgRepositoryAdmin;
import io.softwareecg.wfx.lookup.api.Lookup;
import io.softwareecg.wfx.platform.api.Module;
import io.softwareecg.wfx.windowmanager.api.ApplicationWindow;
import jakarta.annotation.Priority;
import jakarta.inject.Singleton;
import javafx.scene.control.MenuItem;
import javafx.stage.Stage;

/**
 * Application-level wrapper that contributes repository administration actions.
 */
@Singleton
@Priority(300)
@SuppressWarnings("unused")
public class EkgRepositoryAdminModule implements EkgModule, Module {

    @Override
    public void preload() {
    }

    @Override
    public void start() {
        EkgApplicationController appController = Lookup.lookup(EkgApplicationController.class);
        MenuItem addRepository = new MenuItem("Add Repository...");
        addRepository.setMnemonicParsing(false);
        addRepository.setOnAction(e -> {
            EkgRepositoryAdmin ekgRepositoryAdmin = new EkgRepositoryAdmin();
            ekgRepositoryAdmin.initOwner(appController.getStage());
            ekgRepositoryAdmin.showAndWait();
        });
        appController.addRepositoryMenuItemFirst(addRepository);

        Stage appStage = Lookup.lookup(ApplicationWindow.class).getStage();

        RootItem rootItem = (RootItem) Lookup.lookup(ExplorerController.class).getRootItem();
        rootItem.addContextMenuEntry("Add EKG Repository", e -> {
            EkgRepositoryAdmin ekgRepositoryAdmin = new EkgRepositoryAdmin();
            ekgRepositoryAdmin.setTitle("Add EKG Repository");
            ekgRepositoryAdmin.initOwner(appStage);
            ekgRepositoryAdmin.show();
        });
    }

    @Override
    public void stop() {
    }
}

package io.softwareecg.awb.repository.ui;

import io.softwareecg.awb.application.api.EkgApplicationController;
import io.softwareecg.awb.runtime.module.EkgModule;
import io.softwareecg.awb.repository.ui.admin.EkgRepositoryAdmin;
import io.softwareecg.wfx.lookup.api.Lookup;
import io.softwareecg.wfx.platform.api.Module;
import jakarta.annotation.Priority;
import jakarta.inject.Singleton;
import javafx.scene.control.MenuItem;

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
    }

    @Override
    public void stop() {
    }
}

package io.softwareecg.awb.project.ui;

import io.softwareecg.awb.application.api.EkgApplicationController;
import io.softwareecg.awb.project.ui.mgrdialog.CreateProjectDialog;
import io.softwareecg.awb.project.ui.projectbar.ProjectBar;
import io.softwareecg.wfx.lookup.api.Lookup;
import io.softwareecg.wfx.platform.api.Module;
import jakarta.annotation.Priority;
import jakarta.inject.Singleton;
import javafx.scene.control.MenuItem;

/**
 * Application-level wrapper that contributes project UI elements to the shell.
 */
@Singleton
@Priority(250)
@SuppressWarnings("unused")
public class EkgProjectModule implements Module {

    @Override
    public void preload() {
    }

    @Override
    public void start() {
        EkgApplicationController appController = Lookup.lookup(EkgApplicationController.class);
        appController.addTopNode(new ProjectBar());

        MenuItem createProject = new MenuItem("Create Collector Project");
        createProject.setMnemonicParsing(false);
        createProject.setOnAction(e -> {
            CreateProjectDialog createProjectDialog = new CreateProjectDialog();
            createProjectDialog.initOwner(appController.getStage());
            createProjectDialog.showAndWait();
        });
        appController.addImportMenuItem(createProject);
    }

    @Override
    public void stop() {
    }
}

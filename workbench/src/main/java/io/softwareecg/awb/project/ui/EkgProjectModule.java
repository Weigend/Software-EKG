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

    /** The text of the item in the menu <b>Repository</b>. */
    public static final String ADD_PROJECT = "Add Project...";

    @Override
    public void preload() {
    }

    @Override
    public void start() {
        EkgApplicationController appController = Lookup.lookup(EkgApplicationController.class);
        appController.addTopNode(new ProjectBar());

        // In the menu of the repositories: the dialog adds a project of a remote repository to
        // this workbench. It imports nothing and creates nothing in the repository.
        MenuItem addProject = new MenuItem(ADD_PROJECT);
        addProject.setMnemonicParsing(false);
        addProject.setOnAction(e -> {
            CreateProjectDialog createProjectDialog = new CreateProjectDialog();
            createProjectDialog.initOwner(appController.getStage());
            createProjectDialog.showAndWait();
        });
        appController.addRepositoryMenuItem(addProject);
    }

    @Override
    public void stop() {
    }
}

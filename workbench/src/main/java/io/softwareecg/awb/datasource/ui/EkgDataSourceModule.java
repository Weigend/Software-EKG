package io.softwareecg.awb.datasource.ui;

import io.softwareecg.awb.application.api.EkgApplicationController;
import io.softwareecg.awb.datasource.NewAnalysisService;
import io.softwareecg.awb.projectcontext.api.ShowProjectDataSourcesEvent;
import io.softwareecg.wfx.lookup.api.Lookup;
import io.softwareecg.wfx.platform.api.EventBus;
import io.softwareecg.wfx.platform.api.Module;
import jakarta.annotation.Priority;
import jakarta.inject.Singleton;
import javafx.application.Platform;
import javafx.scene.control.MenuItem;
import javafx.scene.input.KeyCombination;

import java.util.EventObject;
import java.util.HashMap;
import java.util.Map;

/**
 * Adds two entries to the Analysis menu: "New Analysis...", the way to get from a Prometheus
 * environment to current data in the AWB, and "Data Sources...", where the sources of all
 * analyses are watched and managed afterwards.
 */
@Singleton
@Priority(240)
@SuppressWarnings("unused") // discovered by the platform
public class EkgDataSourceModule implements Module {

    private DataSourcesDialog dataSourcesDialog;

    /** The open data sources dialogs of single projects, by project name. */
    private final Map<String, DataSourcesDialog> projectDialogs = new HashMap<>();

    @Override
    public void preload() {
    }

    @Override
    public void start() {
        EkgApplicationController appController = Lookup.lookup(EkgApplicationController.class);

        MenuItem newAnalysis = new MenuItem("New Analysis...");
        newAnalysis.setMnemonicParsing(false);
        newAnalysis.setAccelerator(KeyCombination.keyCombination("Shortcut+N"));
        newAnalysis.setOnAction(e -> {
            NewAnalysisDialog dialog = new NewAnalysisDialog(Lookup.lookup(NewAnalysisService.class));
            dialog.initOwner(appController.getStage());
            dialog.showAndWait();
        });
        appController.addAnalysisMenuItem(newAnalysis);

        MenuItem dataSources = new MenuItem("Data Sources...");
        dataSources.setMnemonicParsing(false);
        dataSources.setOnAction(e -> showDataSources(appController));
        appController.addAnalysisMenuItem(dataSources);

        // "Data Sources..." at a project, and the click on its state in the project bar
        EventBus<EventObject> eventBus = Lookup.lookup(EventBus.class);
        eventBus.subscribe(ShowProjectDataSourcesEvent.class, event -> {
            String projectName = ((ShowProjectDataSourcesEvent) event).getProjectName();
            Platform.runLater(() -> showDataSourcesOf(projectName, appController));
            return true;
        });
    }

    /** Brings the one data sources dialog to the front, or opens it. */
    private void showDataSources(EkgApplicationController appController) {
        if (dataSourcesDialog != null && dataSourcesDialog.isShowing()) {
            ((javafx.stage.Stage) dataSourcesDialog.getDialogPane().getScene().getWindow()).toFront();
            return;
        }
        dataSourcesDialog = new DataSourcesDialog(Lookup.lookup(NewAnalysisService.class));
        dataSourcesDialog.initOwner(appController.getStage());
        dataSourcesDialog.show();
    }

    /** Brings the data sources dialog of the project to the front, or opens it. */
    private void showDataSourcesOf(String projectName, EkgApplicationController appController) {
        DataSourcesDialog open = projectDialogs.get(projectName);
        if (open != null && open.isShowing()) {
            ((javafx.stage.Stage) open.getDialogPane().getScene().getWindow()).toFront();
            return;
        }
        DataSourcesDialog dialog = new DataSourcesDialog(Lookup.lookup(NewAnalysisService.class), projectName);
        dialog.initOwner(appController.getStage());
        projectDialogs.put(projectName, dialog);
        dialog.show();
    }

    @Override
    public void stop() {
    }
}

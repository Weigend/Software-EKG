package io.softwareecg.awb.sdk.importer.ui.dialogs.queryimport;

import io.softwareecg.awb.sdk.awbapi.project.ProjectSelector;
import io.softwareecg.awb.timeseries.port.TimeSeriesImportSource;
import io.softwareecg.awb.sdk.awbapi.repository.ResourceAuthType;
import io.softwareecg.awb.sdk.core.resourceloader.CdiFxmlLoader;
import io.softwareecg.awb.sdk.importer.tasks.LocalProjectImportStrategy;
import io.softwareecg.awb.sdk.importer.tasks.SplitSourceProjectImportStrategy;
import io.softwareecg.awb.sdk.importer.ui.dialogs.ImportDialog;
import javafx.beans.property.ReadOnlyProperty;

/**
 * todo enter comment here
 *
 * @since 14.07.2018
 */
public class QueryImportDialog extends ImportDialog {

    private final QueryImportDialogController controller;

    public QueryImportDialog() {
        controller = CdiFxmlLoader.loadView("QueryImportDialogView.fxml", this).getController();
    }

    @Override
    public void updateState() {
        controller.getProjectSelector().updateState();
        controller.getRepositorySelector().updateState();
        controller.getQuerySourceSelector().updateState();
    }

    @Override
    public ProjectSelector getProjectSelector() {
        return controller.getProjectSelector();
    }

    public ReadOnlyProperty<TimeSeriesImportSource> getImportSourceRepositoryProperty() {
        return controller.getQuerySourceSelector().getImportSourceRepositoryProperty();
    }

    @Override
    public void setLocalImportStrategy(LocalProjectImportStrategy localProjectImportStrategy) {
        controller.setLocalImportStrategy(localProjectImportStrategy);
    }

    @Override
    public void setRemoteImportStrategy(SplitSourceProjectImportStrategy splitSourceProjectImportStrategy) {
        controller.setRemoteImportStrategy(splitSourceProjectImportStrategy);
    }

    public void setSupportedAuthTypes(ResourceAuthType... supportedAuthTypes) {
        controller.setSupportedAuthTypes(supportedAuthTypes);
    }
}

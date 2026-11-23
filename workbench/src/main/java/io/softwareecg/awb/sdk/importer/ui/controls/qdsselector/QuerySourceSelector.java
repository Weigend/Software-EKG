package io.softwareecg.awb.sdk.importer.ui.controls.qdsselector;

import io.softwareecg.awb.timeseries.port.TimeSeriesImportSource;
import io.softwareecg.awb.sdk.awbapi.repository.Repository;
import io.softwareecg.awb.sdk.awbapi.repository.ResourceAuthType;
import io.softwareecg.awb.sdk.core.resourceloader.CdiFxmlLoader;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyProperty;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.scene.layout.GridPane;

/**
 * An UI component that provides an configuration area
 * with multiple layers that can used to specify an new remote
 * types for data import or selecting an existing one.
 *
 * @since 28.07.2018
 */
public class QuerySourceSelector extends GridPane {

    private final QuerySourceSelectorController controller;

    public QuerySourceSelector() {
        controller = CdiFxmlLoader.loadView("QuerySourceSelectorView.fxml", this).getController();
    }

    /**
     * Returns an ObjectProperty used to hold a EkgRepository instance
     * used to read and store projects.
     *
     * @return an ObjectProperty used to hold a types
     */
    public ObjectProperty<Repository> getRepositoryProperty() {
        return controller.getViewModel().selectedRepositoryProperty();
    }

    public ReadOnlyProperty<TimeSeriesImportSource> getImportSourceRepositoryProperty() {
        return controller.getViewModel().getImportSourceRepositoryProperty();
    }

    /**
     * @return the address of the server as it stands in its field
     */
    public ReadOnlyStringProperty repositoryUrlProperty() {
        return controller.getViewModel().repositoryUrlProperty();
    }

    public void disableViewReset(boolean isDisabled) {
        controller.disableViewReset(isDisabled);
    }


    public void updateState() {
        controller.clearView();
    }

    public void submitChanges() {
        controller.submitChanges();
    }

    public void setSupportedAuthTypes(ResourceAuthType... supportedAuthTypes) {
        controller.setSupportedAuthTypes(supportedAuthTypes);
    }

    public void setProjectName(String projectName, boolean isNewProject) {
        // order is important because setProjectName will result in an update of an property-change-listener
        controller.getViewModel().isNewProject(isNewProject);
        controller.getViewModel().setProjectName(projectName);
    }

    public double getLabelWidth() {
        return controller.labelWidthProperty().get();
    }

    public DoubleProperty labelWidthProperty() {
        return controller.labelWidthProperty();
    }

    public void setLabelWidth(double labelWidth) {
        this.controller.labelWidthProperty().set(labelWidth);
    }

    public double getRowHeight() {
        return controller.getRowHeight();
    }

    public DoubleProperty rowHeightProperty() {
        return controller.rowHeightProperty();
    }

    public void setRowHeight(double rowHeight) {
        this.controller.setRowHeight(rowHeight);
    }
}

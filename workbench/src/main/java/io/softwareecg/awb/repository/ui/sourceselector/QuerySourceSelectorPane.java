package io.softwareecg.awb.repository.ui.sourceselector;

import io.avaje.inject.Prototype;
import io.softwareecg.awb.repository.api.QuerySourceSelector;
import io.softwareecg.awb.repository.api.Repository;
import io.softwareecg.awb.repository.api.ResourceAuthType;
import io.softwareecg.awb.repository.api.TimeSeriesImportSource;
import io.softwareecg.awb.runtime.resources.CdiFxmlLoader;
import jakarta.annotation.Priority;
import javafx.beans.property.Property;
import javafx.beans.property.ReadOnlyProperty;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.scene.Node;
import javafx.scene.layout.GridPane;

@Priority(100)
@Prototype
public class QuerySourceSelectorPane extends GridPane implements QuerySourceSelector {

    private final QuerySourceSelectorController controller;

    public QuerySourceSelectorPane() {
        controller = CdiFxmlLoader.loadView("QuerySourceSelectorView.fxml", this).getController();
    }

    @Override
    public Property<Repository> getRepositoryProperty() {
        return controller.getViewModel().selectedRepositoryProperty();
    }

    @Override
    public ReadOnlyProperty<TimeSeriesImportSource> getImportSourceRepositoryProperty() {
        return controller.getViewModel().getImportSourceRepositoryProperty();
    }

    @Override
    public ReadOnlyStringProperty repositoryUrlProperty() {
        return controller.getViewModel().repositoryUrlProperty();
    }

    @Override
    public void updateState() {
        controller.clearView();
    }

    @Override
    public void submitChanges() {
        controller.submitChanges();
    }

    @Override
    public void setSupportedAuthTypes(ResourceAuthType... supportedAuthTypes) {
        controller.setSupportedAuthTypes(supportedAuthTypes);
    }

    @Override
    public void setProjectName(String projectName, boolean isNewProject) {
        // order is important because setProjectName will result in an update of an property-change-listener
        controller.getViewModel().isNewProject(isNewProject);
        controller.getViewModel().setProjectName(projectName);
    }

    @Override
    public void setLabelWidth(double labelWidth) {
        controller.setLabelWidth(labelWidth);
    }

    @Override
    public void setRowHeight(double rowHeight) {
        controller.setRowHeight(rowHeight);
    }

    @Override
    public Node asJfxComponent() {
        return this;
    }
}

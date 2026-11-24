package io.softwareecg.awb.importer.ui.controls;

import io.softwareecg.awb.projectcontext.api.ProjectSelector;
import io.softwareecg.awb.projectcontext.api.ProjectTimeSeriesType;
import io.softwareecg.awb.projectcontext.api.TargetProjectConfig;
import io.softwareecg.awb.projectcontext.spi.AliasMappingProvider;
import io.softwareecg.awb.repository.api.Repository;
import io.softwareecg.wfx.lookup.api.Lookup;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.Property;
import javafx.beans.value.ObservableValue;
import javafx.scene.Node;
import javafx.scene.layout.BorderPane;

/**
 * todo insert comment here
 *
 * @since 13.02.2020
 */
public class ProjectSelectorProxy extends BorderPane implements ProjectSelector {

    private ProjectSelector projectSelector;

    public ProjectSelectorProxy() {
        initialize();
    }

    public void initialize() {
        projectSelector = Lookup.lookup(ProjectSelector.class);

        if (projectSelector == null) {
            throw new IllegalStateException("Expect a ProjectSelector instance in classpath.");
        }

        this.setCenter(projectSelector.asJfxComponent());
    }

    public ProjectSelector getProjectSelector() {

        if (projectSelector == null) {
            projectSelector = Lookup.lookup(ProjectSelector.class);

            if (projectSelector == null) {
                throw new IllegalStateException("Expect a ProjectSelector instance in classpath.");
            }
        }

        return projectSelector;
    }

    @Override
    public void setCloudFlavorSupportEnabled(boolean cloudSupportEnabled) {
        getProjectSelector().setCloudFlavorSupportEnabled(cloudSupportEnabled);
    }

    @Override
    public void setImporterId(String importerId) {
        getProjectSelector().setImporterId(importerId);
    }

    @Override
    public void setSupportedTimeSeriesType(ProjectTimeSeriesType supportedProjectType) {
        getProjectSelector().setSupportedTimeSeriesType(supportedProjectType);
    }

    @Override
    public void setClassicFlavorSupportEnabled(boolean isEnabled) {
        getProjectSelector().setClassicFlavorSupportEnabled(isEnabled);
    }

    @Override
    public void setImporterAliasMappingProvider(AliasMappingProvider importerSpecificDimensionMappingProvider) {
        getProjectSelector().setImporterAliasMappingProvider(importerSpecificDimensionMappingProvider);
    }

    @Override
    public void enableSplitSourceProjectSupport(boolean isSplitSourceSupported) {
        getProjectSelector().enableSplitSourceProjectSupport(isSplitSourceSupported);
    }

    @Override
    public void enableLocalProjectSupport(boolean hasLocalProjectSupport) {
        getProjectSelector().enableLocalProjectSupport(hasLocalProjectSupport);
    }

    @Override
    public void updateState() {
        getProjectSelector().updateState();
    }

    @Override
    public void preselectProject(String projectName) {
        getProjectSelector().preselectProject(projectName);
    }

    @Override
    public void submitChanges() {
        getProjectSelector().submitChanges();
    }

    @Override
    public Property<Repository> getRepositoryProperty() {
        return getProjectSelector().getRepositoryProperty();
    }

    @Override
    public BooleanBinding isValidProjectDefinitionProperty() {
        return getProjectSelector().isValidProjectDefinitionProperty();
    }

    @Override
    public TargetProjectConfig getTargetProjectConfiguration() {
        return getProjectSelector().getTargetProjectConfiguration();
    }

    @Override
    public ObservableValue<TargetProjectConfig> getProjectConfigurationProperty() {
        return getProjectSelector().getProjectConfigurationProperty();
    }

    @Override
    public Node asJfxComponent() {
        return this;
    }
}

package io.softwareecg.awb.projectcontext.api;

import io.softwareecg.awb.projectcontext.spi.AliasMappingProvider;
import io.softwareecg.awb.projectcontext.model.ProjectTimeSeriesType;
import io.softwareecg.awb.projectcontext.model.TargetProjectConfig;
import io.softwareecg.awb.repository.api.Repository;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.Property;
import javafx.beans.value.ObservableValue;
import javafx.scene.Node;

/**
 * An interface that represents an UI selector component that the EKG user
 * takes to define the target project of the time series data.
 * This can either the definition of the new project or the selection of an existing one.
 *
 * Beside the selection and querying which project is selected the API of this interface allows both
 * sides (importer and AWB) to configure which project properties are supported or not.
 *
 * For example an importer can explicitly disable the cloud flavor support.
 *
 * @since 30.01.2020
 */
public interface ProjectSelector {

    /**
     * Set a boolean that indicate that the ProjectSelector provides the option to configure new
     * cloud projects/select existing cloud project or not
     *
     * @param cloudSupportEnabled TRUE if cloud typed projects available in the ProjectSelector or FALSE if not
     */
    void setCloudFlavorSupportEnabled(boolean cloudSupportEnabled);

    /**
     * Sets a boolean flag if the imported data will support classic view flavor or not.
     * This will stored in the project settings if new created.
     *
     * @param classicSupportEnabled TRUE if the the imported data provides required data for class view flavor
     */
    void setClassicFlavorSupportEnabled(boolean classicSupportEnabled);

    /**
     * Sets the unique importer id that will assigned to the project if this is new created
     * during the import procedure.
     * The importer id has to be global unique (at least for an specific EKG repository).
     * It can either be an generated string or a readable name.
     * A good patter is to use Maven group-id + artifact id.
     *
     * @param importerId the unique id of the import module
     */
    void setImporterId(String importerId);

    /**
     * Set the ProjectTimeSeriesType that defines if the time series initially imported to a project
     * can overwritten or extended by other (every) importers or are only suitable to specific importer.
     *
     * @param supportedProjectType the supported project series type
     */
    void setSupportedTimeSeriesType(ProjectTimeSeriesType supportedProjectType);

    /**
     * Set the an {@link AliasMappingProvider} instance that will serve domain specific labels for the
     * filter dimension. This labels will assigned at creation time of the project an can't changed
     * later by the user or importer.
     *
     * @param importerSpecificDimensionMappingProvider an instance of {@link AliasMappingProvider}
     */
    void setImporterAliasMappingProvider(AliasMappingProvider importerSpecificDimensionMappingProvider);

    /**
     * Set's a boolean flag to configure if the project (if new created) will support/use
     * split source data handling or not.
     * Split source data handling means, the metadata of the time series will be imported to the EKG repository
     * and can used to search specific time series but the series payload will fetched on demand from the remote
     * repository.
     *
     * @param isSplitSourceSupported a boolean value that indicates if split source mode is supported or not
     */
    void enableSplitSourceProjectSupport(boolean isSplitSourceSupported);

    /**
     * Set's a boolean flag to configure if the project (if new created) will support local data handling
     * that means the series metadata and the payload are stored in the EKG repository and retrieved from it.
     * This is the standard case of Software-EKG for the most analytic scenarios.
     *
     * @param hasLocalProjectSupport  a boolean value that indicates if full local data handling is supported or not
     */
    void enableLocalProjectSupport(boolean hasLocalProjectSupport);

    /**
     * Triggers that component to update all it internal states and the according view representation.
     *
     * This method can use to enforce view updates to make changes visible that normally doesn't
     * trigger view state updates.
     */
    void updateState();

    /**
     * Pre-selects an existing project by name. Switches to "existing project" mode
     * and selects the matching project in the project list.
     *
     * @param projectName the name of the project to pre-select, or null for no-op
     */
    default void preselectProject(String projectName) {
        // default no-op for backward compatibility
    }

    /**
     * Submit (writes) a configured project properties a new project instance to the repository.
     * Calling this method then the user has selected an existing project won't have any effect.
     *
     * This method has to call at every import procedure to ensure that the loosely coupled ProjectView model
     * that belongs to the imported time series data is persisted.
     */
    void submitChanges();

    /**
     * Returns a JavaFX property the component uses to store the configured repository the project belongs
     * to/will written to. Users of this API can use the property to register listeners that do something
     * than the user select another repository.
     *
     * @return a JavaFX property instance that holds a reference to the selected EKG repository
     */
    Property<Repository> getRepositoryProperty();

    /**
     * Returns a JavaFx BooleanBinding that service the information if the configured project is valid or not.
     * This binding can used to control if other UI control for example the "Ok" button to start the import are
     * enabled or not.
     *
     * @return a boolean binding that serve the information if the configured project data is valid or not.
     */
    BooleanBinding isValidProjectDefinitionProperty();

    /**
     * Returns an information object that stores some of the essential information that are required
     * by the importers to fulfill the series metadata and chose the correct import behaviour.
     * The {@link TargetProjectConfig} is not similar (no subset or parent) of the {@link ProjectView}. It only
     * shares the project name property.
     *
     * @return a {@link TargetProjectConfig} instance with the configured settings
     */
    TargetProjectConfig getTargetProjectConfiguration();

    /**
     * Returns a JavaFX property the component uses to temporarily store the configuration of project
     * import settings.
     *
     * @return a TargetProjectConfig instance
     */
    ObservableValue<TargetProjectConfig> getProjectConfigurationProperty();

    /**
     * Returns a reference to the JavaFX Node instance that should be
     * the technical base of each ProjectSelector UI component.
     * The Node reference can used to embed the component in other JavaFX components.
     *
     * @return a JavaFX Node reference to the selector component
     */
    Node asJfxComponent();
}

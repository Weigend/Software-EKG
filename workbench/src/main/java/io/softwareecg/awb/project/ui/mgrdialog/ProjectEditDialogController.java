package io.softwareecg.awb.project.ui.mgrdialog;

import io.softwareecg.awb.project.api.ProjectDataAccessService;
import io.softwareecg.awb.project.api.model.Project;
import io.softwareecg.awb.repository.api.EkgRepository;
import io.softwareecg.awb.sdk.awbapi.explorer.ExplorerUpdateEvent;
import io.softwareecg.wfx.platform.api.EventBus;
import io.softwareecg.awb.sdk.core.lookup.ServiceDiscovery;
import jakarta.inject.Inject;
import io.softwareecg.awb.sdk.importer.ui.controls.qdsselector.QuerySourceSelector;
import javafx.fxml.FXML;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.layout.BorderPane;
import java.util.EventObject;

/**
 * The controller of the ProjectEditDialogController UI component that includes
 * the whole UI logic of the dialog like managing it's state and interactions.
 *
 * @since 24.11.2018
 */
public class ProjectEditDialogController extends ProjectDialogController {

    //-----------------------------------------------------------------------------------------------------------------
    // FXML view components
    //-----------------------------------------------------------------------------------------------------------------

    @FXML
    private QuerySourceSelector querySourceSelector;

    @FXML
    private BorderPane bpSourceSelectorDisableOverlay;


    //-----------------------------------------------------------------------------------------------------------------
    // members the controller needs for state and event handling
    //-----------------------------------------------------------------------------------------------------------------

    /**
     * EKG event bus used to notify other about new
     * repositories or types modifications
     */
    @Inject
    private EventBus<EventObject> eventBus;

    private final ProjectEditDialogModel viewModel;


    //================================================================================================================
    //  Constructor
    //================================================================================================================

    public ProjectEditDialogController() {
        super(new ProjectEditDialogModel());
        this.viewModel = (ProjectEditDialogModel) super.viewModel;
    }

    //================================================================================================================
    //  Controller API
    //================================================================================================================

    /**
     * Sets the Dialog this controller belongs to an manages the
     * child components.
     *
     * @param projectMgrDialog the Dialog that own the components controlled by this controller
     */
    public void setParent(Dialog projectMgrDialog) {
        projectMgrDialog.getDialogPane().lookupButton(ButtonType.APPLY)
                .disableProperty().bind(viewModel.isValidProjectNameProperty().not());
    }

    /**
     * Persist a new specified project to types if necessary.
     * If user select an existing project nothing will happened.
     *
     * @param buttonType the type of the pressed button
     * @return the created types
     */
    public Project persistProject(ButtonType buttonType) {

        // reset state if not clicked the apply button
        if (buttonType != ButtonType.APPLY) {
            resetViewStates();
            return null;
        }

        ProjectDataAccessService projectDataAccess =
                ServiceDiscovery.lookup(ProjectDataAccessService.class, viewModel.getSelectedRepository());

        Project projectToUpdate = viewModel.getProject();
        // projectToUpdate.setName(viewModel.getProjectName()); - not supported yet
        projectToUpdate.setProjectFlavor(viewModel.getProjectFlavor());
        projectToUpdate.setCloudPlatformType(viewModel.getCloudPlatformType().toString());

        Project updatedProject = projectDataAccess.updateExistingProject(projectToUpdate);

        if (viewModel.hasImporterSourceRepositoryProperty().get()) {
            querySourceSelector.submitChanges();
        }

        eventBus.publish(new ExplorerUpdateEvent(this, updatedProject));
        resetViewStates();
        return updatedProject;
    }

    public void setRepository(EkgRepository repository) {
        viewModel.setSelectedRepository(repository);
        querySourceSelector.getRepositoryProperty().set(viewModel.getSelectedRepository());
    }

    public void setProject(Project project) {
        viewModel.setEditedProject(project);

        if (project == null) {
            return;
        }

        txtProjectName.setText(project.getName());
        cbCloudPlatformType.setValue(project.getCloudPlatformType());
        cbProjectFlavor.setValue(project.getProjectFlavor());

        querySourceSelector.setProjectName(viewModel.getProjectName(), false);
        updateProjectNames(viewModel.getSelectedRepository(), viewModel.getProjectName());
    }

    //================================================================================================================
    // internal initializing logic
    //================================================================================================================

    @Override
    protected void initializeViewState() {
        super.initializeViewState();
        querySourceSelector.disableViewReset(true);
    }

    @Override
    protected void initializeViewBehavior() {
        super.initializeViewBehavior();
        bpSourceSelectorDisableOverlay.visibleProperty().bind(viewModel.hasImporterSourceRepositoryProperty().not());

        txtProjectName.setStyle("-fx-text-fill: #a4a4a4");
        txtProjectName.focusedProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue) {
                querySourceSelector.requestFocus();
            }
        });
    }
}

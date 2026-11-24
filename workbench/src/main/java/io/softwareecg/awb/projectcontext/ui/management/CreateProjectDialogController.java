package io.softwareecg.awb.projectcontext.ui.management;

import io.softwareecg.awb.projectcontext.api.ProjectDataAccessService;
import io.softwareecg.awb.projectcontext.api.Project;
import io.softwareecg.awb.repository.api.RepositorySelector;
import io.softwareecg.awb.runtime.events.ExplorerUpdateEvent;
import io.softwareecg.wfx.platform.api.EventBus;
import io.softwareecg.awb.runtime.discovery.ServiceDiscovery;
import jakarta.inject.Inject;
import javafx.fxml.FXML;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.layout.BorderPane;
import java.util.EventObject;

/**
 * The controller of the CreateProjectDialog UI component that includes
 * the whole UI logic of the dialog like managing it's state and interactions.
 *
 * @since 21.06.2018
 */
public class CreateProjectDialogController extends ProjectDialogController {


    //-----------------------------------------------------------------------------------------------------------------
    // FXML view components

    /** The place of the repository selector in the dialog. */
    @FXML
    private BorderPane bpRepositorySelector;

    //-----------------------------------------------------------------------------------------------------------------

    @Inject
    private RepositorySelector repositorySelector;

    /**
     * EKG event bus used to notify other about new
     * repositories or types modifications
     */
    @Inject
    private EventBus<EventObject> eventBus;


    private final CreateProjectDialogModel viewModel;


    //================================================================================================================
    //  Constructor
    //================================================================================================================

    public CreateProjectDialogController() {
        super(new CreateProjectDialogModel());
        this.viewModel = (CreateProjectDialogModel) super.viewModel;
    }

    //================================================================================================================
    //  Controller API that could be used by the FileImportDialog component or deriving implementations
    //================================================================================================================

    /**
     * Sets the Dialog this controller belongs to an manages the
     * child components.
     *
     * @param createProjectDialog the Dialog that own the components controlled by this controller
     */
    public void setParent(Dialog createProjectDialog) {
        createProjectDialog.getDialogPane().lookupButton(ButtonType.APPLY)
                .disableProperty().bind(viewModel.isValidProjectConfigProperty().not());
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

        Project newProject = projectDataAccess.persistProject(viewModel.getConfiguredProject());

        eventBus.publish(new ExplorerUpdateEvent(this, newProject));
        resetViewStates();
        return newProject;
    }

    //================================================================================================================
    //  internal implementation of controller functionality
    //================================================================================================================

    @Override
    protected void initializeModelToView() {
        super.initializeModelToView();
        viewModel.selectedRepositoryProperty().bind(repositorySelector.selectedRepositoryProperty());
    }

    @Override
    protected void initializeViewState() {
        super.initializeViewState();
        repositorySelector.setLabelWidth(140);
        bpRepositorySelector.setCenter(repositorySelector.asJfxComponent());
        updateProjectNames(repositorySelector.getSelectedRepository(), null);
    }

    /**
     * Initialize all handler that belongs to this component and will
     * proceed click actions or other event handling logic.
     */
    @Override
    protected void initializeHandler() {
        repositorySelector.selectedRepositoryProperty().addListener((observable, prevRepro, selectedRepro)
                -> updateProjectNames(selectedRepro, null));

        repositorySelector.setFilter(repository -> !repository.isEmbedded());
    }

}

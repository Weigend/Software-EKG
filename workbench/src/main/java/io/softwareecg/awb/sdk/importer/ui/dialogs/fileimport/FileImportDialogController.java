package io.softwareecg.awb.sdk.importer.ui.dialogs.fileimport;

import io.softwareecg.awb.sdk.awbapi.project.ProjectSelector;
import io.softwareecg.awb.sdk.awbapi.repository.RepositorySelector;
import io.softwareecg.awb.sdk.awbapi.repository.SeriesImportService;
import io.softwareecg.awb.sdk.core.lookup.ServiceDiscovery;
import io.softwareecg.awb.sdk.importer.tasks.LocalProjectImportStrategy;
import io.softwareecg.awb.sdk.importer.tasks.LocalProjectImportTask;
import io.softwareecg.awb.sdk.importer.tasks.SplitSourceProjectImportStrategy;
import io.softwareecg.awb.sdk.importer.ui.controls.fileselector.LabeledFileSelector;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

/**
 * The default controller implementation for FileImportDialogs.
 * Wires RepositorySelector, ProjectSelector and LabeledFileSelector together
 * and trigger LocalProjectImportTasks if all selectors have na valid selection
 * and the user pressed "ok" button.
 *
 * @since 15.07.2018
 */
@SuppressWarnings("unused")  // intellij isn't able to resolve all injections
public class FileImportDialogController implements Initializable {

    /**
     * The modal ImportDialog itself that contain all UI components
     * controls by this controller as child's
     */
    @FXML
    private FileImportDialog importDialog;

    /**
     * The label in the header section that will display a static
     * description the ImportDialog is used for.
     */
    @FXML
    private Label headerLabel;

    /**
     * Component used to select the types used as persistence layer for
     * the whole project and according time-series data
     */
    @FXML
    protected RepositorySelector repositorySelector;

    /**
     * The project selector we will use to define the project
     * the series will imported to
     */
    @FXML
    protected ProjectSelector projectSelector;

    /**
     * The default file selector that can select single files or
     * directories.
     */
    @FXML
    protected LabeledFileSelector defaultFileSelector;

    /**
     * The submit button of the import dialog that will trigger
     * the import process
     */
    @FXML
    protected Button btnOK;

    /**
     * The button to cancel the import
     */
    @FXML
    protected Button btnCancel;

    /**
     * A group of UI components that can filled by derived instances
     * of FileImportDialog (in default case this is an empty group)
     */
    @FXML
    protected VBox vbExtendedChilds;

    /**
     * The import strategy that will invoked/started if the user
     * clicks "ok" and has specified all necessary setting.
     */
    protected LocalProjectImportStrategy localProjectImportStrategy = null;


    //================================================================================================================
    //  Initializable interface implementation
    //================================================================================================================

    /* (non-Javadoc)
     * @see javafx.fxml.Initializable#initialize(java.net.URL, java.util.ResourceBundle)
     */
    @Override
    public void initialize(URL location, ResourceBundle resources) {

        // bind header description text to data from outside
        headerLabel.textProperty().bind(importDialog.getHeaderDescriptionProperty());

        // init default behaviour (validation, filter, ...0) and binding
        // before calling methods overwritten by deriving classes
        initViewBehavior();

        // init event handlers
        initHandler();
    }


    //================================================================================================================
    //  Controller API that could be used by the FileImportDialog component or deriving implementations
    //================================================================================================================

    public ProjectSelector getProjectSelector() {
       return projectSelector;
    }

    public RepositorySelector getRepositorySelector() {
        return repositorySelector;
    }

    public void setLocalImportStrategy(LocalProjectImportStrategy localProjectImportStrategy) {
       this.localProjectImportStrategy = localProjectImportStrategy;
    }

    public void setRemoteImportStrategy(SplitSourceProjectImportStrategy splitSourceProjectImportStrategy) {
       throw new IllegalStateException("Remote Project support isn't supported by FileImport for performance reasons.");
    }

    public List<String> getSelectedFilesPaths() {
        return defaultFileSelector.getSelectedFileProperty();
    }

    //================================================================================================================
    //  helper logic to handle the state/behavior of this component that can optional overwritten by deriving classes
    //================================================================================================================

    /**
     * initialize the action handler of this controller
     */
    protected void initHandler() {
        this.btnOK.setOnAction(this::handleOkClick);
        this.btnCancel.setOnAction(this::closeWindow);
    }

    /**
     * Defines the binding for disabled status of Dialog buttons and wires
     * the RepositorySelector to ProjectSelector
     */
    protected void initViewBehavior() {

        // bindProjectName types selection to project selector to enable its functionality
        this.projectSelector.getRepositoryProperty().bind(repositorySelector.selectedRepositoryProperty());

        preInitOkButtonDisabledState();
    }

    /**
     * Binds the disable state of the ok button to all UI components that must be valid
     * configured before the user can submit the button (otherwise it will disabled).
     */
    protected void preInitOkButtonDisabledState() {
        btnOK.disableProperty().bind(
                projectSelector.isValidProjectDefinitionProperty().not()
                        .or(repositorySelector.selectedRepositoryProperty().isNull())
                        .or(defaultFileSelector.isValidFileSelectedProperty().not()));
    }

    /**
     * Handle the click action than a user clicks on the Ok button
     * to import the selected files
     *
     * @param actionEvent the action event fired by the button
     */
    protected void handleOkClick(ActionEvent actionEvent) {
        closeWindow(actionEvent);
        projectSelector.submitChanges();

        if (localProjectImportStrategy != null) {
            LocalProjectImportTask task = localProjectImportStrategy.createImportTask(
                    ServiceDiscovery.lookup(SeriesImportService.class, repositorySelector.getSelectedRepository()),
                    projectSelector.getTargetProjectConfiguration());

            task.start();
        }
    }

    /**
     * Closes the FileImportDialog without any further action
     *
     * @param event close event
     */
    protected void closeWindow(ActionEvent event) {
        Node button = (Node) event.getSource();
        Stage stage = (Stage) button.getScene().getWindow();
        stage.close();
    }

}

package io.softwareecg.awb.importer.ui.dialogs.queryimport;

import io.softwareecg.awb.projectcontext.api.ProjectSelector;
import io.softwareecg.awb.projectcontext.api.TargetProjectConfig;
import io.softwareecg.awb.repository.api.QuerySourceSelector;
import io.softwareecg.awb.repository.api.RepositorySelector;
import io.softwareecg.awb.repository.api.ResourceAuthType;
import io.softwareecg.awb.repository.api.TimeSeriesWriteService;
import io.softwareecg.awb.runtime.logging.EkgLogger;
import io.softwareecg.awb.runtime.discovery.ServiceDiscovery;
import io.softwareecg.awb.importer.framework.LocalProjectImportStrategy;
import io.softwareecg.awb.importer.framework.LocalProjectImportTask;
import io.softwareecg.awb.importer.framework.RemoteProjectImportTask;
import io.softwareecg.awb.importer.framework.SplitSourceProjectImportStrategy;
import jakarta.inject.Inject;
import javafx.beans.binding.Bindings;
import javafx.beans.property.StringProperty;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;

import java.net.URL;
import java.util.ResourceBundle;
import java.util.function.Supplier;

/**
 * Controller that manages the view of the QueryImportDialog.
 * It controls the behaviour, the style and action handlers to implement
 * any kind of interaction the view component provides.
 *
 * @since 13.07.2018
 */
public class QueryImportDialogController implements Initializable {

    /**
     * The logger used by the controller to log errors and special events
     */
    private static final Logger LOGGER = EkgLogger.get();

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
    @Inject
    protected RepositorySelector repositorySelector;

    /** The place of the repository selector in the dialog. */
    @FXML
    protected BorderPane bpRepositorySelector;

    /**
     * The project selector we will use to define the project
     * the series will imported to
     */
    @Inject
    protected ProjectSelector projectSelector;

    /** The place of the project selector in the dialog. */
    @FXML
    protected BorderPane bpProjectSelector;

    /**
     * The query source selector that will use to define
     * the access properties for the remote database
     */
    @Inject
    protected QuerySourceSelector querySourceSelector;

    /** The place of the source selector in the dialog. */
    @FXML
    protected BorderPane bpQuerySourceSelector;

    /**
     * The ok button that will submit/save the settings
     * and starts the import.
     */
    @FXML
    protected Button btnOK;

    /**
     * The cancel button that will close the dialog without
     * execute any changes.
     */
    @FXML
    protected Button btnCancel;

    /**
     * Label for the delta import checkbox (left column).
     * Visibility is controlled together with deltaImportBox.
     */
    @FXML
    protected Label lblDeltaImport;

    /**
     * Container for the delta import checkbox.
     * Visibility is controlled based on whether the selected project is new or existing.
     */
    @FXML
    protected HBox deltaImportBox;

    /**
     * Checkbox that allows the user to choose delta import mode.
     * Only visible when importing to an existing project (not a new one).
     * When checked, only data since the last import will be fetched.
     */
    @FXML
    protected CheckBox chkDeltaImport;

    /**
     * ComboBox to select the import time range.
     * Options: 1d, 2d, 3d, 5d, 10d, All
     */
    @FXML
    protected ComboBox<String> cmbImportRange;

    /**
     * Gives the strategy of an import that is started when the user
     * clicks "ok" and has specified all necessary setting.
     */
    protected Supplier<LocalProjectImportStrategy> localProjectImportStrategy = null;

    /**
     * Gives the strategy of an import that is started when the user
     * clicks "ok" and has specified all necessary setting.
     */
    protected Supplier<SplitSourceProjectImportStrategy> splitSourceProjectImportStrategy = null;


    //================================================================================================================
    //  Initializable interface implementation
    //================================================================================================================

    /* (non-Javadoc)
     * @see javafx.fxml.Initializable#initialize(java.net.URL, java.util.ResourceBundle)
     */
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        repositorySelector.setLabelWidth(240);
        bpRepositorySelector.setCenter(repositorySelector.asJfxComponent());
        bpProjectSelector.setCenter(projectSelector.asJfxComponent());
        bpQuerySourceSelector.setCenter(querySourceSelector.asJfxComponent());

        // init pseudo classes and other view styles that
        // will setup programmatically
        initViewStyles();

        // init default behaviour (validation, filter, ...0) and binding
        // before calling methods overwritten by deriving classes
        initViewBehavior();

        // update view from model properties
        initModelToView();

        // init event handlers
        initHandler();
    }

    //================================================================================================================
    //  Controller API that could be used by the FileImportDialog component or deriving implementations
    //================================================================================================================

    public void setSupportedAuthTypes(ResourceAuthType... supportedAuthTypes) {
        querySourceSelector.setSupportedAuthTypes(supportedAuthTypes);
    }

    public ProjectSelector getProjectSelector() {
        return projectSelector;
    }


    /** @return the description in the header of the dialog */
    public StringProperty headerTextProperty() {
        return headerLabel.textProperty();
    }

    public RepositorySelector getRepositorySelector() {
        return repositorySelector;
    }

    public QuerySourceSelector getQuerySourceSelector() {
        return querySourceSelector;
    }

    public void setLocalImportStrategy(Supplier<LocalProjectImportStrategy> localProjectImportStrategy) {
        this.localProjectImportStrategy = localProjectImportStrategy;
    }

    public void setRemoteImportStrategy(Supplier<SplitSourceProjectImportStrategy> splitSourceProjectImportStrategy) {
        this.splitSourceProjectImportStrategy = splitSourceProjectImportStrategy;
    }


    //================================================================================================================
    // internal logic to initialize and control the view
    //================================================================================================================

    /**
     * Initialize action handler that will invoked with according events
     * than a user clicks some of the UI component like buttons.
     */
    private void initHandler() {
        this.btnOK.setOnAction(this::handleOkClick);
        this.btnCancel.setOnAction(this::closeWindow);
    }

    /**
     * Initialize the view components with data by binding the model data to it
     * or set it directly.
     */
    private void initModelToView() {
        // Populate import time range ComboBox
        if (cmbImportRange != null) {
            cmbImportRange.getItems().addAll("1d", "2d", "3d", "5d", "10d", TargetProjectConfig.IMPORT_RANGE_ALL);
            cmbImportRange.setValue(TargetProjectConfig.IMPORT_RANGE_ALL);
        }

        // Disable import range ComboBox when delta import is selected
        if (chkDeltaImport != null && cmbImportRange != null) {
            chkDeltaImport.selectedProperty().addListener((obs, wasSelected, isSelected) -> {
                cmbImportRange.setDisable(isSelected);
            });
        }
    }

    /**
     * Initialize the view component behaviour like enable/disable switching
     * or or any other bindings.
     */
    private void initViewBehavior() {
        this.projectSelector.getRepositoryProperty().bind(repositorySelector.selectedRepositoryProperty());
        this.querySourceSelector.getRepositoryProperty().bind(repositorySelector.selectedRepositoryProperty());

        projectSelector.getProjectConfigurationProperty().addListener((observable, oldValue, newProjectConfig) -> {
            if (newProjectConfig == null) {
                this.querySourceSelector.setProjectName(null,
                        projectSelector.getTargetProjectConfiguration() != null &&
                                projectSelector.getTargetProjectConfiguration().isNewProject());
                updateDeltaImportVisibility(true);
            } else {
                this.querySourceSelector.setProjectName(newProjectConfig.getProjectName(),
                        projectSelector.getTargetProjectConfiguration().isNewProject());
                updateDeltaImportVisibility(projectSelector.getTargetProjectConfiguration().isNewProject());
            }
        });

        // an import needs a project to go to and a server to come from
        this.btnOK.disableProperty().bind(
                projectSelector.isValidProjectDefinitionProperty().not()
                        .or(repositorySelector.selectedRepositoryProperty().isNull())
                        .or(Bindings.createBooleanBinding(
                                () -> StringUtils.isBlank(querySourceSelector.repositoryUrlProperty().get()),
                                querySourceSelector.repositoryUrlProperty())));
    }

    /**
     * Updates the visibility of the Delta Import checkbox based on whether
     * the selected project is a new project or an existing one.
     * Delta import is only available for existing projects.
     *
     * @param isNewProject true if a new project is being created
     */
    private void updateDeltaImportVisibility(boolean isNewProject) {
        if (deltaImportBox != null && chkDeltaImport != null) {
            boolean showDelta = !isNewProject;
            deltaImportBox.setVisible(showDelta);
            deltaImportBox.setManaged(showDelta);
            if (lblDeltaImport != null) {
                lblDeltaImport.setVisible(showDelta);
                lblDeltaImport.setManaged(showDelta);
            }
            if (isNewProject) {
                chkDeltaImport.setSelected(false);
            }
        }
    }

    /**
     * Initialize the view styles that have to be configured programmatically.
     * (default styling should be define by style sheets)
     */
    private void initViewStyles() {

    }

    /**
     * Handle the click action than a user clicks on the Ok button
     * to import the selected files
     *
     * @param actionEvent the action event fired by the button
     */
    protected void handleOkClick(ActionEvent actionEvent) {

        projectSelector.submitChanges();
        querySourceSelector.submitChanges();

        // Capture config in a local variable to ensure the delta flag is set
        // on the SAME instance that gets passed to the import task.
        // getTargetProjectConfiguration() may return a new instance on each call.
        TargetProjectConfig config = projectSelector.getTargetProjectConfiguration();

        // Set delta import flag on the project configuration
        if (chkDeltaImport != null && config != null && !config.isNewProject()) {
            config.setDeltaImport(chkDeltaImport.isSelected());
            LOGGER.info("Delta import flag set to: {}", chkDeltaImport.isSelected());
        }

        // Set import time range from ComboBox
        if (cmbImportRange != null && config != null) {
            String selectedRange = cmbImportRange.getValue();
            config.setImportRange(selectedRange != null ? selectedRange : TargetProjectConfig.IMPORT_RANGE_ALL);
            LOGGER.info("Import range set to: {}", config.getImportRange());
        }

        closeWindow(actionEvent);

        boolean useSplitSource = config != null && config.useSplitSource();

        if (!useSplitSource) {
            if (localProjectImportStrategy == null) {
                LOGGER.error("Local project is chosen but no local import strategy is available.");
                return;
            }

            LocalProjectImportTask task = new LocalProjectImportTask(
                    ServiceDiscovery.lookup(TimeSeriesWriteService.class, repositorySelector.getSelectedRepository()),
                    config, localProjectImportStrategy.get());

            task.start();
        }

        if (useSplitSource) {
            if (splitSourceProjectImportStrategy == null) {
                LOGGER.error("Remote project is chosen but no local import strategy is available.");
                return;
            }

            RemoteProjectImportTask task = new RemoteProjectImportTask(
                    repositorySelector.getSelectedRepository(), config, splitSourceProjectImportStrategy.get());

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

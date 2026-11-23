package io.softwareecg.awb.repository.ui.sourceselector;

import io.softwareecg.awb.repository.model.TimeSeriesImportSource;
import io.softwareecg.awb.repository.api.Repository;
import io.softwareecg.awb.secrets.model.ResourceAuthType;
import io.softwareecg.awb.repository.SourceRepositoryService;
import io.softwareecg.awb.runtime.logging.EkgLogger;
import io.softwareecg.awb.runtime.discovery.ServiceDiscovery;
import io.softwareecg.awb.uitoolkit.controls.EkgComboBoxListViewSkin;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import org.slf4j.Logger;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;

import static io.softwareecg.awb.secrets.model.ResourceAuthType.*;
import static io.softwareecg.awb.uitoolkit.controls.EkgComboBoxListViewSkin.CellBehaviorResource;


/**
 * This class is a controller that manages the view component that
 * will used to define the connection and credential settings for and
 * remote data-source like an import repository or a remote services API.
 *
 * @since 28.07.2018
 */
public class QuerySourceSelectorController implements Initializable {

    private static final Logger LOGGER = EkgLogger.get();

    private static final String NO_VALID_PROJECT_TEXT = "Please define an (existing/new) project first!";

    private final Map<Repository, SourceRepositoryService> sourceRepositoryServiceCache = new HashMap<>();

    @FXML
    private TextField txtRepositoryURL;


    //-------------------------------------------------------------------------
    // ui controls that belong to credential configuration
    //-------------------------------------------------------------------------

    @FXML
    private ComboBox<ResourceAuthType> cbRepositoryAuthType;

    @FXML
    private Label lbRepositoryOAuthServerUrl;

    @FXML
    private TextField txtRepositoryOAuthServerUrl;

    @FXML
    private Label lbRepositoryUserName;

    @FXML
    private TextField txtRepositoryUserName;

    @FXML
    private Label lbRepositoryApiKey;

    @FXML
    private Label lbRepositoryJwtToken;

    @FXML
    private Label lbRepositoryPassword;

    @FXML
    private PasswordField txtRepositoryPassword;

    private final DoubleProperty labelWidth = new SimpleDoubleProperty(245);

    private final DoubleProperty rowHeight = new SimpleDoubleProperty(34);

    private final Map<ResourceAuthType, BooleanProperty> supportedAuthTypesMap = Map.of(
            NONE, new SimpleBooleanProperty(false),
            USERNAME_PASSWORD, new SimpleBooleanProperty(false),
            API_KEY, new SimpleBooleanProperty(false),
            JWT_BEARER_TOKEN, new SimpleBooleanProperty(false),
            OAUTH2_CLIENT_CREDENTIAL, new SimpleBooleanProperty(false)
    );

    private final QuerySourceSelectorModel viewModel = new QuerySourceSelectorModel();

    //================================================================================================================
    //  Initializable interface implementation
    //================================================================================================================

    /* (non-Javadoc)
     * @see javafx.fxml.Initializable#initialize(java.net.URL, java.util.ResourceBundle)
     */
    @Override
    public void initialize(URL location, ResourceBundle resources) {

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
    //  controller API
    //================================================================================================================


    public double getLabelWidth() {
        return labelWidth.get();
    }

    public DoubleProperty labelWidthProperty() {
        return labelWidth;
    }

    public void setLabelWidth(double labelWidth) {
        this.labelWidth.set(labelWidth);
    }

    public double getRowHeight() {
        return rowHeight.get();
    }

    public DoubleProperty rowHeightProperty() {
        return rowHeight;
    }

    public void setRowHeight(double rowHeight) {
        this.rowHeight.set(rowHeight);
    }

    /**
     * Returns the view model that belong to the QuerySourceSelector
     * UI component
     *
     * @return the view model of this component
     */
    public QuerySourceSelectorModel getViewModel() {
        return viewModel;
    }

    /**
     * Sets the ResourceAuthType the QuerySourceSelector should enable
     * to activate all options that are supported by the module that use
     * this selector for it's configuration.
     *
     * @param supportedAuthTypes zero or more RessourceAuthType that should enabled
     */
    public void setSupportedAuthTypes(ResourceAuthType... supportedAuthTypes) {

        supportedAuthTypesMap.get(NONE).set(true);
        supportedAuthTypesMap.get(USERNAME_PASSWORD).set(true);
        supportedAuthTypesMap.get(API_KEY).set(true);
        supportedAuthTypesMap.get(JWT_BEARER_TOKEN).set(true);
        supportedAuthTypesMap.get(OAUTH2_CLIENT_CREDENTIAL).set(true);

        for (ResourceAuthType supportedAuthType : supportedAuthTypes) {
            supportedAuthTypesMap.get(supportedAuthType).set(false);
        }
    }

    /**
     * Persist a new specified query source to types if necessary.
     * If the user selects a project with an already defined query source
     * that is untouched nothing will happen.
     */
    public void submitChanges() {

        if (!viewModel.isValidProjectConfig()) {
            return;
        }

        TimeSeriesImportSource sourceDefinition = viewModel.getSourceRepository();

        if (sourceDefinition == null) {
            sourceDefinition = new TimeSeriesImportSource();
        }

        String projectName = viewModel.getProjectName();
        sourceDefinition.setAccordingProjectName(projectName);
        sourceDefinition.setRepositoryUrl(viewModel.getRepositoryUrl());
        sourceDefinition.setAuthType(viewModel.getSelectedAuthTyp());
        sourceDefinition.setUsername(viewModel.getUsername());
        sourceDefinition.setPassword(viewModel.getPassword());
        sourceDefinition.setOAuthServerUrl(viewModel.getOAuthServerUrl());

        TimeSeriesImportSource existingSourceDefinition = getRepoService().queryImportSourceRepository(projectName);
        if (sourceDefinition.equals(existingSourceDefinition)) {
            LOGGER.info("The defined source repository definition is equal to an existing one for project {}. " +
                    "Skip further actions!", projectName);
            return;
        }

        getRepoService().deleteImportSourceRepositoryByProjectName(sourceDefinition.getAccordingProjectName());
        getRepoService().persistImporterSourceRepository(sourceDefinition);
    }


    public void disableViewReset(boolean isDisable) {
        //viewResetEnabled = !isDisable;
    }


    //================================================================================================================
    // internal logic to initialize and control the view
    //================================================================================================================

    /**
     * Initialize all handler that belongs to this component and will
     * proceed click actions or other event handling logic.
     */
    private void initHandler() {

        // fetch importer source data if project name/config has changed
        viewModel.projectNameProperty().addListener((observable, oldProjectName, newProjectName) -> {

            if (newProjectName != null && newProjectName.equals(oldProjectName)) {
                return;
            }

            viewModel.setIsValidProjectName(newProjectName != null);

            if (newProjectName == null) {
                return;
            }

            handleProjectUpdate(newProjectName);
        });

        viewModel.selectedAuthTypProperty().addListener((observable, oldValue, newAuthType) -> {
            if (newAuthType == ResourceAuthType.NONE) {
                txtRepositoryUserName.setText(null);
                txtRepositoryPassword.setText(null);
                txtRepositoryOAuthServerUrl.setText(null);

            } else if (newAuthType == ResourceAuthType.API_KEY) {
                txtRepositoryUserName.setText(null);
                txtRepositoryOAuthServerUrl.setText(null);

            } else if (newAuthType == ResourceAuthType.JWT_BEARER_TOKEN) {
                txtRepositoryUserName.setText(null);
                txtRepositoryOAuthServerUrl.setText(null);

            } else if (newAuthType == ResourceAuthType.USERNAME_PASSWORD) {
                txtRepositoryOAuthServerUrl.setText(null);
            }
        });
    }

    /**
     * Initialize the view components with data by binding the model data to it
     * or set it directly.
     */
    private void initModelToView() {
        cbRepositoryAuthType.setItems(FXCollections.observableArrayList(ResourceAuthType.values()));
        cbRepositoryAuthType.setValue(ResourceAuthType.NONE);

        viewModel.repositoryUrlProperty().bind(txtRepositoryURL.textProperty());
        viewModel.selectedAuthTypProperty().bind(cbRepositoryAuthType.getSelectionModel().selectedItemProperty());
        viewModel.usernameProperty().bind(txtRepositoryUserName.textProperty());
        viewModel.passwordProperty().bind(txtRepositoryPassword.textProperty());
        viewModel.oauthServerUrlProperty().bind(txtRepositoryOAuthServerUrl.textProperty());
    }

    /**
     * Initialize the view component behaviour like enable/disable switching
     * or or any other visible state changes (like specific prompt texts) that
     * based on the underlying view data.
     */
    private void initViewBehavior() {

        // delete state every time the model dialog will open/reopen
//        querySourceSelector.layoutBoundsProperty().addListener(observable -> {
//            if (viewResetEnabled) {
//                clearView();
//            }
//        });

        // set disable an specific prompt text behaviour based on project specification
        txtRepositoryURL.setPromptText(NO_VALID_PROJECT_TEXT);
        txtRepositoryURL.disableProperty().bind(viewModel.isValidProjectConfigProperty().not());
        txtRepositoryURL.disabledProperty().addListener((observable, oldValue, isDisabled) -> {
            if (isDisabled) {
                txtRepositoryURL.setPromptText(NO_VALID_PROJECT_TEXT);
            } else {
                txtRepositoryURL.setPromptText("Please specify the URL of the data source repository");
            }
        });

        lbRepositoryOAuthServerUrl.disableProperty().bind(viewModel.selectedAuthTypProperty().isNotEqualTo(OAUTH2_CLIENT_CREDENTIAL));
        txtRepositoryOAuthServerUrl.disableProperty().bind(viewModel.selectedAuthTypProperty().isNotEqualTo(OAUTH2_CLIENT_CREDENTIAL));

        lbRepositoryJwtToken.visibleProperty().bind(viewModel.selectedAuthTypProperty().isEqualTo(JWT_BEARER_TOKEN));
        lbRepositoryApiKey.visibleProperty().bind(viewModel.selectedAuthTypProperty().isEqualTo(API_KEY));
        lbRepositoryPassword.visibleProperty().bind(
                     viewModel.selectedAuthTypProperty().isNotEqualTo(API_KEY)
                .and(viewModel.selectedAuthTypProperty().isNotEqualTo(JWT_BEARER_TOKEN)));

        lbRepositoryPassword.disableProperty().bind(viewModel.selectedAuthTypProperty().isEqualTo(NONE));
        txtRepositoryPassword.disableProperty().bind(lbRepositoryPassword.disableProperty());

        lbRepositoryUserName.disableProperty().bind(viewModel.selectedAuthTypProperty().isNotEqualTo(USERNAME_PASSWORD)
                .and(viewModel.selectedAuthTypProperty().isNotEqualTo(OAUTH2_CLIENT_CREDENTIAL)));
        txtRepositoryUserName.disableProperty().bind(lbRepositoryUserName.disableProperty());
    }

    protected void clearView() {
        txtRepositoryURL.setText(null);
        txtRepositoryUserName.setText(null);
        txtRepositoryPassword.setText(null);
        txtRepositoryOAuthServerUrl.setText(null);
        cbRepositoryAuthType.setValue(NONE);
    }

    /**
     * Initialize the view styles that have to be configured programmatically.
     * (default styling should be define by style sheets)
     */
    private void initViewStyles() {
        Map<ResourceAuthType, CellBehaviorResource> behaviorResourceMap = new HashMap<>();

        behaviorResourceMap.put(NONE, new CellBehaviorResource(
                "icons/repository-authtype-none-icon.svg", supportedAuthTypesMap.get(NONE))
        );

        behaviorResourceMap.put(API_KEY, new CellBehaviorResource(
                "icons/repository-authtype-apikey-icon.svg", supportedAuthTypesMap.get(API_KEY))
        );

        behaviorResourceMap.put(USERNAME_PASSWORD, new CellBehaviorResource(
                "icons/repository-authtype-password-icon.svg", supportedAuthTypesMap.get(USERNAME_PASSWORD))
        );

        behaviorResourceMap.put(JWT_BEARER_TOKEN, new CellBehaviorResource(
                "icons/repository-authtype-jwt-icon.svg", supportedAuthTypesMap.get(JWT_BEARER_TOKEN))
        );

        behaviorResourceMap.put(OAUTH2_CLIENT_CREDENTIAL, new CellBehaviorResource(
                "icons/repository-authtype-oauth2-icon.svg", supportedAuthTypesMap.get(OAUTH2_CLIENT_CREDENTIAL))
        );

        behaviorResourceMap.put(CERTIFICATE, new CellBehaviorResource(
                "icons/repository-authtype-certificate-icon.svg", supportedAuthTypesMap.get(OAUTH2_CLIENT_CREDENTIAL))
        );

        cbRepositoryAuthType.setSkin(new EkgComboBoxListViewSkin<>(cbRepositoryAuthType, behaviorResourceMap, NONE));
    }

    //================================================================================================================
    // internal handlers that implement the controller business logic
    //================================================================================================================

    private SourceRepositoryService getRepoService() {

        Repository repository = viewModel.selectedRepositoryProperty().get();

        if (!sourceRepositoryServiceCache.containsKey(repository)) {
            sourceRepositoryServiceCache.put(viewModel.selectedRepositoryProperty().get(),
                    ServiceDiscovery.lookup(SourceRepositoryService.class, repository));
        }

        return sourceRepositoryServiceCache.get(repository);
    }

    private void handleProjectUpdate(String newProjectName) {

        if (viewModel.isNewProject()) {
            return;
        }

        TimeSeriesImportSource importerSourceRepo = getRepoService().queryImportSourceRepository(newProjectName);

        if (importerSourceRepo == null) {
            clearView();
            return;
        }

        viewModel.setSourceRepository(importerSourceRepo);
        txtRepositoryURL.setText(importerSourceRepo.getRepositoryUrl());
        cbRepositoryAuthType.setValue(importerSourceRepo.getAuthType());

        if (importerSourceRepo.getAuthType() != NONE) {
            txtRepositoryUserName.setText(importerSourceRepo.getUsername());
            txtRepositoryPassword.setText(importerSourceRepo.getPassword());
            txtRepositoryOAuthServerUrl.setText(importerSourceRepo.getOAuthServerUrl());
        }
    }
}

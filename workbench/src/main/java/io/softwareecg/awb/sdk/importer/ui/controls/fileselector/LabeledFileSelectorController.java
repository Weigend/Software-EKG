package io.softwareecg.awb.sdk.importer.ui.controls.fileselector;

import io.softwareecg.awb.sdk.core.config.PropertyConfigStore;
import javafx.beans.property.ReadOnlyListProperty;
import javafx.css.PseudoClass;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SplitMenuButton;
import javafx.scene.control.TextField;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Window;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

/**
 * The controller of LabeledFileSelector that implements the logic to select files,
 * validate it, do error handling and handles different kinds of customization like
 * specific labels or custom defined default directories if the user open the file chooser.
 *
 * @since 26.07.2018
 */
public class LabeledFileSelectorController implements Initializable {

    private static final PseudoClass FOCUSED_PSEUDOCLASS = PseudoClass.getPseudoClass("focused");

    private static final PseudoClass INVALID_PSEUDO_CLASS = PseudoClass.getPseudoClass("invalid");

    /**
     * The path of the config key in the property file
     * used to store and fetch the last selected directory
     * of file browser dialog
     */
    private static final String DEFAULT_LOOKUP_DIR_KEY = "awb-importer.default_lookup_dir";

    /**
     * The label displayed on the left site of the file chooser to describes what
     * the user will select.
     */
    @FXML
    protected Label lblFileSelectorLabel;

    /**
     * The text field for the path
     */
    @FXML
    protected TextField txtFilePath;

    /**
     * Button to open a directory chooser dialog
     */
    @FXML
    protected SplitMenuButton btnBrowse;

    /**
     * Button to open a directory chooser dialog
     */
    @FXML
    protected MenuItem btnBrowsePath;

    /**
     * Button to open a files chooser dialog
     */
    @FXML
    protected MenuItem btnBrowseFiles;

    /**
     * Initial Directory
     */
    protected String initialDirectory;

    /**
     * Config store to persist UI config states
     */
    private final PropertyConfigStore configStore = new PropertyConfigStore();

    /**
     * the view model that holds the state of the view
     */
    private final LabeledFileSelectorModel viewModel = createViewModel();


    //================================================================================================================
    //  Initializable interface implementation
    //================================================================================================================

    /* (non-Javadoc)
     * @see javafx.fxml.Initializable#initialize(java.net.URL, java.util.ResourceBundle)
     */
    @Override
    public void initialize(URL location, ResourceBundle resources) {

        initialDirectory = configStore.getConfigValue(DEFAULT_LOOKUP_DIR_KEY);

        if (initialDirectory == null) {
            initialDirectory = System.getenv("user.home") != null ? System.getenv("user.home"): ".";
            configStore.setConfigValue(DEFAULT_LOOKUP_DIR_KEY, initialDirectory);
        }

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
    // Controller API that could be used by the FileImportDialog component or deriving implementations
    //================================================================================================================

    /**
     * Return the list property that contains the selected file name + paths as String
     *
     * @return the list property with file names
     */
    public ReadOnlyListProperty<String> getSelectedFileProperty() {
        return viewModel.selectedFilePathsProperty();
    }


    //================================================================================================================
    // internal logic to initialize and control the view
    //================================================================================================================

    /**
     * Initialize the view styles that have to be configured programmatically.
     * (default styling should be define by style sheets)
     */
    private void initViewStyles() {
        // addAndSum invalid pseudo class to text-field - to control the validation state
        txtFilePath.pseudoClassStateChanged(INVALID_PSEUDO_CLASS, false);
    }

    /**
     * Initialize the view behaviour like disable/enable behaviour,
     * bindings or validation of inputs
     */
    protected void initViewBehavior() {

        // validation of file selection
        this.viewModel.selectedFilePathsProperty().addListener((observable, oldValue, selectedFiles) ->
                viewModel.setFilePathValid(areChosenPathsValid(selectedFiles)));

        this.viewModel.filePathProperty().addListener((observable, oldValue, filePath) ->
                viewModel.setSelectedFilePaths(Collections.singletonList(filePath)));

        this.btnBrowse.focusedProperty().addListener(((observable, oldValue, newValue)
               -> txtFilePath.pseudoClassStateChanged(FOCUSED_PSEUDOCLASS, newValue || txtFilePath.isFocused())));

        this.txtFilePath.focusedProperty().addListener(((observable, oldValue, newValue)
                -> btnBrowse.pseudoClassStateChanged(FOCUSED_PSEUDOCLASS, newValue || btnBrowse.isFocused())));
    }

    /**
     * Checks if the chosen string is a valid file
     *
     * @param file filepath
     * @return true if valid else false
     */
    protected boolean isChosenFileValid(String file) {
        return isChosenPathValid(new File(file));
    }

    /**
     * Checks if the chosen file is valid
     *
     * @param file file to be checked
     * @return true if valid else false
     */
    protected boolean isChosenPathValid(File file) {
        if (file != null && file.exists() && file.canRead()) {
            txtFilePath.pseudoClassStateChanged(INVALID_PSEUDO_CLASS, false);
            btnBrowse.pseudoClassStateChanged(INVALID_PSEUDO_CLASS, false);
            btnBrowse.pseudoClassStateChanged(FOCUSED_PSEUDOCLASS, true);
            return true;
        } else {
            if (file != null && StringUtils.isBlank(file.toString())) {
                txtFilePath.pseudoClassStateChanged(INVALID_PSEUDO_CLASS, false);
                btnBrowse.pseudoClassStateChanged(INVALID_PSEUDO_CLASS, false);
                btnBrowse.pseudoClassStateChanged(FOCUSED_PSEUDOCLASS, true);
            } else {
                txtFilePath.pseudoClassStateChanged(INVALID_PSEUDO_CLASS, true);
                btnBrowse.pseudoClassStateChanged(INVALID_PSEUDO_CLASS, true);
                btnBrowse.pseudoClassStateChanged(FOCUSED_PSEUDOCLASS, false);
            }

            return false;
        }
    }

    /**
     * Bind model property to view elements.
     * See FXML annotated member of this class to get the
     * overview about supported view elements.
     */
    protected void initModelToView() {
        txtFilePath.textProperty().bindBidirectional(viewModel.filePathProperty());
        lblFileSelectorLabel.textProperty().bind(viewModel.labelTextPropertyProperty());
    }

    /**
     * This default factory method is designed to be overwritten by deriving classes.
     * It returns a LabeledFileSelectorModel implementation used hold the state of FileImportDialog views
     *
     * @return an instance of LabeledFileSelectorModel
     */
    protected LabeledFileSelectorModel createViewModel() {
        return new LabeledFileSelectorModel();
    }

    /**
     * Returns the model the view is based on. It will be a LabeledFileSelectorModel or deriving class
     * depending on the needs of the concrete importer.
     *
     * @return an instance of LabeledFileSelectorModel
     */
    protected LabeledFileSelectorModel getViewModel() {
        return this.viewModel;
    }

    /**
     * Checks if the chosen file is valid
     *
     * @param files files to be checked
     * @return true if valid else false
     */
    private boolean areChosenPathsValid(List<String> files) {
        return files.stream().anyMatch(this::isChosenFileValid);
    }

    /**
     * initialize the controller.
     */
    protected void initHandler() {
        this.btnBrowse.setOnAction(new BrowsePathActionHandler());
        this.btnBrowsePath.setOnAction(new BrowsePathActionHandler());
        this.btnBrowseFiles.setOnAction(new BrowseFilesActionHandler());
    }

    //-----------------------------------------------------------------------------------------------------------------
    //   EventHandler
    //-----------------------------------------------------------------------------------------------------------------

    private class BrowseFilesActionHandler implements EventHandler<ActionEvent> {

        @Override
        public void handle(ActionEvent actionEvent) {
            FileChooser dirChooser = new FileChooser();

            File initialDirectory = new File(LabeledFileSelectorController.this.initialDirectory);

            if (initialDirectory.isFile()) {
                initialDirectory = initialDirectory.getParentFile();
            }

            if (!initialDirectory.exists() || !initialDirectory.canRead()) {
                LabeledFileSelectorController.this.initialDirectory = ".";
                initialDirectory = new File(LabeledFileSelectorController.this.initialDirectory);
            }

            dirChooser.setInitialDirectory(initialDirectory);

            //make file chooser modal by blocking parent window
            Scene scene = btnBrowse.getScene();
            Window window = scene.getWindow();

            List<File> chosenFile = dirChooser.showOpenMultipleDialog(window);
            if (chosenFile != null) {

                // persist the selected directory for the current session and also
                // permanent be available after the next startup
                LabeledFileSelectorController.this.initialDirectory = chosenFile.get(0).getAbsolutePath();
                configStore.setConfigValue(DEFAULT_LOOKUP_DIR_KEY, LabeledFileSelectorController.this.initialDirectory);


                if (chosenFile.get(0).isFile()) {
                    LabeledFileSelectorController.this.initialDirectory = LabeledFileSelectorController.this.initialDirectory.
                            substring(0, LabeledFileSelectorController.this.initialDirectory.lastIndexOf(File.separator));
                }

                viewModel.setSelectedFilePaths(chosenFile.stream().map(File::getAbsolutePath).collect(Collectors.toList()));
            }
        }
    }

    private class BrowsePathActionHandler implements EventHandler<ActionEvent> {

        @Override
        public void handle(ActionEvent actionEvent) {
            DirectoryChooser directoryChooser = new DirectoryChooser();

            File initialDirectory = new File(LabeledFileSelectorController.this.initialDirectory);

            if (initialDirectory.isFile()) {
                initialDirectory = initialDirectory.getParentFile();
            }

            if (!initialDirectory.exists() || !initialDirectory.canRead()) {
                LabeledFileSelectorController.this.initialDirectory = ".";
                initialDirectory = new File(LabeledFileSelectorController.this.initialDirectory);
            }

            directoryChooser.setInitialDirectory(initialDirectory);

            //make file chooser modal by blocking parent window

            Scene scene = btnBrowse.getScene();
            Window window = scene.getWindow();

            File chosenFile = directoryChooser.showDialog(window);
            if (chosenFile != null) {

                // persist the selected directory for the current session and also
                // permanent be available after the next startup
                File selectedDir = new File(chosenFile.getAbsolutePath());

                if (selectedDir.isFile()) {
                    selectedDir = selectedDir.getParentFile();
                }

                LabeledFileSelectorController.this.initialDirectory = selectedDir.getAbsolutePath();
                configStore.setConfigValue(DEFAULT_LOOKUP_DIR_KEY, selectedDir.getAbsolutePath());

                viewModel.setSelectedFilePaths(Collections.singletonList(selectedDir.getAbsolutePath()));
            }
        }
    }

}

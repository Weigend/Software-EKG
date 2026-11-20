package io.softwareecg.awb.sdk.importer.ui.controls.fileselector;

import javafx.beans.property.*;
import javafx.collections.FXCollections;

import java.util.Collections;
import java.util.List;

/**
 * Default view model of all FileImportDialog implementations that can
 * derived if necessary (if specific dialog have more config values).
 *
 * @since 03.07.2018
 */
public class LabeledFileSelectorModel {

    /**
     * the path as string typed by the user or overwritten via FileChooser dialog
     * To avoid cycle event update chains only the textfield is allowed to bindProjectName to this property
     */
    private final SimpleObjectProperty<String> filePath = new SimpleObjectProperty<>("");

    /**
     * the selected paths/path. Only this property will use for imports. The filePath property has to
     * sync to this one and vice versa.
     */
    private final SimpleListProperty<String> selectedFilePaths = new SimpleListProperty<>();

    /**
     * boolean property that indicates if the chosen/specified filepath is valid or not.
     */
    private final SimpleBooleanProperty filePathValid = new SimpleBooleanProperty(false);

    private final StringProperty labelTextProperty = new SimpleStringProperty("");

    public String getLabelTextProperty() {
        return labelTextProperty.get();
    }

    public StringProperty labelTextPropertyProperty() {
        return labelTextProperty;
    }

    public void setLabelTextProperty(String labelTextProperty) {
        this.labelTextProperty.set(labelTextProperty);
    }

    public boolean isFilePathValid() {
        return filePathValid.get();
    }

    public SimpleBooleanProperty filePathValidProperty() {
        return filePathValid;
    }

    public void setFilePathValid(boolean filePathValid) {
        this.filePathValid.set(filePathValid);
    }

    /**
     * Sets the import path property
     */
    public void setPath(String newPath) {
        setSelectedFilePaths(Collections.singletonList(newPath));
    }

    /**
     * Sets a list of files paths; filepath can be a directory or a file
     * @param selectedFilePaths a list of files paths
     */
    public void setSelectedFilePaths(List<String> selectedFilePaths) {
        this.selectedFilePaths.set(FXCollections.observableArrayList(selectedFilePaths));

        // don't update itself
        if (!selectedFilePaths.toString().equals(filePath.get())) {
            filePath.set(format(selectedFilePaths.toString()));
        }
    }

    /**
     * Returns a list of file paths
     *
     * @return a list of file paths
     */
    public List<String> getSelectedFilePaths() {
        return selectedFilePaths.get();
    }

    /**
     * Gets the import paths property
     *
     * @return import paths property
     */
    public Property<String> filePathProperty() {
        return filePath;
    }

    public ListProperty<String> selectedFilePathsProperty() {
        return selectedFilePaths;
    }

    private String format(String pathArrayStringRepresentation) {
        return pathArrayStringRepresentation.replace("[", "").replace("]", "");
    }
}

package io.softwareecg.awb.sdk.importer.ui.controls.fileselector;

import io.softwareecg.awb.sdk.core.resourceloader.CdiFxmlLoader;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ReadOnlyListProperty;
import javafx.scene.layout.GridPane;


/**
 * todo enter comment here
 *
 * @since 26.07.2018
 */
public class LabeledFileSelector extends GridPane {

    private final LabeledFileSelectorController controller;

    public LabeledFileSelector() {
        controller = CdiFxmlLoader.loadView("LabeledFileSelectorView.fxml", this).getController();
    }

    public final void setLabelText(String value) {
        controller.getViewModel().setLabelTextProperty(value);
    }

    public final String getLabelText() {
        return controller.getViewModel().getLabelTextProperty();
    }

    /**
     * Return the list property that contains the selected file name + paths as String
     *
     * @return the list property with file names
     */
    public ReadOnlyListProperty<String> getSelectedFileProperty() {
        return controller.getSelectedFileProperty();
    }

    public BooleanProperty isValidFileSelectedProperty() {
        return controller.getViewModel().filePathValidProperty();
    }
}

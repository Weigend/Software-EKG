package io.softwareecg.awb.importer.ui.csv;

import io.softwareecg.awb.importer.adapter.csv.Frequency;
import io.softwareecg.awb.importer.adapter.csv.Tolerance;
import io.softwareecg.awb.runtime.resources.CdiFxmlLoader;
import javafx.beans.binding.Bindings;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.ComboBox;
import javafx.scene.layout.GridPane;

import java.net.URL;
import java.util.Arrays;
import java.util.ResourceBundle;

/**
 * Detector configuration that will be part of the collector log import dialog
 *
 * @since 28.07.2018
 */
public class DetectorConfig extends GridPane implements Initializable {

    /**
     * The tolerance combo box
     */
    @FXML
    private ComboBox<Tolerance> cbxEqualityDetection;

    /**
     * The tolerance combo box
     */
    @FXML
    private ComboBox<Frequency> cbxFrequency;

    /**
     * The view model of the detector configuration
     */
    private final DetectorConfigModel viewModel = new DetectorConfigModel();

    /**
     * Constructs a new instance of DetectorConfig and load the according (FXML) view
     */
    public DetectorConfig() {
        CdiFxmlLoader.loadView("/" + DetectorConfig.class.getPackageName().replace('.', '/')
                + "/DetectorConfigView.fxml", this, this);
    }


    //================================================================================================================
    //  Initializable interface implementation
    //================================================================================================================

    /* (non-Javadoc)
     * @see javafx.fxml.Initializable#initialize(java.net.URL, java.util.ResourceBundle)
     */
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        initExtModelToView();
    }


    //================================================================================================================
    //  controller logic
    //================================================================================================================


    /**
     * Initializes all fields on the panel.
     */
    protected void initExtModelToView() {

        this.cbxEqualityDetection.setItems((FXCollections.observableArrayList(Arrays.asList(Tolerance.values()))));
        this.cbxEqualityDetection.valueProperty().bindBidirectional(viewModel.currentToleranceProperty());

        this.cbxFrequency.setItems(FXCollections.observableList(Arrays.asList(Frequency.values())));
        this.cbxFrequency.valueProperty().bindBidirectional(viewModel.currentFrequency());

        cbxFrequency.disableProperty().bind(
                Bindings.or(
                        cbxEqualityDetection.valueProperty().isEqualTo(Tolerance.NONE),
                        cbxEqualityDetection.valueProperty().isNull()));
    }



    //================================================================================================================
    //  inner class simple model for the detector
    //================================================================================================================


    private static class DetectorConfigModel {

        private final SimpleObjectProperty<Tolerance> currentTolerance = new SimpleObjectProperty<>(Tolerance.NONE);

        private final SimpleObjectProperty<Frequency> currentFrequency = new SimpleObjectProperty<>(Frequency.ALL);

        /**
         * Gets the tolerance property
         *
         * @return series property
         */
        public Property<Tolerance> currentToleranceProperty() {
            return this.currentTolerance;
        }

        /**
         * Gets the tolerance
         *
         * @return tolerance
         */
        public Tolerance getCurrentTolerance() {
            return this.currentTolerance.get();
        }

        /**
         * Gets the frequency property
         *
         * @return frequency
         */
        public Property<Frequency> currentFrequency() {
            return this.currentFrequency;
        }

        /**
         * Gets the frequency name property
         *
         * @return frequency
         */
        public Frequency getCurrentFrequency() {
            return this.currentFrequency.get();
        }
    }
}

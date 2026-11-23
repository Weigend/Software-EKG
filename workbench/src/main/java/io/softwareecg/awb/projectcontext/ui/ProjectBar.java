package io.softwareecg.awb.projectcontext.ui;

import io.softwareecg.awb.runtime.logging.EkgLogger;
import io.softwareecg.wfx.lookup.api.Lookup;
import javafx.fxml.FXMLLoader;
import javafx.scene.layout.GridPane;
import org.slf4j.Logger;

import java.io.IOException;

/**
 * This ProjectBar component displays a thin status bar
 * that will display some meta data of the project and
 * provides controls to edit project and switch the
 * view state of it.
 *
 * @since 21.06.2018
 */
public class ProjectBar extends GridPane {

    private static final Logger LOGGER = EkgLogger.get();

    /**
     * Constructs a new instance of ProjectBar by loading the FXML
     * templates and instantiate and bind the controller to it.
     */
    public ProjectBar() {
         // attach FXML to this control instance
        try {
            // load the fxml template and return 'this' RatingFilterBar
            FXMLLoader loader = Lookup.lookup(FXMLLoader.class);
            loader.setRoot(this);
            loader.load(getClass().getResourceAsStream("ProjectBarView.fxml"));
            ((ProjectBarController)loader.getController()).setGpProjectBar(this);
        } catch (IOException exception) {
            LOGGER.error("Unable to load the underlying .fxml file.", exception);
            throw new RuntimeException(exception);
        }
    }
}
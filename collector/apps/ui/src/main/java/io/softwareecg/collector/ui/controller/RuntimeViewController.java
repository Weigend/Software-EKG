//______________________________________________________________________________
//
//          Project:    Software EKG
//______________________________________________________________________________
//
//       created by:    Weigend AM
//       creation date: 2009 - 2013
//       changed by:    $Author$
//       change date:   $Date$
//       revision:      $Revision$
//       description:   Collector UI
//______________________________________________________________________________
//
//        Copyright:    Weigend AM
//______________________________________________________________________________
package io.softwareecg.collector.ui.controller;

import io.softwareecg.collector.ui.util.AnimationsHelper;
import io.softwareecg.collector.ui.util.DialogueSwitcher;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;

import java.net.InetAddress;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * FXML Controller class
 *
 */
public class RuntimeViewController implements Initializable {

    /**
     * Our logger.
     */
    private static final Logger LOG = Logger.getLogger(RuntimeViewController.class.getName());

    @FXML
    private Button stopButton;

    @FXML
    private Parent parent;

    @FXML
    private Button datenansichtButton;

    @FXML
    private GridPane gridPane;

    @FXML
    private Label ipLabel;

    @FXML
    private StartViewController startViewController;

    /**
     * Initializes the controller class. IP will be setted and Animation gets
     * startet
     *
     * @param url
     * @param rb
     */
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        try {
            ipLabel.setText(InetAddress.getLocalHost().getHostAddress());
        } catch (UnknownHostException ex) {
            LOG.log(Level.SEVERE, "Unknown host", ex);
        }
        AnimationsHelper.animation(gridPane);
    }

    /**
     * Stops EKG and displays the Startscreen with animation
     */
    @FXML
    public void stopEKG() {

        AnimationsHelper.animation(gridPane, new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                LOG.log(Level.INFO, "Stop Button pressed");
                StartViewController.getCollectorTask().cancel(true);
                Parent startScreen = DialogueSwitcher.getParent(this.getClass(), DialogueSwitcher.FXML_LOCATION + "Startscreen.fxml");
                stopButton.getScene().setRoot(startScreen);
            }
        });

    }

    /**
     * Displays ChartView.fxml
     */
    @FXML
    public void gotoChartView() {
        Logger.getLogger(StartViewController.class.getName()).log(Level.INFO, "Show chart.");
        Parent chartViewfenster = DialogueSwitcher.getParent(this.getClass(), DialogueSwitcher.FXML_LOCATION + "ChartView.fxml");
        datenansichtButton.getScene().setRoot(chartViewfenster);
    }
}

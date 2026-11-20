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
//       description:   Start Screen Controller
//______________________________________________________________________________
//
//        Copyright:    Weigend AM
//______________________________________________________________________________
package io.softwareecg.collector.ui.controller;

import io.softwareecg.collector.control.CollectorCore;
import io.softwareecg.collector.ui.util.AnimationsHelper;
import io.softwareecg.collector.ui.util.DialogueSwitcher;
import javafx.concurrent.Task;
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
public class StartViewController implements Initializable {

    /**
     * Our logger.
     */
    private static final Logger LOG = Logger.getLogger(StartViewController.class.getName());

    @FXML
    private Button btStart;

    @FXML
    private Button btConfiguration;

    @FXML
    private Parent parent;

    @FXML
    private GridPane gridPane;

    @FXML
    private Label lbIpAddress;

    /**
     * The collector task.
     */
    private static Task collectorTask;

    /**
     * Initializes the controller class.
     *
     * @param url the url.
     * @param rb  the bundle.
     */
    @Override
    public void initialize(URL url, ResourceBundle rb) {

        try {
            lbIpAddress.setText(InetAddress.getLocalHost().getHostAddress());
        } catch (UnknownHostException ex) {
            LOG.log(Level.SEVERE, "Unknown host", ex);
        }
        AnimationsHelper.animation(gridPane);
    }

    /**
     * Starts the EKG and switches to the runtime dialog.
     */
    @FXML
    public void startEKG() {

        if (collectorTask != null && collectorTask.isRunning()) {
            LOG.warning("Cant start collector twice.");
            return;
        }

        LOG.log(Level.INFO, "Collector will be started...");

        AnimationsHelper.animation(gridPane, new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                Parent laufzeitFenster = DialogueSwitcher.getParent(this.getClass(), DialogueSwitcher.FXML_LOCATION + "LaufzeitDialog.fxml");
                btStart.getScene().setRoot(laufzeitFenster);

                collectorTask = new Task() {
                    @Override
                    protected Object call() throws Exception {
                        // EKG Collector assumes a EkgCollector directory in the user home.
                        String pathToHome = System.getProperty("user.home");
                        String pathToConfig = pathToHome + "/EkgCollector/config/config.yaml";
                        String pathToLogs = pathToHome + "/EkgCollector/logs";
                        CollectorCore.main(new String[]{pathToConfig, pathToLogs});
                        return null;
                    }
                };

                new Thread(collectorTask).start();

            }
        });
    }

    /**
     * Opens settings
     */
    @FXML
    public void openEinstellungen() {
        LOG.log(Level.INFO, "Open configuration...");
        Parent configurationView = DialogueSwitcher.getParent(this.getClass(), DialogueSwitcher.FXML_LOCATION + "EinstellungsdialogView.fxml");
        btStart.getScene().setRoot(configurationView);
    }

    /**
     * Opens explorer with a ExportAssistant as parameter
     */
    @FXML
    public void exportButtonPressed() {
        LOG.log(Level.INFO, "Export Button pressed");
        // todo
    }

    /**
     * Gets the task.
     * This can be used to stop the collector from another controller.
     *
     * @return a task.
     */
    public static Task getCollectorTask() {
        return collectorTask;
    }

}

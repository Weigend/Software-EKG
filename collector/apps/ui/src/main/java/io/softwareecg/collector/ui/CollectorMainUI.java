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
package io.softwareecg.collector.ui;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Main starter.
 *
 */
public class CollectorMainUI extends Application {

    /**
     * Our logger.
     */
    private static final Logger LOG = Logger.getLogger(CollectorMainUI.class.getName());

    /**
     * The OS name.
     */
    private static final String OS_NAME = System.getProperty("os.name");

    /**
     * The start method.
     *
     * @param stage the stage.
     * @throws Exception an exception.
     */
    @Override
    public void start(Stage stage) throws Exception {

        LOG.log(Level.INFO, "Running on {0}", OS_NAME);
        stage.setTitle("EKG-Collector UI");

        Parent root = FXMLLoader.load(getClass().getResource("res/fxml/Startscreen.fxml"));
        Scene scene = new Scene(root);

        // Make sure everything is aligned
        stage.setX(0);
        stage.setY(0);
        stage.setScene(scene);
        stage.setFullScreenExitHint("");
        stage.show();

    }

    public static void main(String[] args) {
        launch(args);
    }

}

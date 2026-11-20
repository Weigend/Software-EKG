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
package io.softwareecg.collector.ui.util;

import javafx.animation.Timeline;
import javafx.scene.Node;

/**
 */
public class ScreenSaver {

    private static final String lastWindow = "EinstellungendialogView.fxml";
    private static Timeline timeline;

    public static String getLastWindow() {
        return lastWindow;
    }

    public static void resetScreenSaver(Node but, String myName) {
//        try {
//            timeline.stop();
//        } catch (Exception e) {
//            //War wohl der Start
//        }
//        timeline = null;
//        timeline = new Timeline(new KeyFrame(
//                Duration.millis(60000), new EventHandler<ActionEvent>() {
//
//                    @Override
//                    public void handle(ActionEvent ae) {
//                        Logger.getLogger(StartscreenController.class.getName()).log(Level.INFO, "Timeout");
//                        lastWindow = myName;
//                        Parent startFenster = DialogueSwitcher.getParent(this.getClass(), DialogueSwitcher.FXML_LOCATION + "LoginView.fxml");
//                        but.getScene().setRoot(startFenster);
//                    }
//                }));
//        timeline.play();
    }
}

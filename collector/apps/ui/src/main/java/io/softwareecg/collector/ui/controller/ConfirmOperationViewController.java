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

import io.softwareecg.collector.ui.util.*;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.AnchorPane;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * FXML Controller class
 *
 */
public class ConfirmOperationViewController implements Initializable {

    /**
     * Initializes the controller class.
     */
    @FXML
    private AnchorPane parent;

    @FXML
    private Button noButton;

    @FXML
    private Button confirmButton;

    @FXML
    private Label infoLabel;

    @FXML
    private Parent zweiButtons;
    private ResourceBundle currentResources;
    private FileSystemAssistant assistant;

    /**
     * @param url
     * @param rb  Should be of type HashResourceBundle. Must include all in
     *            HasResourceBundle specified keys.
     */
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // TODO
        ScreenSaver.resetScreenSaver(infoLabel, "EinstellungsdialogView.fxml");
        StyleSwitcher.setCssFile(parent);
        AnimationsHelper.animation(zweiButtons);
        currentResources = rb;
        assistant = (FileSystemAssistant) rb.getObject(HashResourceBundle.ASSISTANT_KEY);
        infoLabel.setText("Choose this configuration?");
    }

    @FXML
    public void backButtonPressed() {
        Logger.getLogger(StartViewController.class.getName()).log(Level.INFO, "Zurueck Button pressed");
        ((FileSystemAssistant) currentResources.getObject(HashResourceBundle.ASSISTANT_KEY)).goUp();
        Parent startFenster = DialogueSwitcher.getParent(this.getClass(),
                DialogueSwitcher.FXML_LOCATION + currentResources.getObject(HashResourceBundle.SOURCE_KEY),
                (ResourceBundle) currentResources.getObject(HashResourceBundle.SOURCE_RESOURCE_BUNDLE));
        infoLabel.getScene().setRoot(startFenster);
    }

    @FXML
    public void confirm() {
        Logger.getLogger(StartViewController.class.getName()).log(Level.INFO, "Confirm button pressed");
        try {
            assistant.confirmFileOperation();
            Parent startFenster = DialogueSwitcher.getParent(this.getClass(),
                    DialogueSwitcher.FXML_LOCATION + assistant.getTarget());
            infoLabel.getScene().setRoot(startFenster);
        } catch (IOException ex) {
            Logger.getLogger(ConfirmOperationViewController.class.getName()).log(Level.SEVERE, null, ex);
            infoLabel.setText("Copying not successfull");
            noButton.disableProperty();
            confirmButton.disableProperty();
            noButton.setVisible(false);
            confirmButton.setVisible(false);
        }

    }
}

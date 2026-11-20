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
import javafx.scene.layout.GridPane;

import java.net.URL;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * FXML Controller class
 *
 */
public class EinstellungsdialogViewController implements Initializable {

    @FXML
    private Button backButton;

    @FXML
    private Parent parent;

    @FXML
    private Button showSpaceButton;

    @FXML
    private Button importConfigButton;

    @FXML
    private Button changePasswordButton;

    /**
     * For the animation
     */
    @FXML
    private GridPane vierButtonsinderMitte;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        ScreenSaver.resetScreenSaver(backButton, "EinstellungsdialogView.fxml");
        StyleSwitcher.setCssFile(parent);
        showSpaceButton.setText("Speicher\nanzeigen");
        importConfigButton.setText("Konfiguration\n importieren");
        changePasswordButton.setText("Passwort\n \u00E4ndern");
        AnimationsHelper.animation(vierButtonsinderMitte);
    }

    @FXML
    public void zurueckButtonPressed() {
        Logger.getLogger(StartViewController.class.getName()).log(Level.INFO, "Zurueck Button pressed");
        Parent startFenster = DialogueSwitcher.getParent(this.getClass(), DialogueSwitcher.FXML_LOCATION + "Startscreen.fxml"); //schauen ob Collector laeuft
        backButton.getScene().setRoot(startFenster);
    }

    @FXML
    public void passwortButtonPressed() {
        Logger.getLogger(StartViewController.class.getName()).log(Level.INFO, "passwort andern Button pressed");
        Parent sicherheitFenster = DialogueSwitcher.getParent(this.getClass(), DialogueSwitcher.FXML_LOCATION + "SicherheitView.fxml"); //schauen ob Collector laeuft
        backButton.getScene().setRoot(sicherheitFenster);
    }

    public void showSpaceButtonPressed() {
        Logger.getLogger(StartViewController.class.getName()).log(Level.INFO, "Space Button pressed");
        Parent sicherheitFenster = DialogueSwitcher.getParent(this.getClass(), DialogueSwitcher.FXML_LOCATION + "SpeicherView.fxml");
        backButton.getScene().setRoot(sicherheitFenster);
    }

    public void konfigurationImportButtonPressed() {
        Logger.getLogger(StartViewController.class.getName()).log(Level.INFO, "Konfiguration import Button pressed");
        HashResourceBundle f = new HashResourceBundle();
        f.putValue(HashResourceBundle.ASSISTANT_KEY, new ImportAssistant());
        f.putValue(HashResourceBundle.SOURCE_KEY, "EinstellungsdialogView.fxml");
        Parent konfigAendernFenster = DialogueSwitcher.getParent(this.getClass(), DialogueSwitcher.FXML_LOCATION + "ExplorerView.fxml", f);
        backButton.getScene().setRoot(konfigAendernFenster);
    }

    public void styleaendernButtonPressed() {
        Logger.getLogger(StartViewController.class.getName()).log(Level.INFO, "Sytle aendern Button pressed");
        Parent sicherheitFenster = DialogueSwitcher.getParent(this.getClass(), DialogueSwitcher.FXML_LOCATION + "StyleChangerView.fxml");
        backButton.getScene().setRoot(sicherheitFenster);
    }
}

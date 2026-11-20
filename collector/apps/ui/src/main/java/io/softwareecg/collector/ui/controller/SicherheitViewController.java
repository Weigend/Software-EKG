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

import io.softwareecg.collector.ui.util.DialogueSwitcher;
import io.softwareecg.collector.ui.util.PasswortVerwaltung;
import io.softwareecg.collector.ui.util.ScreenSaver;
import io.softwareecg.collector.ui.util.StyleSwitcher;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;

import java.net.URL;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * FXML Controller class
 *
 */
public class SicherheitViewController implements Initializable {

    /**
     * String containing the current password
     */
    private String oldPassword;

    /**
     * String containing the new password
     */
    private String newPassword;

    @FXML
    private Parent parent;

    /**
     * enum of states the window is able to have
     */
    private enum Fortschritt {

        EINLOGGEN, ERNEUTEINLOGGEN, NEUESPW, PWBESTAETIGEN, FERTIG

    }

    /**
     * Current state of the window
     */
    private Fortschritt fortschritt;

    /**
     * Button used to go back to the Settings-View
     */
    @FXML
    private Button backButton;

    /**
     * Label containing either what the user entered or "Password"
     */
    @FXML
    private Label passwortLabel;

    /**
     * Label used for text information
     */
    @FXML
    private Label infoLabel;

    /**
     * Button for inserting one
     */
    @FXML
    private Button einsButton;

    /**
     * Button for inserting two
     */
    @FXML
    private Button zweiButton;

    /**
     * Button for inserting three
     */
    @FXML
    private Button dreiButton;

    /**
     * Button for inserting four
     */
    @FXML
    private Button vierButton;

    /**
     * Initializes the controller class.
     *
     * @param url
     * @param rb
     */
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        StyleSwitcher.setCssFile(parent);
        ScreenSaver.resetScreenSaver(backButton, "EinstellungsdialogView.fxml");
        newPassword = "";
        oldPassword = "";
        fortschritt = Fortschritt.EINLOGGEN;
        setFensterzustand();
    }

    /**
     * Goes back to the einstellungenDialog
     */
    public void zurueckButtonPressed() {
        Logger.getLogger(StartViewController.class.getName()).log(Level.INFO, "Zurueck button Pressed");
        Parent StartFenster = DialogueSwitcher.getParent(this.getClass(), DialogueSwitcher.FXML_LOCATION + "EinstellungsdialogView.fxml"); //schauen ob Collector laeuft
        backButton.getScene().setRoot(StartFenster);
    }

    /**
     * Method is used to determine which button was pressed.
     *
     * @param ev Contains the source of method call. If no source is
     *           included/source is invalid(not a numbered Button from SicherheitView), a
     *           RuntimeException is thrown.
     */
    public void numberPressed(ActionEvent ev) {
        Logger.getLogger(StartViewController.class.getName()).log(Level.INFO, ev.getSource().toString());
        ScreenSaver.resetScreenSaver(backButton, "EinstellungsdialogView.fxml");
        if (ev.getSource().equals(einsButton)) {
            passwortAendern("1");
        } else if (ev.getSource().equals(zweiButton)) {
            passwortAendern("2");
        } else if (ev.getSource().equals(dreiButton)) {
            passwortAendern("3");
        } else if (ev.getSource().equals(vierButton)) {
            passwortAendern("4");
        } else {
            throw new RuntimeException("numberPressed was called without a valid action event."
                    + "ActionEvent ev must contain one of the numberButtons as source");
        }
    }

    /**
     * Cleans the passwordLabel
     */
    public void loeschenButtonPressed() {
        Logger.getLogger(StartViewController.class.getName()).log(Level.INFO, "loschen button Pressed");
        passwortLabel.setText("");
    }

    /**
     * This is the process of changing passwords
     *
     * @param neueNummer the nummer which is now added to the password
     */
    private void passwortAendern(String neueNummer) {
        String labelText = passwortLabel.getText() + neueNummer;
        passwortLabel.setText(labelText);
        if (labelText.length() == 5) {
            passwortLabel.setText(neueNummer);
        } else if (labelText.length() == 4) {
            if (fortschritt.equals(Fortschritt.EINLOGGEN) || fortschritt.equals(Fortschritt.ERNEUTEINLOGGEN)) {
                if (PasswortVerwaltung.checkLogin(labelText)) {
                    fortschritt = Fortschritt.NEUESPW;
                    oldPassword = labelText;
                } else {
                    fortschritt = Fortschritt.ERNEUTEINLOGGEN;
                }
            } else if (fortschritt.equals(Fortschritt.NEUESPW)) {
                newPassword = labelText;
                fortschritt = Fortschritt.PWBESTAETIGEN;
            } else if (fortschritt.equals(Fortschritt.PWBESTAETIGEN)) {
                if (newPassword.equals(labelText) && PasswortVerwaltung.setPasswort(oldPassword, newPassword)) {
                    fortschritt = Fortschritt.FERTIG;
                } else {
                    fortschritt = Fortschritt.EINLOGGEN;
                }
            }
            setFensterzustand();
        }
    }

    /**
     * This Method writes the right Text in the info label
     */
    private void setFensterzustand() {
        //passwortLabel.setText("");
        if (fortschritt.equals(Fortschritt.EINLOGGEN)) {
            infoLabel.setText("Altes Passwort eingeben");
        } else if (fortschritt.equals(Fortschritt.ERNEUTEINLOGGEN)) {
            infoLabel.setText("Falsches Passwort");
        } else if (fortschritt.equals(Fortschritt.NEUESPW)) {
            infoLabel.setText("Neues Passwort eingeben");
        } else if (fortschritt.equals(Fortschritt.PWBESTAETIGEN)) {
            infoLabel.setText("Neues Passwort bestätigen");
        } else if (fortschritt.equals(Fortschritt.FERTIG)) {
            zurueckButtonPressed();
        }
    }

    /*
     private void checkPassword(String input) {
     if (passwortLabel.getText().length() > 3) {
     passwortLabel.setText(input);
     } else {
     if (passwortLabel.getText().length() == 3) { //es gibt 4 Ziffern...
     if (PasswortVerwaltung.checkLogin(passwortLabel.getText() + input)) { //es stimmt mit dem aktuellen uberein
     Logger.getLogger(StartscreenController.class.getName()).log(Level.INFO, "richtig");
     altesPasswort = passwortLabel.getText() + input;  //es wird eine stufe weitergegangen
     loggedIn = true;
     infoLabel.setText("Bitte neues Passwort eingeben");
     passwortLabel.setText("");
     } else {//es stimmt nicht mit dem aktuellen uberein
     if (loggedIn) { //passt aber aufs alte
     PasswortVerwaltung.setPasswort(altesPasswort, passwortLabel.getText() + input);//es wird geandert
     Logger.getLogger(StartscreenController.class.getName()).log(Level.INFO, "changed ");
     Parent StartFenster = DialogueSwitcher.getParent(this.getClass(), DialogueSwitcher.FXML_LOCATION + "EinstellungsdialogView.fxml"); //schauen ob Collector laeuft
     backButton.getScene().setRoot(StartFenster);
     } else { //is trozdem falsch
     Logger.getLogger(StartscreenController.class.getName()).log(Level.INFO, "falsch");
     infoLabel.setText("Falsch");
     passwortLabel.setText("");
     }
     Logger.getLogger(StartscreenController.class.getName()).log(Level.INFO, "falsch");
     infoLabel.setText("Falsch");
     passwortLabel.setText("");
     }
     } else {
     passwortLabel.setText(passwortLabel.getText() + input);
     }
     }
     }*/
}

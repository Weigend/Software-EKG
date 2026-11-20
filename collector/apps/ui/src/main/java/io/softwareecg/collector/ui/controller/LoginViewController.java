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
public class LoginViewController implements Initializable {

    private String lastWindow;

    @FXML
    private Parent parent;
    /**
     * Button used to go back to the Settings-View
     */
    @FXML
    private Button zurueckButton;

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
        lastWindow = ScreenSaver.getLastWindow();
    }

    /**
     * Goes back to the einstellungenDialog
     */
    /**
     * Method is used to determine which button was pressed.
     *
     * @param ev Contains the source of method call. If no source is
     *           included/source is invalid(not a numbered Button from SicherheitView), a
     *           RuntimeException is thrown.
     */
    public void numberPressed(ActionEvent ev) {
        Logger.getLogger(StartViewController.class.getName()).log(Level.INFO, ev.getSource().toString());
        if (ev.getSource().equals(einsButton)) {
            checkPassword("1");
        } else if (ev.getSource().equals(zweiButton)) {
            checkPassword("2");
        } else if (ev.getSource().equals(dreiButton)) {
            checkPassword("3");
        } else if (ev.getSource().equals(vierButton)) {
            checkPassword("4");
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

    private void checkPassword(String input) {
        if (passwortLabel.getText().length() > 3) {
            passwortLabel.setText(input);
        } else {
            if (passwortLabel.getText().length() == 3) {
                if (PasswortVerwaltung.checkLogin(passwortLabel.getText() + input)) {
                    Logger.getLogger(StartViewController.class.getName()).log(Level.INFO, "richtig");

                    Parent StartFenster = DialogueSwitcher.getParent(this.getClass(), DialogueSwitcher.FXML_LOCATION + lastWindow); //schauen ob Collector laeuft
                    einsButton.getScene().setRoot(StartFenster);
                } else {
                    Logger.getLogger(StartViewController.class.getName()).log(Level.INFO, "falsch");
                    passwortLabel.setText("Falsch");
                }
            } else {
                passwortLabel.setText(passwortLabel.getText() + input);
            }
        }

    }
}

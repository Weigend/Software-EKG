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
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;

import java.net.URL;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * FXML Controller class
 *
 */
public class ExplorerViewController implements Initializable {

    @FXML
    private Button backButton;

    @FXML
    private Parent parent;

    @FXML
    private ListView listView;

    private FileSystemAssistant assistant;
    /**
     * ResourceBundle for Inforamtions like destination
     */
    private ResourceBundle currentResources;

    /**
     * Used for log-output
     */
    private int i = 0;

    /**
     * Target of backButton
     */
    private String backButtonTargetView = "EinstellungsdialogView.fxml";

    private HBox lastSelection;

    /**
     * Initializes the controller class.
     *
     * @param url
     * @param rb  Resource bundle containing the source and destination of the
     *            explorer
     */
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        StyleSwitcher.setCssFile(parent);
        ScreenSaver.resetScreenSaver(backButton, "EinstellungsdialogView.fxml");
        currentResources = rb;
        if (currentResources == null) {
            throw new RuntimeException("ResourceBundle rb must not be null");
        }

        backButtonTargetView = (String) currentResources.getObject(HashResourceBundle.SOURCE_KEY);
        assistant = (FileSystemAssistant) currentResources.getObject(HashResourceBundle.ASSISTANT_KEY);
        listView.setItems(assistant.getFiles());
        listView.setOnMouseClicked(getListViewListener());
    }

    /**
     * Loads settings main screen
     */
    @FXML
    public void zurueckButtonPressed() {
        Logger.getLogger(StartViewController.class.getName()).log(Level.INFO, "Zurueck Button pressed");

        Parent backFenster;

        //make sure that the sources's ResourceBundle will be returned to it
        if (currentResources.containsKey(HashResourceBundle.SOURCE_RESOURCE_BUNDLE)) {
            backFenster = DialogueSwitcher.getParent(this.getClass(), DialogueSwitcher.FXML_LOCATION + backButtonTargetView,
                    (ResourceBundle) currentResources.getObject(HashResourceBundle.SOURCE_RESOURCE_BUNDLE));
        } else {
            backFenster = DialogueSwitcher.getParent(this.getClass(), DialogueSwitcher.FXML_LOCATION + backButtonTargetView);
        }
        backButton.getScene().setRoot(backFenster);

    }

    /**
     * Resets the sreensaver at any mouse movement.
     */
    public void listViewMouseMoved() {
        Logger.getLogger(StartViewController.class.getName()).log(Level.FINE, "explorer: {0}", i++);
        ScreenSaver.resetScreenSaver(backButton, "EinstellungsdialogView.fxml");
    }

    /**
     * Updates the list
     *
     * @param box Clicked HBox
     */
    private void updateList(HBox box) {
        String selection = ((Label) box.getChildren().get(1)).getText();
        Logger.getLogger(StartViewController.class.getName()).log(Level.INFO, "clicked on {0}", selection);
        assistant.setPath(selection);
        if (assistant.isFileSystemDestinationReached()) {
            HashResourceBundle res = new HashResourceBundle();
            res.putValue(HashResourceBundle.SOURCE_KEY, "ExplorerView.fxml");
            res.putValue(HashResourceBundle.ASSISTANT_KEY, assistant);
            res.putValue(HashResourceBundle.SOURCE_RESOURCE_BUNDLE, currentResources);
            Parent startFenster = DialogueSwitcher.getParent(this.getClass(), DialogueSwitcher.FXML_LOCATION + assistant.getTarget(), res);
            backButton.getScene().setRoot(startFenster);
        } else {
            listView.setItems(assistant.getFiles());
        }
    }

    /**
     * Creates the eventhandler for the listview
     *
     * @return a new eventHandler
     */
    private EventHandler<? super MouseEvent> getListViewListener() {
        return (MouseEvent event) -> {
            if (listView.getSelectionModel().getSelectedItem() == null) {
                Logger.getLogger(StartViewController.class.getName()).log(Level.INFO, "No item selected");
            } else if (lastSelection == null) {
                lastSelection = (HBox) listView.getSelectionModel().getSelectedItem();
            } else if (lastSelection.equals(listView.getSelectionModel().getSelectedItem())) {
                Logger.getLogger(StartViewController.class.getName()).log(Level.INFO, "'Double-Click' occured");
                lastSelection = null;
                HBox selectedBox = (HBox) listView.getSelectionModel().getSelectedItem();
                listView.getSelectionModel().clearSelection();
                updateList(selectedBox);
            } else {
                lastSelection = (HBox) listView.getSelectionModel().getSelectedItem();
            }
        };
    }
}

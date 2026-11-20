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

import io.softwareecg.collector.ui.controls.ListItemFactory;
import io.softwareecg.collector.ui.util.DialogueSwitcher;
import io.softwareecg.collector.ui.util.ScreenSaver;
import io.softwareecg.collector.ui.util.StyleSwitcher;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.input.MouseEvent;

import java.net.URL;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * FXML Controller class
 *
 */
public class StyleChangerViewController implements Initializable {

    @FXML
    private Button backButton;
    @FXML
    private ListView stylesListView;
    @FXML
    private Parent parent;

    private Object lastSelection;

    /**
     * Initializes the controller class.
     */
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // TODO
        StyleSwitcher.setCssFile(parent);
        ScreenSaver.resetScreenSaver(backButton, "StyleChangerView.fxml");
//add all styles to list
        ObservableList<Label> styles = FXCollections.observableArrayList();

        for (StyleSwitcher.Style p : StyleSwitcher.Style.values()) {
            //set the current Style Label bold
            if (StyleSwitcher.getcurrentStyle().equals(p)) {
                Label labelelement = ListItemFactory.getLabelItem(p.name());
                labelelement.getStyleClass().add("listViewActive");
                styles.add(labelelement);
            } else {
                styles.add(ListItemFactory.getLabelItem(p.name()));
            }

        }
        stylesListView.setItems(styles);

// Doppelklick auf ein Element
        stylesListView.setOnMouseClicked((MouseEvent event) -> {
            if (event.getClickCount() == 2) {
                Label label = (Label) stylesListView.getSelectionModel().getSelectedItem();
                activeStylesLoeschen();
                label.getStyleClass().add("listViewActive");
                String selecton = label.getText();
                Logger.getLogger(StartViewController.class.getName()).log(Level.INFO, "clicked on {0}", selecton);

                for (StyleSwitcher.Style p : StyleSwitcher.Style.values()) {
                    if (p.toString().equals(selecton)) {
                        StyleSwitcher.changeStyle(p);
                        StyleSwitcher.setCssFile(parent);
                        break;
                    }
                }

            }
        });
    }

    private EventHandler<? super MouseEvent> getEventHandler() {
        return new EventHandler<MouseEvent>() {

            @Override
            public void handle(MouseEvent event) {
                if (stylesListView.getSelectionModel().getSelectedItem().equals(lastSelection)) {
                    Label label = (Label) stylesListView.getSelectionModel().getSelectedItem();
                    activeStylesLoeschen();
                    label.getStyleClass().add("listViewActive");
                    String selecton = label.getText();
                    Logger.getLogger(StartViewController.class.getName()).log(Level.INFO, "clicked on {0}", selecton);

                    for (StyleSwitcher.Style p : StyleSwitcher.Style.values()) {
                        if (p.toString().equals(selecton)) {
                            StyleSwitcher.changeStyle(p);
                            StyleSwitcher.setCssFile(parent);
                            break;
                        }
                    }
                } else {
                    lastSelection = stylesListView.getSelectionModel().getSelectedItem();
                }
            }

        };
    }

    @FXML
    void zurueckButtonPressed(ActionEvent event) {
        Logger.getLogger(StartViewController.class.getName()).log(Level.INFO, "Zurueck Button pressed");
        Parent startFenster = DialogueSwitcher.getParent(this.getClass(), DialogueSwitcher.FXML_LOCATION + "EinstellungsdialogView.fxml");
        backButton.getScene().setRoot(startFenster);

    }

    private void activeStylesLoeschen() {
        for (int i = 0; i < stylesListView.getItems().size(); i++) {
            Label styleLabel = (Label) stylesListView.getItems().get(i);
            for (int z = 0; z < styleLabel.getStyleClass().size(); z++) {
                if (styleLabel.getStyleClass().get(z).equals("listViewActive")) {
                    styleLabel.getStyleClass().remove(z);
                }
            }

        }
    }

}

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
import io.softwareecg.collector.ui.util.FileSystemAssistant;
import io.softwareecg.collector.ui.util.ScreenSaver;
import io.softwareecg.collector.ui.util.StyleSwitcher;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.chart.PieChart;
import javafx.scene.control.Button;

import java.io.File;
import java.net.URL;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * FXML Controller class
 *
 */
public class SpeicherViewController implements Initializable {

    /**
     * Initializes the controller class.
     */
    @FXML
    private Parent parent;
    @FXML
    private Button backButton;

    @FXML
    private PieChart pieChart;

    /**
     * not finshed jet
     *
     * @param url
     * @param rb
     */
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // TODO
        ScreenSaver.resetScreenSaver(parent, "SpeicherView.fxml");
        StyleSwitcher.setCssFile(parent);
        File laufwerk;

        if (FileSystemAssistant.isUnix()) {
            laufwerk = new File("/");
        } else {
            laufwerk = new File("C:\\");
        }

        ObservableList<PieChart.Data> pieChartData;
        pieChartData = FXCollections.observableArrayList(
                new PieChart.Data("Free Space", laufwerk.getFreeSpace()),
                new PieChart.Data("Total Space", laufwerk.getTotalSpace()),
                new PieChart.Data("Usable Space", laufwerk.getUsableSpace())
        );
        pieChart.setData(pieChartData);
        pieChart.setAnimated(true);
        pieChart.setLabelsVisible(true);
        pieChart.setLegendVisible(true);


    }

    /**
     * back to einstellungsdialog
     */
    @FXML
    public void backButtonPressed() {
        Logger.getLogger(StartViewController.class.getName()).log(Level.INFO, "Zurueck Button pressed");
        Parent startFenster = DialogueSwitcher.getParent(this.getClass(), DialogueSwitcher.FXML_LOCATION + "EinstellungsdialogView.fxml"); //schauen ob Collector laeuft
        backButton.getScene().setRoot(startFenster);
    }
}

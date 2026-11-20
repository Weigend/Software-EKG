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

import io.softwareecg.collector.memory.CollectorMemory;
import io.softwareecg.collector.ui.util.DialogueSwitcher;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
import javafx.scene.chart.XYChart.Series;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Chart live view.
 *
 */
public class ChartViewController implements Initializable {

    /**
     * How many point has one chart.
     */
    private static final int POINTS_PER_CHART = 50;

    /**
     * The hostname to access the counter memory.
     */
    private static final String COUNTER_MEMORY_HOSTNAME = "127.0.0.1";

    /**
     * The polling interval how much ms the timer waits until next update.
     */
    private static final int POLLING_INTERVAL = 2000;

    /**
     * The back button.
     */
    @FXML
    private Button backButton;

    /**
     * The chart to display live data.
     */
    @FXML
    private LineChart<String, Number> lineChart;

    /**
     * The combo box.
     */
    @FXML
    private ComboBox combo;

    /**
     * Parent needed for animation.
     */
    @FXML
    private Parent parent;

    /**
     * The timer to poll data.
     */
    private Timer timer;

    /**
     * The current series which will be changed if the user selects a new in the
     * dialogue.
     */
    private final List<XYChart.Series<String, Number>> currentSeries = FXCollections.observableArrayList();

    /**
     * The current selection of the combo box.
     */
    private String currentSelection;

    /**
     * The entries in the combo box.
     */
    private final ObservableList<String> options = FXCollections.observableArrayList();

    /**
     * A accending number for the x-axis.
     */
    private int number = 0;

    /**
     * Initializes the controller class. set also the Chart
     *
     * @param url
     * @param rb
     */
    @Override
    public void initialize(URL url, ResourceBundle rb) {

        lineChart.setAnimated(true);
        lineChart.setCreateSymbols(false);

        combo.setItems(options);
        combo.setOnAction(event -> {
            // clear series.
            currentSeries.clear();

            // change selection
            currentSelection = combo.getSelectionModel().getSelectedItem().toString();
            // change series
            ObservableList<Series<String, Number>> series = createSeries(currentSelection);

            lineChart.setData(series);
            // reset number
            number = 0;
        });

        // czclic update
        timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                updateChart();
            }
        }, 0, POLLING_INTERVAL);
    }

    /**
     * Helper to create list of series containing the metric and min, max, avg.
     *
     * @return a series list.
     */
    private ObservableList<Series<String, Number>> createSeries(String currentSelection) {

        ObservableList<XYChart.Data<String, Number>> emptyData = FXCollections.observableArrayList();
        currentSeries.add(new XYChart.Series<>("Maximum (" + currentSelection + ")", emptyData));

        emptyData = FXCollections.observableArrayList();
        currentSeries.add(new XYChart.Series<>("Miniumum (" + currentSelection + ")", emptyData));

        emptyData = FXCollections.observableArrayList();
        currentSeries.add(new XYChart.Series<>(currentSelection, emptyData));

        ObservableList<Series<String, Number>> series = FXCollections.observableArrayList();
        series.addAll(currentSeries);
        return series;
    }

    /**
     * Updates cyclic the chart.
     */
    private void updateChart() {

        Platform.runLater(() -> {
            // only once
            CollectorMemory memory = CollectorMemory.getMemory(COUNTER_MEMORY_HOSTNAME);
            String[] counter = memory.getAllCounter(COUNTER_MEMORY_HOSTNAME);

            if (options.size() == 0 && counter.length > 0) {
                options.clear();
                options.addAll(counter);
                combo.getSelectionModel().select(counter[0]);
            }
            if (currentSelection != null) {
                addData(memory.value(currentSelection), 2);
                addData(memory.min(currentSelection), 1);
                addData(memory.max(currentSelection), 0);
                number++;
            }
        });
    }

    /**
     * Adds data the a series in the series list.
     *
     * @param doubleValue the value.
     * @param idx         the index of the series.
     */
    private void addData(double doubleValue, int idx) {
        currentSeries.get(idx).getData().add(new LineChart.Data(Double.toString(number), doubleValue));
        if (currentSeries.get(idx).getData().size() > POINTS_PER_CHART) {
            currentSeries.get(idx).getData().remove(0);
        }
    }

    /**
     * if the back Button is pressed then go to laufzeitdialog or stat
     */
    @FXML
    public void backButtonPressed() {
        Parent root = DialogueSwitcher.getParent(this.getClass(), DialogueSwitcher.FXML_LOCATION + "LaufzeitDialog.fxml");
        parent.getScene().setRoot(root);
        timer.cancel();
    }
}

package io.softwareecg.awb.evidence.api;

import javafx.scene.layout.Region;
import javafx.stage.Window;

import java.util.List;

/**
 * Takes a chart out of the workbench. The methods ask the user where to, and are to be called on
 * the JavaFX thread.
 */
public interface ChartExport {

    /**
     * Saves a picture of the chart.
     *
     * @param owner     the window the dialog belongs to
     * @param chart     the chart
     * @param axisWidth the width of its value axis: in a picture of a chosen size, the writing is
     *                  as large as it suits this width
     */
    void exportAsImage(Window owner, Region chart, double axisWidth);

    /**
     * Saves the values of series as an Excel sheet, one beside the other.
     *
     * @param owner  the window the dialog belongs to
     * @param series the series
     */
    void exportAsExcel(Window owner, List<ExportedSeries> series);
}

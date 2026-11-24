package io.softwareecg.awb.evidence.api;

import io.softwareecg.awb.uitoolkit.chart.ZoomableStackedChart;
import javafx.stage.Window;

/**
 * Takes a chart out of the workbench. The methods ask the user where to, and are to be called on
 * the JavaFX thread.
 */
public interface ChartExport {

    /**
     * Saves a picture of the chart.
     *
     * @param owner the window the dialog belongs to
     * @param chart the chart
     */
    void exportAsImage(Window owner, ZoomableStackedChart chart);

    /**
     * Saves the values the chart shows, of the charts in the background as well, as an Excel sheet.
     *
     * @param owner the window the dialog belongs to
     * @param chart the chart
     */
    void exportAsExcel(Window owner, ZoomableStackedChart chart);
}

package io.softwareecg.awb.evidence.api;

import javafx.scene.chart.XYChart;

import java.util.List;

/**
 * A series whose values are exported.
 *
 * @param name   its name
 * @param points its times, in milliseconds, and values
 */
public record ExportedSeries(String name, List<XYChart.Data<Long, Double>> points) {

    /**
     * @return the point at an index, {@code null} if the series has less points
     */
    public XYChart.Data<Long, Double> pointAt(int index) {
        return index < points.size() ? points.get(index) : null;
    }
}

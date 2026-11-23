package io.softwareecg.awb.evidence.export;

import javafx.scene.chart.XYChart;

import java.util.List;

/**
 * Tiny Container to storage series name and the series data points
 *
 * @since 16.09.2018
 */
public class ExcelExportSeriesData {

    private final String seriesName;

    private final List<XYChart.Data<Long, Double>> seriesData;

    public ExcelExportSeriesData(String seriesName, List<XYChart.Data<Long, Double>> seriesData) {
        this.seriesName = seriesName;
        this.seriesData = seriesData;
    }

    public String getSeriesName() {
        return seriesName;
    }

    public List<XYChart.Data<Long, Double>> getSeriesData() {
        return seriesData;
    }

    public XYChart.Data<Long, Double> getDataAt(int idx) {

        if (idx >= seriesData.size()) {
            return null;
        }

        return seriesData.get(idx);
    }
}

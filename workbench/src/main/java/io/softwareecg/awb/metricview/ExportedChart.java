package io.softwareecg.awb.metricview;

import io.softwareecg.awb.evidence.api.ExportedSeries;
import io.softwareecg.awb.uitoolkit.chart.ColoredChart;
import io.softwareecg.awb.uitoolkit.chart.ZoomableStackedChart;

import java.util.List;
import java.util.stream.Stream;

/**
 * What an export needs of a chart.
 */
final class ExportedChart {

    private ExportedChart() {
    }

    /** What the chart in front shows, then what those behind it show. */
    static List<ExportedSeries> seriesOf(ZoomableStackedChart chart) {
        return Stream.concat(Stream.of(chart.getBaseChart()), chart.getBackgroundCharts().stream())
                .map(ColoredChart::getAllSeries)
                .flatMap(List::stream)
                .map(series -> new ExportedSeries(series.getName(), series.getData()))
                .toList();
    }

    static double axisWidthOf(ZoomableStackedChart chart) {
        return chart.getBaseChart().getYAxis().castToAxis().getWidth();
    }
}

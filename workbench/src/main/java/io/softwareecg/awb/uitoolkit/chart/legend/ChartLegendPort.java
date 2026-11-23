package io.softwareecg.awb.uitoolkit.chart.legend;

import io.softwareecg.awb.uitoolkit.chart.ColoredChart;

import java.util.List;

/** Boundary between chart composition and a legend view. */
public interface ChartLegendPort {
    void setBaseChart(ColoredChart baseChart);
    void setBackgroundChart(List<ColoredChart> backgroundCharts);
    void setActionHandler(LegendActionCallbacks actions);
}

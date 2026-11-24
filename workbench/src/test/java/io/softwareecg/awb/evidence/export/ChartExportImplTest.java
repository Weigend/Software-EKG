package io.softwareecg.awb.evidence.export;

import io.softwareecg.awb.uitoolkit.chart.ColoredChart;
import io.softwareecg.awb.uitoolkit.chart.ZoomableStackedChart;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.scene.chart.XYChart;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * An Excel sheet has what the chart shows, with the charts in the background.
 */
public class ChartExportImplTest {

    @BeforeClass
    public static void startToolkit() {
        try {
            Platform.startup(() -> { });
        } catch (IllegalStateException alreadyRunning) {
            // toolkit was started by another test in this JVM
        }
        Platform.setImplicitExit(false);
    }

    @Test
    public void theChartInFrontComesFirst() {
        ColoredChart front = chart(series("cpu", 1), series("load", 2));
        ColoredChart behind = chart(series("memory", 3));
        ColoredChart farBehind = chart(series("disk", 4));
        ZoomableStackedChart chart = mock(ZoomableStackedChart.class);
        when(chart.getBaseChart()).thenReturn(front);
        when(chart.getBackgroundCharts()).thenReturn(FXCollections.observableArrayList(behind, farBehind));

        List<ExcelExportSeriesData> exported = ChartExportImpl.seriesOf(chart);

        assertEquals(List.of("cpu", "load", "memory", "disk"),
                exported.stream().map(ExcelExportSeriesData::getSeriesName).toList());
        assertEquals(List.of(1.0, 2.0, 3.0, 4.0),
                exported.stream().map(series -> series.getDataAt(0).getYValue()).toList());
    }

    @SafeVarargs
    private static ColoredChart chart(XYChart.Series<Long, Double>... series) {
        ColoredChart chart = mock(ColoredChart.class);
        when(chart.getAllSeries()).thenReturn(Arrays.asList(series));
        return chart;
    }

    private static XYChart.Series<Long, Double> series(String name, double value) {
        return new XYChart.Series<>(name, FXCollections.observableArrayList(new XYChart.Data<>(1000L, value)));
    }
}

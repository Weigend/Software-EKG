package io.softwareecg.awb.uitoolkit.chart;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.paint.Color;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import io.softwareecg.awb.uitoolkit.chart.ColoredChart;
/**
 * A chart whose data is loaded again - as it is every few seconds while the data is collected -
 * keeps what the user set for its series.
 */
public class ColoredChartReplaceSeriesTest {

    private ColoredChart chart;

    private final AtomicInteger legendRebuilds = new AtomicInteger();

    @BeforeClass
    public static void startToolkit() {
        try {
            Platform.startup(() -> { });
        } catch (IllegalStateException alreadyRunning) {
            // toolkit was started by another test in this JVM
        }
        Platform.setImplicitExit(false);
    }

    @Before
    @SuppressWarnings({"unchecked", "rawtypes"})
    public void createChart() throws Exception {
        onFx(() -> {
            chart = new ColoredChart((XYChart) new LineChart<>(new NumberAxis(), new NumberAxis()));
            chart.addSeries(series("cpu", 1, 2));
            chart.addSeries(series("memory", 10, 20));
            chart.addListener(observable -> legendRebuilds.incrementAndGet());
            return null;
        });
    }

    @Test
    public void newValuesLeaveHiddenSeriesHiddenAndColoursAsTheyAre() throws Exception {
        onFx(() -> {
            XYChart.Series<Long, Double> cpuBefore = chart.getAllSeries().get(0);
            chart.getColoredSeries("memory").get(0).setVisible(false);
            chart.getColoredSeries("cpu").get(0).setColor(Color.CRIMSON);

            chart.replaceSeries(List.of(series("cpu", 1, 2, 3), series("memory", 10, 20, 30)), true);

            assertSame("the series stays the one it was - with everything set for it", cpuBefore, chart.getAllSeries().get(0));
            assertEquals(List.of(1.0, 2.0, 3.0), values("cpu"));
            assertEquals(List.of(10.0, 20.0, 30.0), values("memory"));
            assertFalse("what the user hid stays hidden", chart.getColoredSeries("memory").get(0).isVisible());
            assertTrue(chart.getColoredSeries("cpu").get(0).isVisible());
            assertEquals(Color.CRIMSON, chart.getColoredSeries("cpu").get(0).getColor());
            assertEquals("the legend is left as the user sees it", 0, legendRebuilds.get());
            return null;
        });
    }

    @Test
    public void seriesThatComeAndGoAreAddedAndRemovedAroundTheOnesThatStay() throws Exception {
        onFx(() -> {
            chart.getColoredSeries("memory").get(0).setVisible(false);

            chart.replaceSeries(List.of(series("memory", 11), series("load", 5, 6)), false);

            assertEquals(List.of("memory", "load"), chart.getAllSeries().stream().map(XYChart.Series::getName).collect(Collectors.toList()));
            assertFalse(chart.getColoredSeries("memory").get(0).isVisible());
            assertTrue("a new series is shown", chart.getColoredSeries("load").get(0).isVisible());
            assertNull("a series that is gone has no settings left", chart.getColoredSeries("cpu"));
            assertTrue("the legend shows the changed set of series", legendRebuilds.get() > 0);
            return null;
        });
    }

    @Test
    public void seriesOfTheSameNameCannotBeToldApartAndAreLoadedFromScratch() throws Exception {
        onFx(() -> {
            chart.replaceSeries(List.of(series("cpu", 1), series("cpu", 2)), false);

            assertEquals(2, chart.getAllSeries().size());
            assertEquals(2, chart.getColoredSeries("cpu").size());
            return null;
        });
    }

    private List<Double> values(String seriesName) {
        return chart.getColoredSeries(seriesName).get(0).getFxSeries().getData().stream()
                .map(XYChart.Data::getYValue).collect(Collectors.toList());
    }

    private static XYChart.Series<Long, Double> series(String name, double... values) {
        XYChart.Series<Long, Double> series = new XYChart.Series<>(FXCollections.observableArrayList());
        series.setName(name);
        for (int i = 0; i < values.length; i++) {
            series.getData().add(new XYChart.Data<>((long) i, values[i]));
        }
        return series;
    }

    private static <T> T onFx(Callable<T> action) throws Exception {
        CompletableFuture<T> result = new CompletableFuture<>();
        Platform.runLater(() -> {
            try {
                result.complete(action.call());
            } catch (Throwable t) {
                result.completeExceptionally(t);
            }
        });
        try {
            return result.get(10, TimeUnit.SECONDS);
        } catch (java.util.concurrent.ExecutionException e) {
            if (e.getCause() instanceof AssertionError assertion) {
                throw assertion;
            }
            throw e;
        }
    }
}

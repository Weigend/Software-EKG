package io.softwareecg.awb.uitoolkit.chart.axis;

import javafx.application.Platform;
import javafx.geometry.Side;
import javafx.scene.Scene;
import javafx.scene.layout.Pane;
import javafx.scene.text.Text;
import org.junit.BeforeClass;
import org.junit.Test;

import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;

/**
 * The axis of a chart is replaced by a copy of itself when the type of the chart changes. The
 * copy shows the times as the original did: in the time zone chosen for the chart.
 */
public class DateAxisTest {

    private static final ZoneId BERLIN = ZoneId.of("Europe/Berlin");

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
    public void anAxisIsInUtcUnlessToldOtherwise() {
        assertEquals(ZoneId.of("UTC"), new DateAxis().getTimeZone());
    }

    @Test
    public void theCopyOfAnAxisKeepsItsTimeZone() {
        DateAxis axis = new DateAxis();
        axis.setTimeZone(BERLIN);

        assertEquals(BERLIN, ((DateAxis) axis.spawn()).getTimeZone());
        assertEquals(BERLIN, ((DateAxis) axis.spawnBounded()).getTimeZone());
    }

    /**
     * An axis that ends with a tick shows its label, however wide it is. At some widths it did not:
     * the position of the tick was a rounding error beyond the end.
     */
    @Test
    public void theLabelAtTheEndIsShownAtEveryWidth() throws Exception {
        double start = Instant.parse("2026-11-21T16:48:00Z").toEpochMilli();
        double end = Instant.parse("2026-11-21T16:49:15Z").toEpochMilli();
        DateAxis axis = onFx(() -> new DateAxis(start, end, 15_000));

        assertEquals("widths without the last label", List.of(), widthsWithout("16:49:15", axis));
    }

    private static List<Integer> widthsWithout(String text, DateAxis axis) throws Exception {
        return onFx(() -> {
            axis.setAutoRanging(false);
            axis.setAnimated(false);
            axis.setSide(Side.BOTTOM);
            Pane pane = new Pane(axis);
            new Scene(pane);
            pane.applyCss();
            List<Integer> widths = new ArrayList<>();
            for (int width = 600; width <= 4000; width++) {
                axis.resize(width, 50);
                axis.layout();
                boolean shown = axis.getChildrenUnmodifiable().stream()
                        .filter(Text.class::isInstance).map(Text.class::cast)
                        .anyMatch(label -> label.isVisible() && label.getText().endsWith(text));
                if (!shown) {
                    widths.add(width);
                }
            }
            return widths;
        });
    }

    private static <T> T onFx(Callable<T> action) throws Exception {
        FutureTask<T> task = new FutureTask<>(action);
        Platform.runLater(task);
        return task.get(60, TimeUnit.SECONDS);
    }
}

package io.softwareecg.awb.uitoolkit.chart.axis;

import javafx.application.Platform;
import org.junit.BeforeClass;
import org.junit.Test;

import java.time.ZoneId;

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
}

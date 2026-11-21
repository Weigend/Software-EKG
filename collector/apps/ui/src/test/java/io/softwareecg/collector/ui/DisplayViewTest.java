package io.softwareecg.collector.ui;

import io.softwareecg.collector.control.CollectorJobValues;
import io.softwareecg.collector.control.CollectorStatus;
import io.softwareecg.collector.control.jobs.CollectorJobState.CollectorJobStateEnum;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.image.PixelReader;
import javafx.scene.image.WritableImage;
import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static io.softwareecg.collector.ui.DisplayModelTest.job;
import static io.softwareecg.collector.ui.DisplayModelTest.status;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Builds the pages of the display and looks at what they say. No window is opened; the pages
 * are drawn into pictures, which are kept in target/display for a look at them.
 * <p>
 * Needs a screen to draw for, as every JavaFX test does (xvfb-run on a machine without one), and
 * is skipped where there is none.
 */
public class DisplayViewTest {

    private static final Instant T0 = Instant.parse("2026-11-21T10:00:00Z");

    @BeforeClass
    public static void startToolkit() {
        CompletableFuture<Void> started = new CompletableFuture<>();
        try {
            Platform.startup(() -> started.complete(null));
            started.get(20, TimeUnit.SECONDS);
        } catch (IllegalStateException alreadyStarted) {
            // by another test of this run
        } catch (Throwable noScreen) {
            Assume.assumeNoException("JavaFX cannot draw on this machine", noScreen);
        }
    }

    @Test
    public void thePageOfTheCollectorSaysHowItsJobsAreDoing() throws Exception {
        DisplayModel model = new DisplayModel();
        model.update(collector(60));
        List<String> texts = onFxThread(() -> {
            DisplayView view = new DisplayView(model, "Checkout incident", () -> T0.plusSeconds(62), ZoneOffset.UTC);
            Scene scene = new Scene(view.root(), 800, 480);
            draw(scene, "overview-800x480.png");
            return textsOf(view.root());
        });

        assertTrue(texts.toString(), texts.contains("Checkout incident"));
        assertTrue("the collector as a whole: " + texts, texts.contains("1 problem"));
        assertTrue(texts.toString(), texts.contains("10:01:02"));
        assertTrue("the job with the problem and what is wrong: " + texts, texts.contains("Not reachable"));
        assertTrue(texts.toString(), texts.stream().anyMatch(text -> text.contains("db-02.example.org cannot be read over SSH")));
        assertTrue(texts.toString(), texts.contains("app-01  ·  Checkout API"));
        assertTrue(texts.toString(), texts.contains("jvm (jmx)  ·  3 metrics"));
        assertTrue(texts.toString(), texts.stream().anyMatch(text -> text.startsWith("Running for 1 h 2 min")));
    }

    @Test
    public void thePageOfAJobShowsItsMetricsWithTheirValues() throws Exception {
        DisplayModel model = new DisplayModel();
        for (int second = 0; second <= 90; second++) {
            model.update(collector(second));
        }
        List<String> texts = onFxThread(() -> {
            DisplayView view = new DisplayView(model, "Checkout incident", () -> T0.plusSeconds(91), ZoneOffset.UTC);
            Scene scene = new Scene(view.root(), 800, 480);
            view.open(model.jobs().stream().filter(job -> job.title().startsWith("app-01")).findFirst().orElseThrow());
            draw(scene, "job-800x480.png");
            return textsOf(view.root());
        });

        assertTrue(texts.toString(), texts.contains("‹  Jobs"));
        assertTrue(texts.toString(), texts.contains("app-01  ·  Checkout API"));
        assertTrue(texts.toString(), texts.contains("MXBean(java.lang:type=Memory).HeapMemoryUsage.used"));
        assertTrue("the value read last, short: " + texts, texts.contains("590 M"));
        assertTrue(texts.toString(), texts.contains("OK"));
        assertFalse("the page of the collector is not shown beside it", texts.contains("Problems"));
    }

    @Test
    public void aSmallPanelAndACollectorWithoutJobs() throws Exception {
        DisplayModel model = new DisplayModel();
        for (int second = 0; second <= 30; second++) {
            model.update(collector(second));
        }
        onFxThread(() -> {
            DisplayView view = new DisplayView(model, "Checkout incident", () -> T0.plusSeconds(31), ZoneOffset.UTC);
            draw(new Scene(view.root(), 480, 320), "overview-480x320.png");
            return null;
        });

        List<String> texts = onFxThread(() -> {
            DisplayView view = new DisplayView(new DisplayModel(), "Software-EKG Collector", () -> T0, ZoneOffset.UTC);
            draw(new Scene(view.root(), 800, 480), "empty-800x480.png");
            return textsOf(view.root());
        });
        assertTrue(texts.toString(), texts.contains("The collector has no jobs."));
        assertEquals("Not started", texts.stream().filter(text -> text.startsWith("Not")).findFirst().orElse(null));
    }

    // ---------------------------------------------------------------------------------------------
    // a collector to show
    // ---------------------------------------------------------------------------------------------

    /**
     * Three application servers and two backend services, a little after their start.
     */
    private static CollectorStatus collector(int second) {
        Instant time = T0.plusSeconds(second);
        double heap = 500e6 + second * 1e6;
        CollectorJobValues jvm = new CollectorJobValues(time,
                List.of("MXBean(java.lang:type=Memory).HeapMemoryUsage.used", "MXBean(java.lang:type=Threading).ThreadCount",
                        "request time ms, slowest"),
                java.util.Arrays.asList(heap, 40.0 + (second % 7), second % 5 == 0 ? null : 120.0 + 80 * Math.sin(second / 4.0)));
        CollectorJobValues os = new CollectorJobValues(time, List.of("\\CPU\\us", "\\Load\\one", "\\Memory\\used"),
                List.of(12.0 + 8 * Math.sin(second / 6.0), 0.4 + (second % 9) / 10.0, 6.2e9));
        return new CollectorStatus(true, T0.minusSeconds(3_660), List.of(
                jobOf("app-01", "Checkout API", "jvm", "jmx", CollectorJobStateEnum.RUN_OK, 3, time, null, jvm),
                jobOf("app-02", "Checkout API", "jvm", "jmx", CollectorJobStateEnum.RUN_OK, 3, time, null, jvm),
                jobOf("db-01", "global", "os", "unix", CollectorJobStateEnum.RUN_OK, 3, time, null, os),
                jobOf("db-02", "global", "os", "unix", CollectorJobStateEnum.ENV_ERROR, 3, null,
                        "/var/log/service/service.log on db-02.example.org cannot be read over SSH: Connection refused", null),
                jobOf("mq-01", "service", "errors", "log", CollectorJobStateEnum.WAIT_FOR_DATA, 2, null, null, null)));
    }

    private static io.softwareecg.collector.control.CollectorJobStatus jobOf(
            String host, String process, String metricGroup, String source, CollectorJobStateEnum state, int metrics,
            Instant lastSample, String error, CollectorJobValues values) {
        return new io.softwareecg.collector.control.CollectorJobStatus("servers", host, host, process, metricGroup,
                source, state, metrics, lastSample, error, values == null ? CollectorJobValues.NONE : values);
    }

    // ---------------------------------------------------------------------------------------------
    // helpers
    // ---------------------------------------------------------------------------------------------

    private static <T> T onFxThread(Callable<T> task) throws Exception {
        CompletableFuture<T> result = new CompletableFuture<>();
        Platform.runLater(() -> {
            try {
                result.complete(task.call());
            } catch (Throwable e) {
                result.completeExceptionally(e);
            }
        });
        return result.get(30, TimeUnit.SECONDS);
    }

    /** Lays the scene out and keeps a picture of it. */
    private static void draw(Scene scene, String name) throws Exception {
        scene.getRoot().applyCss();
        scene.getRoot().layout();
        WritableImage image = scene.snapshot(null);
        PixelReader pixels = image.getPixelReader();
        BufferedImage picture = new BufferedImage((int) image.getWidth(), (int) image.getHeight(), BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < picture.getHeight(); y++) {
            for (int x = 0; x < picture.getWidth(); x++) {
                picture.setRGB(x, y, pixels.getArgb(x, y));
            }
        }
        File directory = new File("target/display");
        directory.mkdirs();
        ImageIO.write(picture, "png", new File(directory, name));
    }

    /** The texts that can be read on the page that is shown. */
    private static List<String> textsOf(Node node) {
        List<String> texts = new ArrayList<>();
        collect(node, texts);
        return texts;
    }

    private static void collect(Node node, List<String> texts) {
        if (!node.isVisible()) {
            return;
        }
        if (node instanceof Label label && label.getText() != null && !label.getText().isEmpty()) {
            texts.add(label.getText());
        } else if (node instanceof javafx.scene.control.Labeled labeled && labeled.getText() != null
                && !labeled.getText().isEmpty()) {
            texts.add(labeled.getText());
        }
        if (node instanceof javafx.scene.Parent parent) {
            for (Node child : parent.getChildrenUnmodifiable()) {
                collect(child, texts);
            }
        }
    }
}

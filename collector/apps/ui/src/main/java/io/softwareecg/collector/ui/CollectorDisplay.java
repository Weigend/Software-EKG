package io.softwareecg.collector.ui;

import io.softwareecg.collector.config.CollectorConfigurations;
import io.softwareecg.collector.control.CollectorEngine;
import io.softwareecg.collector.control.CollectorFrontend;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.input.KeyCombination;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.Instant;
import java.time.ZoneId;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Shows on a screen of the machine the collector runs on what the collector is doing: whether
 * its jobs deliver, what is wrong if one does not, and the values they read lately.
 * <p>
 * It is a look at the collector, nothing more. It does not start or stop anything and changes no
 * setting, so it needs no login, and closing its window leaves the collector collecting.
 * <p>
 * The collector starts it if it is told to: {@code --ui} of the start script, or
 * {@code io.softwareecg.collector.ui.enabled=true} in collector.properties.
 */
public class CollectorDisplay implements CollectorFrontend {

    private static final Logger LOGGER = LoggerFactory.getLogger(CollectorDisplay.class);

    private static final String PRODUCT = "Software-EKG Collector";

    private static final int START_TIMEOUT_SECONDS = 30;

    private volatile Stage stage;

    private volatile Timeline ticking;

    @Override
    public void start(CollectorEngine engine, File configuration) {
        DisplaySettings settings = DisplaySettings.configured();
        String title = projectOf(configuration);

        CompletableFuture<Void> shown = new CompletableFuture<>();
        Runnable show = () -> {
            try {
                show(engine, title, settings);
                shown.complete(null);
            } catch (RuntimeException | Error e) {
                shown.completeExceptionally(e);
            }
        };
        // closing the window must not end the toolkit: the display may be opened again
        Platform.setImplicitExit(false);
        try {
            Platform.startup(show);
        } catch (IllegalStateException alreadyStarted) {
            Platform.runLater(show);
        }

        try {
            shown.get(START_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (ExecutionException e) {
            throw new IllegalStateException(e.getCause().getMessage(), e.getCause());
        } catch (TimeoutException e) {
            throw new IllegalStateException("the screen did not answer within " + START_TIMEOUT_SECONDS + " seconds");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("starting was interrupted");
        }
    }

    private void show(CollectorEngine engine, String title, DisplaySettings settings) {
        DisplayModel model = new DisplayModel();
        model.update(engine.getStatus());
        DisplayView view = new DisplayView(model, title, Instant::now, ZoneId.systemDefault());

        Stage window = new Stage();
        window.setTitle(title.equals(PRODUCT) ? PRODUCT : title + " - " + PRODUCT);
        window.setScene(new Scene(view.root(), settings.width(), settings.height()));
        window.setMinWidth(480);
        window.setMinHeight(320);
        if (settings.fullScreen()) {
            // a panel has no keyboard to leave the full screen with, and needs no hint how to
            window.setFullScreenExitHint("");
            window.setFullScreenExitKeyCombination(KeyCombination.NO_MATCH);
            window.setFullScreen(true);
        }
        window.setOnCloseRequest(event ->
                LOGGER.info("The display of the collector was closed. The collector goes on collecting."));
        window.show();
        stage = window;

        Timeline timeline = new Timeline(new KeyFrame(javafx.util.Duration.seconds(1), event -> {
            model.update(engine.getStatus());
            view.refresh();
        }));
        timeline.setCycleCount(Animation.INDEFINITE);
        timeline.play();
        ticking = timeline;
    }

    @Override
    public void stop() {
        try {
            Platform.runLater(() -> {
                if (ticking != null) {
                    ticking.stop();
                }
                if (stage != null) {
                    stage.close();
                }
            });
            Platform.exit();
        } catch (IllegalStateException notStarted) {
            // there is nothing to end
        }
    }

    /**
     * @return the project the configuration collects for, as the name of what is shown; the name
     *         of the product if the configuration names none
     */
    static String projectOf(File configuration) {
        try {
            String project = CollectorConfigurations.read(
                    Files.readString(configuration.toPath(), StandardCharsets.UTF_8)).project();
            return project == null || project.isBlank() ? PRODUCT : project.strip();
        } catch (Exception e) {
            return PRODUCT;   // the collector itself has read it already; the name is not worth failing for
        }
    }
}

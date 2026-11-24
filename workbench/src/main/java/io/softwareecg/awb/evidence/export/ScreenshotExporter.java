package io.softwareecg.awb.evidence.export;

import io.softwareecg.awb.runtime.logging.EkgLogger;
import javafx.scene.layout.Region;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.SnapshotParameters;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;
import javafx.scene.transform.Transform;
import org.slf4j.Logger;

import javax.imageio.ImageIO;
import java.io.File;
import java.io.IOException;

/**
 * This exporter encapsulate the whole logic to create a screenshot
 * of a chart by taking account the
 * given settings to the dimension, background and output path.
 *
 * @since 16.11.2018
 */
public class ScreenshotExporter {

    private static final double PREFERED_AXIS_CHART_WITH_RATIO = 28.0;

    /**
     * Logger used by this instance to persist errors
     */
    private static final Logger LOGGER = EkgLogger.get();

    /**
     * The chart that
     * will exported as bitmap image.
     */
    private final Region chartComponent;

    private final double axisWidth;

    /**
     * Construct a new instance of ScreenshotExporter that can capture
     * screenshots from the given chart.
     *
     * @param chart     the chart component that acts as source for the screenshot exports
     * @param axisWidth the width of its value axis
     */
    public ScreenshotExporter(Region chart, double axisWidth) {
        this.chartComponent = chart;
        this.axisWidth = axisWidth;
    }

    /**
     * Create a new screenshot of the chart using the given settings.
     * The screenshot will written to the file system at the location the
     * exportFile parameter points to.
     *
     * @param dimension a enumeration that specifies the output size/dimension of the created image
     * @param useWhiteBg a boolean flag that controls if the image background will filled with white color or keep transparent
     * @param invertAxisColor invert the axis color to use white axis and labels instead the black ones
     * @param exportFile a File instance that points to the output location the image will written to
     */
    public void export(ScreenshotDimension dimension, boolean useWhiteBg, boolean invertAxisColor, File exportFile) {

        if (exportFile == null) {
            throw new IllegalArgumentException("The file that points to the screenshot export target is NULL.");
        }

        SnapshotParameters snapshotParameters = new SnapshotParameters();

        WritableImage image;

        if (dimension.getScaleFactor() > 1) {
            snapshotParameters.setTransform(Transform.scale(dimension.getScaleFactor(), dimension.getScaleFactor()));
        }

        if (invertAxisColor) {
            chartComponent.getStyleClass().add("whiteAxis");
        }

        if (!useWhiteBg) {
            snapshotParameters.setFill(Color.TRANSPARENT);
        }

        if (dimension.getScaleFactor() <= 0) {
            double targetWidth = dimension.getDimensionWidth();
            double targetHeight = dimension.getDimensionHeight();

            double currentWidth = chartComponent.getWidth();
            double currentHeight = chartComponent.getHeight();

            double currentMaxWidth = chartComponent.getMaxWidth();
            double currentMaxHeight = chartComponent.getMaxHeight();

            double currentMinWidth = chartComponent.getMinWidth();
            double currentMinHeight = chartComponent.getMinHeight();

            double axisChartWidthRatio = targetWidth / axisWidth;

            double scaleFactor = axisChartWidthRatio / PREFERED_AXIS_CHART_WITH_RATIO;
            scaleFactor = Math.round(scaleFactor * 100.0) / 100.0;

            targetHeight /= scaleFactor;
            targetWidth /= scaleFactor;

            chartComponent.setMaxHeight(targetHeight);
            chartComponent.setMinHeight(targetHeight);
            chartComponent.setMaxWidth(targetWidth);
            chartComponent.setMinWidth(targetWidth);

            snapshotParameters.setTransform(Transform.scale(scaleFactor, scaleFactor));

            image = chartComponent.snapshot(snapshotParameters, null);

            chartComponent.setPrefWidth(currentWidth);
            chartComponent.setPrefHeight(currentHeight);

            chartComponent.setMinWidth(currentMinWidth);
            chartComponent.setMinHeight(currentMinHeight);

            chartComponent.setMaxWidth(currentMaxWidth);
            chartComponent.setMaxHeight(currentMaxHeight);

        } else {
            image = chartComponent.snapshot(snapshotParameters, null);
        }

        if (invertAxisColor) {
            chartComponent.getStyleClass().remove("whiteAxis");
        }

        try {
            ImageIO.write(SwingFXUtils.fromFXImage(image, null), "png", exportFile);
        } catch (IOException e) {
            LOGGER.error("Error occurred during save the screenshot", e);
        }
    }
}

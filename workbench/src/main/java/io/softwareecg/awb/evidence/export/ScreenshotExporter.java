package io.softwareecg.awb.evidence.export;

import io.softwareecg.awb.runtime.logging.EkgLogger;
import javafx.scene.layout.Region;
import javafx.embed.swing.SwingFXUtils;
import javafx.geometry.Bounds;
import javafx.geometry.Rectangle2D;
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

    private static final int MAX_ATTEMPTS_TO_FIT = 5;

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

            double axisChartWidthRatio = targetWidth / axisWidth;

            double scaleFactor = axisChartWidthRatio / PREFERED_AXIS_CHART_WITH_RATIO;
            scaleFactor = Math.round(scaleFactor * 100.0) / 100.0;

            snapshotParameters.setTransform(Transform.scale(scaleFactor, scaleFactor));

            double currentWidth = chartComponent.getWidth();
            double currentHeight = chartComponent.getHeight();
            // out of the layout of the window, which would give it its size back or make room for it
            chartComponent.setManaged(false);
            try {
                fitInto(targetWidth / scaleFactor, targetHeight / scaleFactor, 1 / scaleFactor);

                Bounds bounds = chartComponent.getBoundsInParent();
                snapshotParameters.setViewport(new Rectangle2D(Math.floor(bounds.getMinX() * scaleFactor),
                        Math.floor(bounds.getMinY() * scaleFactor), targetWidth, targetHeight));
                image = chartComponent.snapshot(snapshotParameters, new WritableImage((int) targetWidth, (int) targetHeight));
            } finally {
                chartComponent.resize(currentWidth, currentHeight);
                chartComponent.setManaged(true);
            }

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

    /**
     * Gives the chart a size in which all of it fits into the size of the picture. The chart is
     * larger than its own size says: the label at the end of the time axis sticks out of it. By how
     * much is known when it is laid out, and may change when the chart gets smaller.
     *
     * @param pixel the size of a pixel of the picture, which the picture may be shifted by
     */
    private void fitInto(double width, double height, double pixel) {
        double chartWidth = width;
        double chartHeight = height;
        for (int attempt = 0; attempt < MAX_ATTEMPTS_TO_FIT; attempt++) {
            chartComponent.resize(chartWidth, chartHeight);
            chartComponent.applyCss();
            chartComponent.layout();

            Bounds bounds = chartComponent.getBoundsInParent();
            double tooWide = bounds.getWidth() + pixel - width;
            double tooHigh = bounds.getHeight() + pixel - height;
            if (tooWide <= 0 && tooHigh <= 0) {
                return;
            }
            chartWidth -= Math.max(0, tooWide);
            chartHeight -= Math.max(0, tooHigh);
        }
    }
}

package io.softwareecg.collector.ui;

import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

/**
 * The values of a metric as a small curve beside its number: not to be read off, but to see
 * at a glance whether it is flat, climbing or jumping.
 */
final class Sparkline extends Canvas {

    private static final Color LINE = Color.web("#35c8f5");

    private static final Color LAST = Color.web("#b6f1ff");

    private static final Color BASE = Color.web("#ffffff", 0.10);

    Sparkline(double width, double height) {
        super(width, height);
    }

    /**
     * @param values the values, oldest first; {@link Double#NaN} where one is missing
     */
    void draw(double[] values) {
        GraphicsContext graphics = getGraphicsContext2D();
        double width = getWidth();
        double height = getHeight();
        graphics.clearRect(0, 0, width, height);

        graphics.setStroke(BASE);
        graphics.setLineWidth(1);
        graphics.strokeLine(0, height - 0.5, width, height - 0.5);

        double min = Double.NaN;
        double max = Double.NaN;
        for (double value : values) {
            if (!Double.isNaN(value)) {
                min = Double.isNaN(min) ? value : Math.min(min, value);
                max = Double.isNaN(max) ? value : Math.max(max, value);
            }
        }
        if (Double.isNaN(min)) {
            return;
        }

        // The curve always has the width of a full history, so that it grows from the left
        // and then moves, instead of being stretched while it is short.
        double step = (width - 6) / (DisplayModel.HISTORY - 1);
        double range = max - min;
        double top = 4;
        double bottom = height - 5;

        graphics.setStroke(LINE);
        graphics.setLineWidth(2);
        double lastX = Double.NaN;
        double lastY = Double.NaN;
        boolean drawing = false;
        graphics.beginPath();
        for (int i = 0; i < values.length; i++) {
            if (Double.isNaN(values[i])) {
                drawing = false;   // a gap where a value is missing
                continue;
            }
            double x = 3 + i * step;
            double y = range == 0 ? (top + bottom) / 2 : bottom - (values[i] - min) / range * (bottom - top);
            if (drawing) {
                graphics.lineTo(x, y);
            } else {
                graphics.moveTo(x, y);
                // a value between two gaps would otherwise not be seen
                graphics.lineTo(x + 0.01, y);
                drawing = true;
            }
            lastX = x;
            lastY = y;
        }
        graphics.stroke();

        graphics.setFill(LAST);
        graphics.fillOval(lastX - 3, lastY - 3, 6, 6);
    }
}

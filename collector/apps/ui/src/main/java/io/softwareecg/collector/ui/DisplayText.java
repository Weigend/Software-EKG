package io.softwareecg.collector.ui;

import java.time.Duration;
import java.time.Instant;
import java.util.Locale;

/**
 * Numbers and times as they are written on the display: short, so that they can be read from a
 * step away.
 */
final class DisplayText {

    /** What stands where there is no value. */
    static final String NONE = "–";

    private DisplayText() {
    }

    /**
     * @return the value with as many digits as tell something: 0.25, 12.5, 1 250, 12.5 k, 3.4 M
     */
    static String value(double value) {
        if (Double.isNaN(value) || Double.isInfinite(value)) {
            return NONE;
        }
        double size = Math.abs(value);
        if (size >= 1e12) {
            return scaled(value / 1e12) + " T";
        }
        if (size >= 1e9) {
            return scaled(value / 1e9) + " G";
        }
        if (size >= 1e6) {
            return scaled(value / 1e6) + " M";
        }
        if (size >= 1e4) {
            return scaled(value / 1e3) + " k";
        }
        if (value == Math.rint(value)) {
            return String.format(Locale.ROOT, "%.0f", value);
        }
        if (size >= 100) {
            return String.format(Locale.ROOT, "%.1f", value);
        }
        if (size >= 1) {
            return String.format(Locale.ROOT, "%.2f", value);
        }
        return String.format(Locale.ROOT, "%.3f", value);
    }

    private static String scaled(double value) {
        return String.format(Locale.ROOT, Math.abs(value) >= 100 ? "%.0f" : Math.abs(value) >= 10 ? "%.1f" : "%.2f", value);
    }

    /**
     * @return how long ago that was: "now", "12 s ago", "5 min ago", "3 h ago", "2 d ago"
     */
    static String ago(Instant time, Instant now) {
        if (time == null) {
            return NONE;
        }
        long seconds = Math.max(0, Duration.between(time, now).getSeconds());
        if (seconds < 2) {
            return "now";
        }
        if (seconds < 120) {
            return seconds + " s ago";
        }
        if (seconds < 7_200) {
            return seconds / 60 + " min ago";
        }
        if (seconds < 172_800) {
            return seconds / 3_600 + " h ago";
        }
        return seconds / 86_400 + " d ago";
    }

    /**
     * @return a span of time: "45 s", "12 min", "3 h 20 min", "2 d 4 h"
     */
    static String span(Duration duration) {
        long seconds = Math.max(0, duration.getSeconds());
        if (seconds < 60) {
            return seconds + " s";
        }
        if (seconds < 3_600) {
            return seconds / 60 + " min";
        }
        if (seconds < 86_400) {
            return seconds / 3_600 + " h " + (seconds % 3_600) / 60 + " min";
        }
        return seconds / 86_400 + " d " + (seconds % 86_400) / 3_600 + " h";
    }
}

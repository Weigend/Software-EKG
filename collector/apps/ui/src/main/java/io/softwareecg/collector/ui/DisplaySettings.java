package io.softwareecg.collector.ui;

import io.softwareecg.collector.common.CollectorProperties;

import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * How the display takes its screen, as collector.properties says:
 * <pre>
 *   io.softwareecg.collector.ui.enabled=true       show the display at all
 *   io.softwareecg.collector.ui.fullscreen=true    take the whole screen, as a panel does
 *   io.softwareecg.collector.ui.size=800x480       the size of the window otherwise
 * </pre>
 *
 * @param fullScreen whether the display takes the whole screen
 * @param width      the width of its window otherwise
 * @param height     the height of its window otherwise
 */
record DisplaySettings(boolean fullScreen, int width, int height) {

    static final String FULLSCREEN_KEY = "io.softwareecg.collector.ui.fullscreen";

    static final String SIZE_KEY = "io.softwareecg.collector.ui.size";

    /** The size of the small touch screens the display is made for. */
    static final int DEFAULT_WIDTH = 800;

    static final int DEFAULT_HEIGHT = 480;

    private static final Pattern SIZE = Pattern.compile("(\\d{3,5})\\s*[xX]\\s*(\\d{3,5})");

    static DisplaySettings configured() {
        return of(CollectorProperties.getProperties());
    }

    /**
     * @throws IllegalStateException if the size is not written as width x height
     */
    static DisplaySettings of(Properties properties) {
        boolean fullScreen = Boolean.parseBoolean(properties.getProperty(FULLSCREEN_KEY, "false").strip());
        String size = properties.getProperty(SIZE_KEY);
        if (size == null || size.isBlank()) {
            return new DisplaySettings(fullScreen, DEFAULT_WIDTH, DEFAULT_HEIGHT);
        }
        Matcher matcher = SIZE.matcher(size.strip());
        if (!matcher.matches()) {
            throw new IllegalStateException("The setting " + SIZE_KEY + "=" + size + " is not a size. Write it as "
                    + "width x height in pixels, for example 800x480.");
        }
        return new DisplaySettings(fullScreen, Integer.parseInt(matcher.group(1)), Integer.parseInt(matcher.group(2)));
    }
}

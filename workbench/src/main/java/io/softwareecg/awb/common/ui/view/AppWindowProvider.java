package io.softwareecg.awb.common.ui.view;

import javafx.stage.Window;

/**
 * A simple helper class that
 *
 * @since 04.03.2020
 */
public class AppWindowProvider {

    private final Window appWindow;

    /**
     * Creates a new AppWindowProvider instance that
     * is responsible to serve the Window instance given
     * via constructor parameter.
     *
     * @param appWindow the Window instance that should provided by this class
     */
    public AppWindowProvider(Window appWindow) {
        this.appWindow = appWindow;
    }

    public Window getAppWindow() {
        return appWindow;
    }
}

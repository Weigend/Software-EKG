package io.softwareecg.collector.control;

import java.io.File;

/**
 * Something that shows what a running stand-alone collector is doing: a display on the machine
 * it runs on, for example.
 * <p>
 * A collector runs without one. It starts a frontend only if it is told to
 * ({@code io.softwareecg.collector.ui.enabled=true} in collector.properties, or the option
 * {@code --ui} of the start script), and takes the one it finds with the Java
 * {@link java.util.ServiceLoader}. A frontend that cannot start - no display, for one - does not
 * keep the collector from collecting.
 */
public interface CollectorFrontend {

    /**
     * Starts the frontend. Returns as soon as it is started; it runs on threads of its own.
     *
     * @param engine        the engine whose jobs are shown
     * @param configuration the configuration the collector runs with
     * @throws RuntimeException if the frontend cannot start on this machine
     */
    void start(CollectorEngine engine, File configuration);

    /**
     * Ends the frontend when the collector ends.
     */
    void stop();
}

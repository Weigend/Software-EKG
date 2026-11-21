//______________________________________________________________________________
//
//          Project:    Software EKG
//______________________________________________________________________________
//
//       created by:    Johannes Weigend, Josef Adersberger
//       creation date: 2009
//       changed by:    $Author: weigend $
//       change date:   $Date: 2010-07-21 13:03:04 +0200 (Mi, 21 Jul 2010) $
//       revision:      $Revision: 1082 $
//       description:   Reads Windows Performance MetricDefinition by using a custom
//                      jni dll. Reads JMX Memory MetricDefinition.
//______________________________________________________________________________
//
//        Copyright:    Weigend AM
//______________________________________________________________________________

package io.softwareecg.collector.control;

import io.softwareecg.collector.common.CollectorProperties;
import io.softwareecg.collector.common.Console;
import io.softwareecg.collector.common.LoggerFactory;
import io.softwareecg.collector.common.Registry;
import io.softwareecg.collector.config.CollectorConfiguration;
import io.softwareecg.collector.config.CollectorConfigurations;
import io.softwareecg.collector.connector.ICollectorConnector;
import org.slf4j.Logger;

import java.io.File;
import java.util.Iterator;
import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;


/**
 * Logs performance data from different sources to a configurable storage.
 *
 */
public final class CollectorCore {

    /**
     * the logger used by the CollectorCore
     */
    private static final Logger LOGGER = LoggerFactory.create();

    /**
     * The engine that runs the collector jobs of the stand-alone collector.
     */
    private static final CollectorEngine ENGINE = new CollectorEngine();

    /**
     * Our connector
     */
    private static final ICollectorConnector connector = Registry.getDefault().lookup(ICollectorConnector.class);

    /**
     * The system property that switches the display of the collector on, as the option --ui of
     * the start script sets it. The same as io.softwareecg.collector.ui.enabled in
     * collector.properties.
     */
    public static final String UI_SWITCH = "collector.ui";

    /**
     * What shows the collector on a display of its machine, if that is switched on.
     */
    private static volatile CollectorFrontend frontend;

    /**
     * Private constructor because the
     * core should use in static way only
     */
    private CollectorCore() {
        // no op
    }

    /**
     * The main function that will initialize and run the collector jobs depending
     * on the specified configuration.
     *
     * The function expects up to two parameters in the argument array and follow the following behaviour:
     *
     * 0 args: default collector config and default log directory/storage will be used
     * 1 arg: argument will interpreted as path to the collector config that will use for the jobs
     * 2 args: first argument will interpreted as path to the collector config that will use for the jobs, the second
     * will interpreted as path to the log directory in case of CSV logs.
     *
     * @param args an array with up to two program arguments that include the config and log path
     */
    public static void main(final String[] args) {

        try {
            // more than one two parameters are not supported
            if (args.length > 2) {
                printUsage();
                return;
            }

            // get default config or the specified one
            final File configFile = resolveConfigFile(args);
            if (configFile == null) {
                return;
            }

            // optional log directory as second parameter
            setupCollectorLogPath(args);

            // print config status
            LOGGER.info("Using config file: '" + configFile.getAbsolutePath()
                    + "'. Using log base directory: '" + CollectorProperties.getLogdir());

            final CollectorConfiguration config = CollectorConfigurations.fromFile(configFile);
            ENGINE.start(config);

            if (connector != null) {
                connector.start();
            }

            startFrontendIfSwitchedOn(configFile);

            // wait until all collector jobs finished
            ENGINE.awaitTermination();

        } catch (Exception e) {
            LOGGER.error("Collector will be stopped - Exception reached outer control.", e);


            while(!Thread.currentThread().isInterrupted()) {
                try {
                    LOGGER.error("Collector waits for explicit close command. ", e);
                    Thread.sleep(5000);
                } catch (InterruptedException e1) {
                    return;
                }
            }
        }
    }


    /**
     * Parses the program arguments for collector log path and
     * update the setup if necessary.
     *
     * @param args the program arguments as String array
     */
    private static void setupCollectorLogPath(String[] args) {
        if (args.length == 2) {
            CollectorProperties.setLogdir(args[1]);
        }
    }

    /**
     * Parses the program arguments for the config path that points to the collector config
     * and validate the path if specified. If no config file specified, a default will used.
     *
     * @param args the program arguments as String array
     * @return a File instance that points to the collector config or null if no valid path could resolved.
     */
    public static File resolveConfigFile(String[] args) {
        String configFilePath;

        if (args.length == 1 || args.length == 2) {
            // first parameter = path to config file
            configFilePath = args[0];
        } else {
            configFilePath = System.getProperty("config", "config/default/config.yaml");
        }

        File configFile = new File(configFilePath);

        if (!configFile.exists()) {
            LOGGER.info("The specified config file: '" + configFile.getAbsolutePath()
                    + "' doesn't exist. Please double check the path and correct the configuration.");
            return null;
        }

        if (!configFile.canRead()) {
            LOGGER.info("The specified config file: '" + configFile.getAbsolutePath()
                    + "' cannot read (no permissions).");
            return null;
        }

        return configFile;
    }


    /**
     * Prints the usage of collector (correct program arguments that should specified)
     */
    private static void printUsage() {
        Console.getErr().println("Usage: collector <configfile> [logdir]");
    }

    /**
     * Halt all jobs.
     */
    public static void stop() {
        ENGINE.stop();
        connector.stop();
        CollectorFrontend shown = frontend;
        if (shown != null) {
            shown.stop();
        }
    }

    /**
     * A collector runs without a display unless it is told otherwise. One that cannot be started
     * - it is not part of the installation, or the machine has no screen - is said so, and the
     * collector goes on without it: collecting is what it is for.
     */
    private static void startFrontendIfSwitchedOn(File configFile) {
        if (!Boolean.getBoolean(UI_SWITCH) && !CollectorProperties.isUiEnabled()) {
            return;
        }
        try {
            Iterator<CollectorFrontend> found = ServiceLoader.load(CollectorFrontend.class).iterator();
            if (!found.hasNext()) {
                LOGGER.error("The display of the collector is switched on, but this installation has none. "
                        + "The collector runs without it.");
                return;
            }
            CollectorFrontend display = found.next();
            display.start(ENGINE, configFile);
            frontend = display;
            LOGGER.info("The display of the collector is started.");
        } catch (RuntimeException | LinkageError | ServiceConfigurationError e) {
            Throwable root = e;
            while (root.getCause() != null && root.getCause() != root) {
                root = root.getCause();
            }
            LOGGER.error("The display of the collector cannot be started, the collector runs without it: {}",
                    root.getMessage() != null ? root.getMessage() : root.toString());
            LOGGER.debug("Starting the display failed.", e);
        }
    }

    /**
     * @return whether the stand-alone collector runs and the state of each of its jobs
     */
    public static CollectorStatus getStatus() {
        return ENGINE.getStatus();
    }
}
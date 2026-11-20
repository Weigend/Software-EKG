//______________________________________________________________________________
//
//          Project:    Software EKG
//______________________________________________________________________________
//
//       created by:    Johannes Weigend, Josef Adersberger
//       creation date: 2009
//       changed by:    $Author: pospiech $
//       change date:   $Date: 2009-12-16 01:47:26 +0100 (Mi, 16 Dez 2009) $
//       revision:      $Revision: 190 $
//       description:   Starter class for the collector.
//______________________________________________________________________________
//
//        Copyright:    Weigend AM
//______________________________________________________________________________

package io.softwareecg.collector;

import io.softwareecg.collector.common.CollectorVersion;
import io.softwareecg.collector.config.CollectorConfigurations;
import io.softwareecg.collector.config.InvalidConfigurationException;
import io.softwareecg.collector.common.Console;
import io.softwareecg.collector.common.LoggerFactory;
import io.softwareecg.collector.control.CollectorCore;
import org.slf4j.Logger;

import java.io.File;


/**
 * Starter class for the collector.
 *
 */
public final class StartCollector {

    /**
     * Our logger
     */
    private static final Logger LOGGER = LoggerFactory.create();

    static {
        // LogManager in jdk 6.18 - 6.23 leaks
        //System.setProperty("java.datamanagement.logging.manager", "org.apache.juli.ClassLoaderLogManager");
    }

    /**
     * Utility class.
     */
    private StartCollector() {

    }

    /**
     * Main.
     *
     * @param args the arguments.
     */
    public static void main(String[] args) {


        try {
            if (args.length > 0) {
                System.setProperty("collector.runArg.configPath", args[0]);
            }
            // A configuration that is not valid ends the start, with what is wrong in it as the
            // last thing printed - not as one line of a log that goes on.
            File configFile = CollectorCore.resolveConfigFile(args);
            if (configFile != null) {
                try {
                    CollectorConfigurations.fromFile(configFile);
                } catch (InvalidConfigurationException e) {
                    Console.getOut().println(e.getMessage());
                    System.exit(1);
                }
            }


            String product = ("Software EKG " + CollectorVersion.get()).strip();
            String logo = "\n" +
                    " _____ _  ______    ____      _ _           _             \n" +
                    "| ____| |/ / ___|  / ___|___ | | | ___  ___| |_ ___  _ __ \n" +
                    "|  _| | ' / |  _  | |   / _ \\| | |/ _ \\/ __| __/ _ \\| '__|\n" +
                    "| |___| . \\ |_| | | |__| (_) | | |  __/ (__| || (_) | |   \n" +
                    "|_____|_|\\_\\____|  \\____\\___/|_|_|\\___|\\___|\\__\\___/|_|   " +
                    "\n" + " ".repeat(Math.max(0, 55 - product.length())) + product + "\n";
            Console.getOut().println(logo);

            CollectorCore.main(args);

        } catch (Exception e) {
            LOGGER.error("Collector shutdown. Exception reached outer handler.", e);

            while(!Thread.currentThread().isInterrupted()) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e1) {
                    return;
                }
            }
        }
    }
}

//______________________________________________________________________________
//
//          Project:    Software EKG
//______________________________________________________________________________
//
//         Author:      Weigend AM 2009 - 2014
//______________________________________________________________________________
//
// Notice: This piece of software was created, designed and implemented by
// experienced craftsmen and innovators in Munich, Germany.
// Changes should be done with respect to the original design.
//______________________________________________________________________________
package io.softwareecg.collector.common;

import org.apache.commons.io.output.NullOutputStream;
import org.slf4j.LoggerFactory;

import java.io.PrintStream;
import java.util.logging.Level;


/**
 * Simple class to disable logging and std output (stderr and stdout) when doing exceptional Tests.
 * Created by johannes.weigend on 18.04.14.
 */
public class LoggingControl {

    private final java.util.logging.Level currentJuliLevel;
    private final java.util.logging.Logger juliRootLogger;

    private final ch.qos.logback.classic.Logger logbackRootLogger;
    private final ch.qos.logback.classic.Level currentLogbackLevel;

    private final PrintStream currentStdOut;
    private final PrintStream currentStdErr;

    /**
     * Makes a instance which records the current std oubput and logging configuration;
     */
    public LoggingControl() {

        juliRootLogger = java.util.logging.LogManager.getLogManager().getLogger("");
        currentJuliLevel = juliRootLogger.getLevel();

        logbackRootLogger = (ch.qos.logback.classic.Logger)LoggerFactory.getLogger(ch.qos.logback.classic.Logger.ROOT_LOGGER_NAME);
        currentLogbackLevel = logbackRootLogger.getLevel();

        currentStdOut = System.out;
        currentStdErr = System.err;
    }

    /**
     * Switch logging off.
     */
    public void makeQuit() {
        juliRootLogger.setLevel(Level.OFF);
        logbackRootLogger.setLevel(ch.qos.logback.classic.Level.OFF);
        System.setOut(new PrintStream(new NullOutputStream()));
        System.setErr(new PrintStream(new NullOutputStream()));
    }

    /**
     * restores all logging and output streams.
     */
    public void restore() {
        juliRootLogger.setLevel(currentJuliLevel);
        logbackRootLogger.setLevel(currentLogbackLevel);
        System.setErr(currentStdErr);
        System.setOut(currentStdOut);
    }
}

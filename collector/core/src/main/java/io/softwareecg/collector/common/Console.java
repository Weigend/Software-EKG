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

import java.io.PrintStream;


/**
 * Simple console wrapper to avoid sonar errors by accessing Console.getOut() and Console.getErr().
 */
public final class Console {

    /**
     * The console.
     */
    private static final Console console = new Console();

    /**
     * The out.
     */
    private final PrintStream out;

    /**
     * The err.
     */
    private final PrintStream err;

    /**
     * Instantiates a new console.
     */
    @SuppressWarnings("all")
    private Console() {
        out = System.out;
        err = System.err;
    }

    /**
     * Gets the out.
     *
     * @return the out
     */
    public static PrintStream getOut() {
        return console.out;
    }

    /**
     * Gets the err.
     *
     * @return the err
     */
    public static PrintStream getErr() {
        return console.err;
    }
}

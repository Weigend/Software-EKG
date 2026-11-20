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


import org.slf4j.Logger;

/**
 * Simple console progress bar. Never use inside a webapp. The
 * The progress-sign ist intended for console standalone tools.
 */
public class TextProgress {

    /**
     * Our logger.
     */
    private static final Logger LOGGER = LoggerFactory.create();

    /**
     * The Constant SYMBOLS.
     */
    private static final String[] SYMBOLS = {"\b-", "\b\\" + "\b|", "\b/"};

    /**
     * The current count.
     */
    private int currentCount;

    /**
     * The progress thread.
     */
    private Thread progressThread;

    /**
     * Next.
     */
    public void next() {
        int idx = currentCount++ % SYMBOLS.length;
        Console.getOut().print(SYMBOLS[idx]);
    }

    /**
     * Start.
     */
    public void start() {
        progressThread = new ProgressThread();
        progressThread.setDaemon(true);
        progressThread.start();
    }

    /**
     * Stop.
     */
    public void stop() {
        if (progressThread == null) {
            throw new IllegalStateException("No progress thread running. Forgot start() ?");
        }
        progressThread.interrupt();
        Console.getOut().print("\b.");
    }

    /**
     * The Class ProgressThread.
     */
    private class ProgressThread extends Thread {
        /**
         * {@inheritDoc}
         */
        @Override
        public void run() {
            while (!isInterrupted()) {
                next();
                try {
                    Thread.sleep(200);
                } catch (InterruptedException e) {
                    LOGGER.warn("Progress was interrupted.", e);
                    break;
                }
            }
            Console.getOut().print("\b ");
        }
    }
}

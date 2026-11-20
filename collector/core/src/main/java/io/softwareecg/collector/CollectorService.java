package io.softwareecg.collector;

import io.softwareecg.collector.common.LoggerFactory;
import io.softwareecg.collector.control.CollectorCore;
import org.slf4j.Logger;

import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Start class if collector is started a a Windows service.
 *
 * @since 29.05.2015
 */
public final class CollectorService {

    private static final Logger LOGGER = LoggerFactory.create();

    /**
     * Flag that shows if the collector is currently running.
     */
    private static final AtomicBoolean started = new AtomicBoolean(false);
    /**
     * If activated the latch signals that the collector should stop.
     */
    private static final CountDownLatch stopLatch = new CountDownLatch(1);


    private CollectorService() {
        // hide constructor
    }

    /**
     * Start and stop the EKG-Collector as a service.
     *
     * @param args the cli arguments
     */
    public static void main(final String[] args) {
        if (args.length < 1) {
            LOGGER.error("Invalid arguments. Missing service control command.");
        }

        final String controlCommand = args[0];

        if ("start".equalsIgnoreCase(controlCommand)) {
            if (!started.compareAndSet(false, true)) {
                LOGGER.info("Collector already started. Nothing to do.");
                return;
            }

            // Remove first argument (service control command) and start collector with the remaining args:
            startCollector(Arrays.copyOfRange(args, 1, args.length));

            started.set(false);

            // Force termination of JVM in case there are still running threads.
            System.exit(0);

        } else if ("stop".equalsIgnoreCase(controlCommand)) {
            LOGGER.info("Stopping Collector...");
            CollectorCore.stop();
            stopLatch.countDown();

        } else {
            LOGGER.error("Unknown service control command: " + controlCommand);
        }
    }

    private static void startCollector(final String[] args) {
        LOGGER.info("Staring Collector...");
        Thread mainThread = new Thread(() -> {
            CollectorCore.main(args);
            stopLatch.countDown();
        });

        mainThread.start();

        try {
            // wait until CollectorCore.main() finished and count down the latch
            stopLatch.await();

            // Wait at most 7 seconds for all collector threads to terminate:
            mainThread.join(TimeUnit.SECONDS.toMillis(7));

        } catch (InterruptedException e) {
            throw new IllegalStateException(e);
        }

        LOGGER.info("Collector Stopped.");
    }
}

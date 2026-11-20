package io.softwareecg.collector.common;

/**
 * Tiny helper that can use to sleep with a thread without
 * writing the exception block clue code.
 *
 * @since 28.09.2018
 */
public class SleepUtil {

    /**
     * Sleeps the specified time in milliseconds
     *
     * @param sleep the time to sleep in ms
     */
    public static void sleep(long sleep) {
        try {
            Thread.sleep(sleep);
        } catch (InterruptedException e1) {
            throw new IllegalStateException(e1);
        }
    }
}

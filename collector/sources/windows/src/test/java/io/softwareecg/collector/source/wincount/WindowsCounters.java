package io.softwareecg.collector.source.wincount;

import org.junit.Assert;
import org.junit.Assume;

/**
 * Decides whether a test that reads Windows performance counters runs.
 */
final class WindowsCounters {

    /**
     * Set by the build that makes the native library (profile {@code wpc}): on Windows a
     * library that cannot be loaded is then a failure, not a reason to skip the test.
     */
    private static final String REQUIRED = "wpc.required";

    private WindowsCounters() {
    }

    /**
     * Skips the calling test if it does not run on Windows or if the native library is not built.
     */
    static void assumeAvailable() {
        Assume.assumeTrue("not on Windows", System.getProperty("os.name").startsWith("Win"));
        boolean available = new WindowsPerformanceCounterAccess().isAvailable();
        if (Boolean.getBoolean(REQUIRED)) {
            Assert.assertTrue("The native library wpcaccess is not loaded although this build made it."
                    + " java.library.path=" + System.getProperty("java.library.path"), available);
        }
        Assume.assumeTrue("the native library wpcaccess is not built (mvn -Pwpc)", available);
    }
}

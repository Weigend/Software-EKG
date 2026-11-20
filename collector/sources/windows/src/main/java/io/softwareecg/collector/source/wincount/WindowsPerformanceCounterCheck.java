package io.softwareecg.collector.source.wincount;

import java.net.InetAddress;

/**
 * Reads Windows performance counters once and prints them. It shows whether the native library
 * is found and works with the Java runtime, the classpath and the library path at hand:
 * <pre>
 * jre\bin\java -cp "lib\*" -Djava.library.path=bin\dlls
 *     io.softwareecg.collector.source.wincount.WindowsPerformanceCounterCheck [counter ...]
 * </pre>
 * A counter is written without the machine, e.g. {@code \System\Threads}; without arguments
 * that one is read. The exit code is 0 if every counter has a value.
 */
public final class WindowsPerformanceCounterCheck {

    private static final String DEFAULT_COUNTER = "\\System\\Threads";

    private WindowsPerformanceCounterCheck() {
    }

    /**
     * Reads the counters and prints them.
     *
     * @param args the counters, or none for {@value #DEFAULT_COUNTER}
     * @throws Exception if the name of this machine is not known
     */
    @SuppressWarnings("PMD.SystemPrintln")
    public static void main(String[] args) throws Exception {
        WindowsPerformanceCounterAccess access = new WindowsPerformanceCounterAccess();
        if (!access.isAvailable()) {
            System.err.println("The native library of the Windows performance counters is not loaded."
                    + " java.library.path=" + System.getProperty("java.library.path"));
            System.exit(1);
        }
        String machine = "\\\\" + InetAddress.getLocalHost().getHostName();
        boolean complete = true;
        for (String counter : args.length > 0 ? args : new String[]{DEFAULT_COUNTER}) {
            double value;
            try {
                value = access.getCounter(machine + counter);
            } catch (RuntimeException e) {
                System.err.println(machine + counter + ": " + e.getMessage());
                complete = false;
                continue;
            }
            System.out.println(machine + counter + " = " + value);
            complete &= value >= 0;
        }
        System.exit(complete ? 0 : 2);
    }
}

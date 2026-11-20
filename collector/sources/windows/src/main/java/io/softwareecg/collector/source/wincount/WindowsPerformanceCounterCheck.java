package io.softwareecg.collector.source.wincount;

import java.util.List;

/**
 * Reads Windows performance counters once and prints them. It shows whether the native library
 * is found and works with the Java runtime, the classpath and the library path at hand:
 * <pre>
 * jre\bin\java -cp "lib\*" -Djava.library.path=bin\dlls
 *     io.softwareecg.collector.source.wincount.WindowsPerformanceCounterCheck [counter ...]
 * </pre>
 * A counter is written without the machine, with its English names or with those of this
 * Windows, e.g. {@code \System\Threads}; without arguments that one is read. The exit code is 0
 * if every counter has a value.
 */
public final class WindowsPerformanceCounterCheck {

    private static final String DEFAULT_COUNTER = "\\System\\Threads";

    private WindowsPerformanceCounterCheck() {
    }

    /**
     * Reads the counters and prints them.
     *
     * @param args the counters, or none for {@value #DEFAULT_COUNTER}
     */
    @SuppressWarnings("PMD.SystemPrintln")
    public static void main(String[] args) {
        if (!WindowsCounterProbe.isAvailable()) {
            System.err.println("The native library of the Windows performance counters is not loaded."
                    + " java.library.path=" + System.getProperty("java.library.path"));
            System.exit(1);
        }
        List<String> counters = args.length > 0 ? List.of(args) : List.of(DEFAULT_COUNTER);
        double[] values = WindowsCounterProbe.read("localhost", counters);
        boolean complete = true;
        for (int i = 0; i < values.length; i++) {
            System.out.println(counters.get(i) + " = " + values[i]);
            complete &= values[i] >= 0;
        }
        System.exit(complete ? 0 : 2);
    }
}

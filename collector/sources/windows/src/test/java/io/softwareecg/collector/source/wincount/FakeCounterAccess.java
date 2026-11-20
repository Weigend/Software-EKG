package io.softwareecg.collector.source.wincount;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A Windows that is not there: it names its counters in German and knows the ones it is told.
 * Lets the Java side of the source be tested on every system.
 */
class FakeCounterAccess extends WindowsPerformanceCounterAccess {

    /** number, English name, German name */
    private static final String[][] NAMES = {
            {"1", "1847", "1847"},
            {"2", "System", "System"},
            {"4", "Memory", "Arbeitsspeicher"},
            {"6", "% Processor Time", "Prozessorzeit (%)"},
            {"24", "Available Bytes", "Verfügbare Bytes"},
            {"230", "Process", "Prozess"},
            {"238", "Processor", "Prozessor"},
            {"264", "Bytes Received/sec", "Empfangene Bytes/s"},
            {"510", "Network Interface", "Netzwerkschnittstelle"},
            // a second number for a name that is there already
            {"1848", "% Processor Time", "Prozessorzeit (%)"},
    };

    /** What a path with wildcards stands for. */
    final Map<String, List<String>> expansions = new HashMap<>();

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public String[] englishNames() {
        String[] list = new String[NAMES.length * 2];
        for (int i = 0; i < NAMES.length; i++) {
            list[2 * i] = NAMES[i][0];
            list[2 * i + 1] = NAMES[i][1];
        }
        return list;
    }

    @Override
    public String lookupName(int index) {
        for (String[] name : NAMES) {
            if (name[0].equals(String.valueOf(index))) {
                return name[2];
            }
        }
        return null;
    }

    @Override
    public String[] resolveWildcards(String counterName) {
        List<String> counters = expansions.get(counterName);
        if (counters == null) {
            throw new IllegalStateException("no such counters: " + counterName);
        }
        return counters.toArray(new String[0]);
    }
}

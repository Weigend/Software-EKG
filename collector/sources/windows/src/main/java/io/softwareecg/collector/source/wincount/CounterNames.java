package io.softwareecg.collector.source.wincount;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Turns counter paths written with English names into the names of the Windows they are read
 * on. Windows names its performance objects and counters in its own language - the processor
 * time is {@code \Processor(_Total)\% Processor Time} on an English and
 * {@code \Prozessor(_Total)\Prozessorzeit (%)} on a German system - but it numbers them the
 * same everywhere. A name is therefore looked up in the English list for its number, and the
 * number for its name on this machine.
 * <p>
 * A name that is not an English one stays as it is, so a path in the language of the machine
 * works as before. The names are those of the machine the collector runs on; another machine
 * that is read from here has to speak the same language.
 */
final class CounterNames {

    private final WindowsPerformanceCounterAccess access;

    /** The numbers of every English name, in lower case. Read when first needed. */
    private Map<String, List<Integer>> indexes;

    private final Map<String, String> localNames = new ConcurrentHashMap<>();

    CounterNames(WindowsPerformanceCounterAccess access) {
        this.access = access;
    }

    /**
     * @param path a counter path without a machine: {@code \Object\Counter} or
     *             {@code \Object(Instance)\Counter}
     * @return the path with the names of its object and its counter in the language of this
     *         Windows; unchanged if it is no such path
     */
    String localize(String path) {
        if (path == null || !path.startsWith("\\") || path.startsWith("\\\\") || !access.isAvailable()) {
            return path;
        }
        int slash = path.indexOf('\\', 1);
        if (slash < 0) {
            return path;
        }
        int parenthesis = path.indexOf('(');
        String object;
        String instance = null;
        String counter;
        if (parenthesis > 0 && parenthesis < slash) {
            // an instance may contain what it likes, but it ends where the counter begins
            int end = path.indexOf(")\\", parenthesis);
            if (end < 0) {
                return path;
            }
            object = path.substring(1, parenthesis);
            instance = path.substring(parenthesis + 1, end);
            counter = path.substring(end + 2);
        } else {
            object = path.substring(1, slash);
            counter = path.substring(slash + 1);
        }
        return "\\" + localName(object) + (instance == null ? "" : "(" + instance + ")") + "\\" + localName(counter);
    }

    /**
     * @return the name of an object or a counter in the language of this Windows; the name
     *         itself if it is not an English one, or a wildcard
     */
    String localName(String name) {
        if (name.isEmpty() || name.indexOf('*') >= 0) {
            return name;
        }
        return localNames.computeIfAbsent(name, this::lookUp);
    }

    private String lookUp(String name) {
        List<Integer> numbers = indexes().get(name.toLowerCase(Locale.ROOT));
        if (numbers != null) {
            for (int number : numbers) {
                String local = access.lookupName(number);
                if (local != null && !local.isBlank()) {
                    return local;
                }
            }
        }
        return name;
    }

    private synchronized Map<String, List<Integer>> indexes() {
        if (indexes == null) {
            Map<String, List<Integer>> read = new HashMap<>();
            String[] list = access.englishNames();
            // number, name, number, name ...
            for (int i = 0; list != null && i + 1 < list.length; i += 2) {
                try {
                    int number = Integer.parseInt(list[i].trim());
                    read.computeIfAbsent(list[i + 1].toLowerCase(Locale.ROOT), key -> new ArrayList<>(1)).add(number);
                } catch (NumberFormatException e) {
                    // not a pair of number and name: nothing to learn from it
                }
            }
            indexes = read;
        }
        return indexes;
    }
}

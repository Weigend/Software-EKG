package io.softwareecg.collector.source.unix.analyzer;

import io.softwareecg.collector.source.CounterSourceListener;

import java.util.ArrayList;
import java.util.Collection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @since 07.03.13
 */
abstract class AbstractTerminalAnalyzer implements ITerminalOutAnalyzer {

    protected String[] terminalOutput = null;

    /**
     * a collection with CounterSourceListener that will triggered if the process list changes and wildcards used.
     */
    protected Collection<CounterSourceListener> counterSourceListeners = new ArrayList<>();

    protected static final String SPACES_REGEX = "\\s+";

    /**
     * The Constant COUNTER_SEARCHGROUP_PATTERN.
     */
    protected static final Pattern COUNTER_SEARCHGROUP_PATTERN = Pattern.compile("\\\\(.*)\\\\.*");

    /**
     * The Constant COUNTER_SEARCHCOUNTER_PATTERN.
     */
    protected static final Pattern COUNTER_SEARCHCOUNTER_PATTERN = Pattern.compile("\\\\.*\\\\(.*)");


    /**
     * Extract by regex.
     *
     * @param name  the name
     * @param regex the regex
     * @param group the group
     * @return the string
     */
    @SuppressWarnings("SameParameterValue")
    protected String extractByRegex(String name, String regex, int group) {
        Pattern pattern = Pattern.compile(regex);
        return extractByRegex(name, pattern, group);
    }

    /**
     * Extract by regex.
     *
     * @param name    the name
     * @param pattern the pattern
     * @param group   the group
     * @return the string
     */
    protected String extractByRegex(String name, Pattern pattern, int group) {
        Matcher matcher = pattern.matcher(name);
        if (!matcher.matches()) {
            return null;
        }

        return matcher.group(group);
    }

    /**
     * Helper to extract the last part of the string (counter):  /group/counter.
     *
     * @param name the full counter with group.
     * @return the counter w/o group.
     */

    protected String extractCounter(String name) {
        return extractByRegex(name, COUNTER_SEARCHCOUNTER_PATTERN, 1);
    }
}

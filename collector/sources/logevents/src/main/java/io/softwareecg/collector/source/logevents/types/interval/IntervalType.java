/*
 * ______________________________________________________________________________
 *
 * Project: Software EKG
 * ______________________________________________________________________________
 *
 * created by: f.lautenschlager
 * creation date: 19.03.14 08:59
 * description: 
 * ______________________________________________________________________________
 *
 * Copyright: (c) Weigend AM, all rights reserved
 * ______________________________________________________________________________
 */
package io.softwareecg.collector.source.logevents.types.interval;

import io.softwareecg.collector.source.logevents.EventConfigProperties;
import io.softwareecg.collector.source.logevents.types.BaseType;

import java.util.regex.Pattern;

/**
 */
public class IntervalType extends BaseType {

    /**
     * Every log interval has a pattern to match a start and an end
     */
    private final Pattern endPattern;

    /**
     * Constructs a interval type
     *
     * @param name - the name o the type
     */
    public IntervalType(final String name) {
        super(EventConfigProperties.getIntervalStartLogPattern(name),
                EventConfigProperties.getIntervalLogDatePattern(name),
                "TIME",
                "",
                getIntervalLogGroup(name), name);

        this.endPattern = Pattern.compile(EventConfigProperties.getIntervalEndLogPattern(name));
    }


    /**
     * @param name the event type
     * @return the metric group for the type
     */
    public static String getIntervalLogGroup(String name) {
        return String.valueOf(EventConfigProperties.PROPERTIES
                .getProperty(String.format("io.softwareecg.collector.storage.solr.types.intervals.%s.metricGroup", name), "Log-Intervals"));
    }

    /**
     * Creates a event from this type
     *
     * @param line - the log line
     * @return a log event
     */
    public LogIntervalEvent parseType(String line) {
        return new LogIntervalEvent(line, this);
    }

    /**
     * @param line the log line
     * @return true if the end pattern matches
     */
    @Override
    public boolean endMatches(String line) {
        return endPattern.matcher(line).matches();
    }

    /**
     * @return the end pattern
     */
    public Pattern getEndPattern() {
        return endPattern;
    }
}

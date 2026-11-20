/*
 * ______________________________________________________________________________
 *
 * Project: Software EKG
 * ______________________________________________________________________________
 *
 * created by: f.lautenschlager
 * creation date: 19.03.14 17:32
 * description: 
 * ______________________________________________________________________________
 *
 * Copyright: (c) Weigend AM, all rights reserved
 * ______________________________________________________________________________
 */
package io.softwareecg.collector.source.logevents.types;


import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.regex.Pattern;

/**
 */
public abstract class BaseEvent {

    /**
     * The event value key constants
     */
    public static final String EVENT_NAME_KEY = "NAME";
    public static final String EVENT_VALUE_KEY = "VALUE";
    public static final String EVENT_DATE_KEY = "TIME";


    /**
     * The event values
     */
    protected Date date;
    protected String name;
    protected Double value;
    protected String group;


    /**
     * Every log event has a pattern to match on a log line
     */
    protected final Pattern logPattern;
    /**
     * The pattern to parse the timestamp from the log
     * Is set in the collector properties. The default is
     * <code>io.softwareecg.collector.storage.solr.LogDatePattern=dd-mm-YYYY hh:MM:sss,SSS</code>
     */
    protected final DateTimeFormatter dateFormat;

    /**
     * The value format to determine if we deal with numeric data or in date format
     */
    protected final String valueFormat;
    protected final ValueTypes valueType;

    /**
     * Constructs a base event on the given base type
     *
     * @param baseType - the base type
     */
    public BaseEvent(BaseType baseType) {
        this.name = baseType.getName();
        this.group = baseType.getGroup();
        this.logPattern = baseType.getStartPattern();
        this.dateFormat = baseType.getDateFormat();
        this.valueFormat = baseType.getValueFormat();
        this.valueType = baseType.getValueType();
    }

    /**
     * @return the group of this event
     */
    public String getGroup() {
        return group;
    }

    /**
     * @return the type name of this event
     */
    public String getName() {
        return name;
    }

    /**
     * @return the date of this event
     */
    public Date getDate() {
        return new Date(date.getTime());
    }

    /**
     * @return the value of this event
     */
    public Double getValue() {
        return value;
    }

    /**
     * Returns true if the given line matches the ending condition
     *
     * @param line - the log line
     * @return true if matches, otherwise false
     */
    public abstract boolean endMatches(String line);

    /**
     * @return returns true if the event is finished, otherwise false
     */
    public abstract boolean finished();

    /**
     * Sets the values from the detected event end
     *
     * @param line - the log line
     */
    public abstract void parseEndLine(String line);


    @Override
    public String toString() {
        return "BaseEvent{" + "date=" + date +
                ", name='" + name + '\'' +
                ", value=" + value +
                ", group='" + group + '\'' +
                ", logPattern=" + logPattern +
                ", dateFormat=" + dateFormat +
                ", valueFormat='" + valueFormat + '\'' +
                ", valueType=" + valueType +
                '}';
    }
}

/*
 * ______________________________________________________________________________
 *
 * Project: Software EKG
 * ______________________________________________________________________________
 *
 * created by: f.lautenschlager
 * creation date: 20.03.14 08:34
 * description: 
 * ______________________________________________________________________________
 *
 * Copyright: (c) Weigend AM, all rights reserved
 * ______________________________________________________________________________
 */
package io.softwareecg.collector.source.logevents.types;

import org.apache.commons.lang3.StringUtils;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.regex.Pattern;

/**
 */
public abstract class BaseType {

    protected final Pattern startPattern;

    /**
     * The pattern to parse the timestamp from the log
     * Is set in the collector properties. The default is
     * <code>io.softwareecg.collector.storage.solr.LogDatePattern=dd-MM-yyyy HH:mm:sss,SSS</code>
     */
    protected final DateTimeFormatter dateFormat;
    /**
     * The value format to determine if we deal with numeric data or in date format
     */
    protected final String valueFormat;
    /**
     * The value type: NUMERIC, TIME or INCREMENT
     */
    protected final ValueTypes valueType;
    /**
     * The group of this event
     */
    protected final String group;

    /**
     * The log type name
     */
    protected final String name;

    /**
     * Constructs a base type
     *
     * @param startPattern - the start pattern for the type
     * @param datePattern  - the date pattern
     * @param valueType    - the type of the value (NUMERIC | TIME)
     * @param valueFormat  - the value format (DE,EN | TIME FORMAT)
     * @param group        - the metric group
     * @param name         - the base type name
     */
    public BaseType(String startPattern, String datePattern, String valueType, String valueFormat, String group, String name) {
        this.startPattern = Pattern.compile(startPattern);

        if (!StringUtils.isEmpty(datePattern)) {
            this.dateFormat = DateTimeFormatter.ofPattern(datePattern).withZone(ZoneId.of("UTC"));
        } else {
            this.dateFormat = null;
        }

        this.valueType = ValueTypes.valueOf(valueType);
        this.valueFormat = valueFormat;
        this.group = group;
        this.name = name;
    }


    /**
     * Creates a concrete event for the type T
     *
     * @param line the log line
     * @return an instance of the type T
     */
    public abstract BaseEvent parseType(String line);

    /**
     * @param line the log line
     * @return true if matches, otherwise false
     */
    public boolean startMatches(String line) {
        return startPattern.matcher(line).matches();
    }

    /**
     * Note: This method should be overridden by the children.
     * The default behaviour will always return false
     *
     * @param line the log line
     * @return true if matches, otherwise false
     */
    public boolean endMatches(String line) {
        return false;
    }

    /**
     * @return the start pattern
     */
    public Pattern getStartPattern() {
        return startPattern;
    }

    /**
     * @return the date time formatter
     */
    public DateTimeFormatter getDateFormat() {
        return dateFormat;
    }

    /**
     * @return the value format
     */
    public String getValueFormat() {
        return valueFormat;
    }

    /**
     * @return the value type
     */
    public ValueTypes getValueType() {
        return valueType;
    }

    /**
     * @return the metric group
     */
    public String getGroup() {
        return group;
    }

    /**
     * @return the type name
     */
    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return "BaseType{" + "startPattern=" + startPattern +
                ", dateFormat=" + dateFormat +
                ", valueFormat='" + valueFormat + '\'' +
                ", valueType=" + valueType +
                ", group='" + group + '\'' +
                ", name='" + name + '\'' +
                '}';
    }
}

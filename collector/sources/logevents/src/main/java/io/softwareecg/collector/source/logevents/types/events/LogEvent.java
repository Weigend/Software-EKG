/*
 * ______________________________________________________________________________
 * <p>
 * Project: Software EKG
 * ______________________________________________________________________________
 * <p>
 * created by: f.lautenschlager
 * creation date: 12.02.14 09:23
 * description: The log event class
 * ______________________________________________________________________________
 * <p>
 * Copyright: (c) Weigend AM, all rights reserved
 * ______________________________________________________________________________
 */
package io.softwareecg.collector.source.logevents.types.events;

import io.softwareecg.collector.source.logevents.types.BaseEvent;
import io.softwareecg.collector.source.logevents.types.BaseType;
import io.softwareecg.collector.source.logevents.types.ValueConverter;

import java.time.ZonedDateTime;
import java.time.format.DateTimeParseException;
import java.util.Date;
import java.util.regex.Matcher;

/**
 * This class
 * <p/>
 * Event Values:
 * (?<NAME>)         -> Event name represents the metric name
 * (?<TIME>)        -> Timestamp represents the x value
 * (?<VALUE>)        -> Value of the event representing the y value
 * Event Payload:
 * (?<xxPropertyxx>)      -> Stores the value in the event payload with key xxPropertyxx (xx.*xx)
 *
 */
public class LogEvent extends BaseEvent {


    /**
     * Constructs a log event from the given line. Sets the timestamp and initialize the event properties
     *
     * @param line     - the log line of whom we extract the event information.
     * @param baseType base type
     */
    public LogEvent(String line, BaseType baseType) {
        super(baseType);
        initializeEventFrom(line);
    }

    /**
     * Initializes the event from the given line. Is implemented from every
     * child class and i
     *
     * @param line - the log line of whom we extract the event information.
     */
    private void initializeEventFrom(String line) {
        Matcher matcher = logPattern.matcher(line);
        if (matcher.matches()) {
            setEventValues(matcher);
        } else {
            throw new IllegalArgumentException(String.format("The given line does not match the pattern. Line:{%s}\tPattern:{%s}", line, logPattern));
        }
    }

    /**
     * Sets the default event data (name, date, value).
     * If one of these are missing or something other unexpected happens an exception will be thrown.
     *
     * @param matcher - the matcher based on the log line
     * @throws IllegalArgumentException - if somethings goes wrong
     */
    private void setEventValues(Matcher matcher) {
        try {
            this.name = matcher.group(EVENT_NAME_KEY);
            this.date = parseDate(matcher.group(EVENT_DATE_KEY));
            this.value = new ValueConverter().parseValue(matcher.group(EVENT_VALUE_KEY), valueType, valueFormat);
        }
        catch (IllegalStateException | IllegalArgumentException e) {
            throw new IllegalArgumentException("Could not set the default event values due to an exception.", e);
        }
    }

    private Date parseDate(String dateValue) {
        if (dateFormat == null) {
            //no format -> raw data (millis)
            return new Date(Long.parseLong(dateValue));
        } else {
            try {
                return new Date(ZonedDateTime.parse(dateValue, dateFormat).toInstant().toEpochMilli());
            } catch (DateTimeParseException e) {
                throw new IllegalArgumentException(e);
            }
        }
    }


    @Override
    public boolean endMatches(String line) {
        return false;
    }

    @Override
    public boolean finished() {
        return true;
    }

    @Override
    public void parseEndLine(String line) {
        //Nothing to do
    }
}

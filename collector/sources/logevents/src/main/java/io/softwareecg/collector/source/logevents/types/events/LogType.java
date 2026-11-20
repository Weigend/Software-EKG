/*
 * ______________________________________________________________________________
 *
 * Project: Software EKG
 * ______________________________________________________________________________
 *
 * created by: f.lautenschlager
 * creation date: 07.03.14 12:26
 * description: The class log type, represents the configured properties
 * ______________________________________________________________________________
 *
 * Copyright: (c) Weigend AM, all rights reserved
 * ______________________________________________________________________________
 */
package io.softwareecg.collector.source.logevents.types.events;

import io.softwareecg.collector.source.logevents.EventConfigProperties;
import io.softwareecg.collector.source.logevents.types.BaseType;

/**
 */
public class LogType extends BaseType {


    /**
     * Constructs a log type
     *
     * @param name - the type name matching the event type in the properties
     */
    public LogType(String name) {
        super(EventConfigProperties.getEventLogPattern(name),
              EventConfigProperties.getLogDatePattern(name),
              EventConfigProperties.getEventLogValueType(name),
              EventConfigProperties.getEventLogValueFormat(name),
              EventConfigProperties.getEventLogGroup(name),
              name);
    }

    /**
     * Creates a event from this type
     *
     * @param line - the log line
     * @return a log event
     */
    public LogEvent parseType(String line) {
        return new LogEvent(line, this);
    }
}

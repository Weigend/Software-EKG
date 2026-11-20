/*
 * ______________________________________________________________________________
 *
 * Project: Software EKG
 * ______________________________________________________________________________
 *
 * created by: f.lautenschlager
 * creation date: 19.03.14 08:34
 * description: 
 * ______________________________________________________________________________
 *
 * Copyright: (c) Weigend AM, all rights reserved
 * ______________________________________________________________________________
 */
package io.softwareecg.collector.source.logevents.types.interval;

import io.softwareecg.collector.common.LoggerFactory;
import io.softwareecg.collector.source.logevents.types.BaseEvent;
import org.slf4j.Logger;

import java.time.ZonedDateTime;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 */
public class LogIntervalEvent extends BaseEvent {

    /**
     * An interval has an ending
     */
    private final Pattern intervalEndPattern;

    /**
     * The event values
     */
    private Date endDate;

    /**
     * Transaction ID for connecting the start/end events of a transaction
     */
    private String transactionId;

    public static final String EVENT_TRANSACTION_ID_KEY = "TRANSACTION";

    private static final Logger LOGGER = LoggerFactory.create();

    /**
     * Flag to mark if a start is detected
     */
    private boolean startDetected = false;
    /**
     * Flag ot mark a interval as closed
     */
    private boolean closed = false;

    /**
     * Constructs a log interval event
     *
     * @param line     - the log line
     * @param baseType - the base interval type
     */
    public LogIntervalEvent(String line, IntervalType baseType) {
        super(baseType);

        this.intervalEndPattern = baseType.getEndPattern();
        initializeEventFrom(line);
    }

    private void initializeEventFrom(String line) {
        LOGGER.debug("event: start");
        Matcher matcher = logPattern.matcher(line);
        if (matcher.matches()) {
            LOGGER.debug("event: start matches");
            startDetected = true;
            setIntervalStartValues(matcher);
        } else {
            throw new IllegalArgumentException(String.format("The given line does not match the pattern. Line:{%s}\tPattern:{%s}", line, logPattern));
        }
    }

    /**
     * Sets the values from the interval start
     *
     * @param matcher - the matcher for the log line
     */
    private void setIntervalStartValues(Matcher matcher) {
        try {
            this.name = matcher.group(EVENT_NAME_KEY);
            this.date = parseDate(matcher.group(EVENT_DATE_KEY));
            if (hasTransactionIdGroup(matcher)) {
                this.transactionId = matcher.group(EVENT_TRANSACTION_ID_KEY);
            }
        } catch (IllegalStateException | IllegalArgumentException e) {
            throw new IllegalArgumentException("Could not set the interval start values due to an exception.", e);
        }
    }

    private boolean hasTransactionIdGroup(Matcher matcher) {
        return matcher.toString().contains("<" + EVENT_TRANSACTION_ID_KEY + ">");
    }

    private Date parseDate(String group) {
        return new Date(ZonedDateTime.parse(group, dateFormat).toInstant().toEpochMilli());
    }

    @Override
    public boolean endMatches(String line) {
        Matcher matcher = intervalEndPattern.matcher(line);

        if (transactionId != null && matcher.matches()) {
            LOGGER.debug("checking for same transaction - start was: " + transactionId);
            try {
                String endTransId = matcher.group(EVENT_TRANSACTION_ID_KEY);
                boolean sameTransaction = endTransId.equalsIgnoreCase(transactionId);
                LOGGER.debug("end transaction " + endTransId + " does match: " + sameTransaction);
                return sameTransaction;
            } catch (Exception e) {
                LOGGER.warn("exception during parsing of transaction ID - matcher " + matcher);
                LOGGER.warn("exception during parsing of transaction ID - line " + line);
                return false;
            }
        } else {
            return matcher.matches();
        }

    }

    @Override
    public boolean finished() {
        return closed;
    }

    /**
     * Parses the end from the given line
     *
     * @param line - the log line
     */
    public void parseEndLine(String line) {
        LOGGER.debug("event: end");
        if (startDetected && endMatches(line)) {
            setIntervalEndValues(line);
            closed = true;
            LOGGER.debug("########### DEBUG interval complete and closed: " + this);
            LOGGER.debug("########### DEBUG line: " + line);
        } else {
            throw new IllegalArgumentException(String.format("For this interval '%s' was no start set", this));
        }
    }


    private void setIntervalEndValues(String line) {
        try {
            Matcher matcher = intervalEndPattern.matcher(line);
            if (!matcher.matches()) {
                throw new IllegalStateException("can not apply end values because line does not match!");
            }
            this.endDate = parseDate(matcher.group(EVENT_DATE_KEY));
            this.value = (double) endDate.getTime() - date.getTime();
        } catch (IllegalStateException | IllegalArgumentException e) {
            throw new IllegalArgumentException("Could not set the default event values due to an exception.", e);
        }
    }

    /**
     * @return true if start detected, otherwise false
     */
    public boolean startDetected() {
        return startDetected;
    }

    /**
     * @return the end date of this interval
     */
    public Date getEndDate() {
        return new Date(endDate.getTime());
    }


    @Override
    public String toString() {
        return "LogIntervalEvent{" + "intervalEndPattern=" + intervalEndPattern +
                ", date=" + getDate() +
                ", endDate=" + endDate +
                ", startDetected=" + startDetected +
                ", closed=" + closed +
                '}';
    }
}

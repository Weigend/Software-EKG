/**
 * ______________________________________________________________________________
 *
 * Project: Software EKG
 * ______________________________________________________________________________
 *
 * created by: f.lautenschlager
 * creation date: 05.02.14 16:42
 * description: 
 * ______________________________________________________________________________
 *
 * Copyright: (c) Weigend AM, all rights reserved
 * ______________________________________________________________________________
 */
package io.softwareecg.collector.source.logevents;

import io.softwareecg.collector.source.logevents.types.BaseEvent;
import io.softwareecg.collector.source.logevents.types.BaseType;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.LineIterator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Reader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Pattern;

/**
 */
public class EventIterator implements Iterator<String> {

    private static final Logger LOGGER = LoggerFactory.getLogger(EventIterator.class);


    /**
     * Pattern for detecting the start of a line
     */
    private final Pattern startPattern;

    /**
     * Our internal entry to aggregate lines into one entry
     */
    private final StringBuilder entry = new StringBuilder();

    /**
     * The internal line iterator
     */
    private LineIterator lineIterator;

    /**
     * Flag to indicate if the multiline feature should be used
     */
    private final boolean multiline;
    /**
     * The started events
     */
    private final List<BaseEvent> startedEvents = new ArrayList<>();

    /**
     * The multi-line iterator returns for every nextLine() a bunch of lines between
     * two starts. The starts are detected with the given startPattern. This is usefully
     * if you have logs with entries over more lines, like stacktraces.
     *
     * @param startPattern -  pattern to detect the start of multi line
     * @param multiline
     * @throws IllegalArgumentException
     */
    public EventIterator(Pattern startPattern, boolean multiline) {
        this.startPattern = startPattern;
        this.multiline = multiline;
    }

    /**
     * Returns the next event.
     *
     * @param registeredEventTypes
     * @return the extracted events
     */
    public List<BaseEvent> nextEvents(List<BaseType> registeredEventTypes) {
        return extractEvents(next(), registeredEventTypes);

    }

    /**
     * Flushes the remaining entry as string
     *
     * @return the remaining entry
     */
    public String flush() {
        return getCurrentEntryAndReset();
    }

    /**
     * Flushes remaining entry and tries to extract events
     *
     * @param registeredEventTypes - the event types
     * @return a list with events
     */
    public List<BaseEvent> flush(List<BaseType> registeredEventTypes) {
        return extractEvents(getCurrentEntryAndReset(), registeredEventTypes);
    }

    /**
     * Extracts concrete events for the given types on the current line
     *
     * @param line                 - the current log entry
     * @param registeredEventTypes - the event types
     * @return a list with concrete base event from the given types
     */
    private List<BaseEvent> extractEvents(String line, List<BaseType> registeredEventTypes) {
        LOGGER.debug("Extracting events from line" + line);
        List<BaseEvent> finishedEvents = new ArrayList<>();

        for (BaseType type : registeredEventTypes) {
            LOGGER.debug("checking type" + type.getName());

            //First check if the line matches an end of a type definition matches
            if (type.endMatches(line)) {
                LOGGER.debug("matched end - interval");
                //Check if an opened interval has a matching end pattern for the current line
                //If true, add it to the events
                checkStartedEventsAndAddFinished(line, finishedEvents);
            }

            //If a start matches we add it to the st
            if (type.startMatches(line)) {
                LOGGER.debug("matched start - event or interval");
                parseEventAndAddFinished(line, finishedEvents, type);

            }
            //no return, so we can handle multiple events per line
        }
        startedEvents.removeAll(finishedEvents);
        return finishedEvents;
    }

    /**
     * Checks the started events, if the given line matches the an end
     *
     * @param line           - the current log entry
     * @param finishedEvents - a list with finished events
     */
    private void checkStartedEventsAndAddFinished(String line, List<BaseEvent> finishedEvents) {
        for (BaseEvent startedType : startedEvents) {
            LOGGER.debug("Checking if a started event detect the current line as closing condition.");
            if (startedType.endMatches(line)) {
                startedType.parseEndLine(line);
                finishedEvents.add(startedType);
            }
        }
    }

    /**
     * Parses an event from the given line
     *
     * @param line           - the current log entry
     * @param finishedEvents - a list with finished events
     * @param type           - the base type
     */
    private void parseEventAndAddFinished(String line, List<BaseEvent> finishedEvents, BaseType type) {
        LOGGER.debug("The current type " + type + " matches on the given line");
        BaseEvent event = type.parseType(line);
        //if the event is finished
        if (event.finished()) {
            LOGGER.debug("finished - event");
            finishedEvents.add(event);
        } else {
            LOGGER.debug("not finished yet - interval");
            startedEvents.add(event);
        }
    }

    /**
     * @return true if the iterator points to more lines
     */
    @Override
    public boolean hasNext() {
        if (lineIterator == null) {
            throw new IllegalStateException("Reader is not set while calling method hasNext().");
        }
        return lineIterator.hasNext();
    }


    /**
     * Did nothing
     */
    @Override
    public void remove() {
        //Nothing to do.
    }

    /**
     * @return the next line, if singe line mode or the next log entry if multiline mode
     */
    @Override
    public String next() {
        if (lineIterator == null) {
            throw new IllegalStateException("Reader is not set while calling method next().");
        }

        if (multiline) {
            return readMultiLine();
        } else {
            return readSingleLine();
        }
    }

    private String readMultiLine() {
        while (lineIterator.hasNext()) {
            String line = lineIterator.nextLine();
            if (startDetectedAndEntryIsNotEmpty(line)) {
                String logEntry = getCurrentEntryAndReset();
                append(line);
                return logEntry;
            } else {
                append(line);
            }
        }
        return "";
    }

    private String readSingleLine() {
        if (lineIterator.hasNext()) {
            return lineIterator.nextLine();
        }
        return "";
    }

    private boolean startDetectedAndEntryIsNotEmpty(String line) {
        return startDetected(line) && entry.length() > 0;
    }

    private boolean startDetected(String line) {
        return startPattern.matcher(line).matches();
    }

    private String getCurrentEntryAndReset() {
        int length = entry.length();
        if (length == 0) {
            return "";
        }
        //Remove the line separator added by append(line) method
        String logLine = entry.substring(0, length - 1);
        entry.delete(0, length);
        return logLine;
    }

    private void append(String line) {
        this.entry.append(line).append(IOUtils.LINE_SEPARATOR);
    }

    /**
     * @param reader sets a new reader
     */
    public void setReader(Reader reader) {
        LOGGER.debug("Set new reader to multi log line iterator");
        lineIterator = new LineIterator(reader);
    }


}

//______________________________________________________________________________
//
//          Project:    Software EKG
//______________________________________________________________________________
//
//         Author:      Weigend AM 2009 - 2014
//______________________________________________________________________________
//
// Notice: This piece of software was created, designed and implemented by
// experienced craftsmen and innovators in Munich, Germany.
// Changes should be done with respect to the original design.
//______________________________________________________________________________
package io.softwareecg.awb.importer.csv.fileutil;

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.LineIterator;

import java.io.Reader;
import java.util.regex.Pattern;

/**
 */
public class MultiLineIterator extends LineIterator {

    /**
     * Pattern for detecting the start of a line
     */
    private final Pattern startPattern;
    /**
     * Our internal entry to aggregate lines into one entry
     */
    private final StringBuilder entry = new StringBuilder();

    /**
     * The multi-line iterator returns for every nextLine() a bunch of lines between
     * two starts. The starts are detected with the given startPattern. This is usefully
     * if you have logs with entries over more lines, like stacktraces.
     *
     * @param reader       - a reader that points to the source
     * @param startPattern -  pattern to detect the start of a line
     */
    public MultiLineIterator(Reader reader, Pattern startPattern) {
        super(reader);
        this.startPattern = startPattern;
    }

    /**
     * Iterates over all lines from super and adds
     * them to the string builder.
     *
     * @return next line string
     */
    @Override
    public String nextLine() {

        while (super.hasNext()) {
            String line = super.nextLine();

            if (startDetectedAndEntryIsNotEmpty(line)) {
                String logEntry = getCurrentEntryAndReset();
                append(line);
                return logEntry;
            } else {
                append(line);
            }
        }
        return getCurrentEntryAndReset();
    }

    private boolean startDetectedAndEntryIsNotEmpty(String line) {
        boolean startDetected = startDetected(line);
        return startDetected && entry.length() > 0;
    }

    private boolean startDetected(String line) {
        return startPattern.matcher(line).matches();
    }

    private String getCurrentEntryAndReset() {
        int length = entry.length();
        String logLine = entry.substring(0, length - 1); //Remove the line separator added by append method
        entry.delete(0, length);
        return logLine;
    }

    private void append(String line) {
        this.entry.append(line).append(IOUtils.LINE_SEPARATOR);
    }
}

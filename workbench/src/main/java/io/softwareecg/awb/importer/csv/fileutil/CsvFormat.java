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

import io.softwareecg.awb.sdk.core.log.EkgLogger;
import org.slf4j.Logger;

import java.io.File;
import java.text.*;
import java.time.Instant;
import java.util.*;

/**
 * An enumeration that describes the different format options of csv files
 * that are supported by the Software-EKG AWB.
 *
 * The format options are a composition of:
 * - the datetime format that is used to specify than the rows (measured values) are written
 * - the number format of the values itself
 * - the delimiter of the csv columns that is in use
 * - a boolean flag that defines if metadata are serialized into the csv headers or not
 *
 * The enum key itself add the additional information which kind of header version is in use
 * that is important for csv file of EKG Collector v6.1 and newer that can use different JSON
 * structure to describe the metadata in the csv header.
 *
 * The enum not only describe the format. It also can detect it automatically from given files.
 *
 * @since 16.09.2014
 */
public enum CsvFormat {

    /**
     * The Next-Generation EKG format that is used since EKG Collector v6.1 with cloud support and
     * serialized the metric metadata (labels) into the header field of the csv.
     * This format uses timestamps instead of readable data time strings that can converted in Exel using
     * the formula "=(((B60/60)/60)/24)+DATUM(1970;1;1)" + custom cell format "TT.MM.JJJJ hh:mm:ss,0"
     */
    EKG_61_FORMAT(";", null, DecimalFormat.getInstance(Locale.US), true),

    /**
     * New Instant Format. Attention: There must not be a 'Z' at the end if SimpleDateFormat is used for parsing!
     */
    INSTANT_FORMAT(";", "yyyy-MM-dd'T'HH:mm:ss.SSS", null, false),

    /**
     * ekg: Delimiter: , EN-Decimalformat with . and , no ""
     */
    DEFAULT_EKG_FORMAT(";", "dd.MM.yyyy HH:mm:ss.SSS", DecimalFormat.getInstance(Locale.US), false),

    /**
     * ekg: Old format w/o ms. Delimiter: , EN-Decimalformat with . and , no ""
     */
    EKG_OLD_FORMAT(";", "dd.MM.yyyy HH:mm:ss", DecimalFormat.getInstance(Locale.US), false),

    /**
     * Microsoft: Delimiter: ; US-Decimalformat with ,.
     */
    MICROSOFT_PERFORMANCE_DATA_FORMAT(";\"", "MM/dd/yyyy HH:mm:ss.SSS", null, false);

    /**
     * a logger instance to write out debug messages which can help to
     * understand which format is recognized.
     */
    private static final Logger LOGGER = EkgLogger.get();

    /**
     * the delimiter that separates the csv columns
     */
    private final String delimiter;

    /**
     * the datetime format as {@link DateFormat} instance that is used to
     * specify than the rows (measured values) are written
     */
    private final DateFormat dateFormat;

    /**
     * the number format of the measured values itself
     */
    private NumberFormat numberFormat;

    /**
     * the datetime format as String that is used to
     * specify than the rows (measured values) are written
     */
    private final String dateFormatString;

    /**
     * a boolean flag which is true than the csv use serialized
     * JSON structures for metadata
     */
    private final boolean useJsonMetaData;

    /**
     * Constructs a new instance of CsvFormat enum that will use
     * the specified parameters to initialize the final enum properties
     * an construct time.
     *
     * @param delimiter the delimiter that separates the csv columns
     * @param dateFormatString the date format that is used to specify the time the values are measured
     * @param numberFormat the number format that is used for the values
     * @param useJsonMetaData a boolean flag which is true than the csv use serialized JSON structures for metadata
     */
    CsvFormat(String delimiter, String dateFormatString, NumberFormat numberFormat, boolean useJsonMetaData) {
        this.delimiter = delimiter;
        this.dateFormatString = dateFormatString;
        if (dateFormatString != null) {
            // Use Locale.US to ensure consistent date parsing regardless of system locale
            this.dateFormat = new SimpleDateFormat(dateFormatString, Locale.US);
            this.dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        } else {
            this.dateFormat = null;
        }

        this.numberFormat = numberFormat;
        this.useJsonMetaData = useJsonMetaData;
    }

    /**
     * Autodetect the correct format that is used in the
     * file that is referenced by the 'file' parameter.
     *
     * @param file a File instance that should point to a valid EKG Collector csv
     * @return the corresponding CsvFormat or null if none of the CsvFormat values matches
     */
    public static CsvFormat autodetect(File file) {

        String firstLine = CsvFileHelper.getFirstLine(file);

        if (firstLine == null) {
            LOGGER.warn("Cannot read first line of file: {}", file.getName());
            return null; // error
        }

        if (firstLine.startsWith("{")) {
            LOGGER.debug("Detected EKG_61_FORMAT (JSON header) for file: {}", file.getName());
            return EKG_61_FORMAT;
        }

        if (firstLine.startsWith("\"(PDH-CSV")) {
            MICROSOFT_PERFORMANCE_DATA_FORMAT.numberFormat = DecimalFormat.getInstance(Locale.US);
            return MICROSOFT_PERFORMANCE_DATA_FORMAT;
        }

        String lastLine = CsvFileHelper.getLastLine(file);

        // Handle empty or unreadable files
        if (lastLine == null || lastLine.isEmpty()) {
            LOGGER.warn("Cannot read last line of file or file is empty: {}", file.getName());
            return null;
        }

        if (count(lastLine, ';') == 0) {
            // probably , separated
            lastLine = lastLine.replaceAll(",", ";");
        }

        lastLine = lastLine.replaceAll("\"", ""); // remove ""
        int idx = lastLine.indexOf(';');
        if (idx == -1) {
            LOGGER.warn("No delimiter (;) found in last line of file: {}. Last line: '{}'", file.getName(), lastLine);
            return null;
        }

        String dateString = lastLine.substring(0, idx).trim();
        LOGGER.debug("Trying to autodetect CSV format. First column value: '{}', file: {}", dateString, file.getName());
        
        for (CsvFormat format : CsvFormat.values()) {
            try {
                Instant date = format.parse(dateString);
                if (date == null) {
                    LOGGER.trace("Format {} did not match for value '{}'", format.name(), dateString);
                    continue;
                }

                format.numberFormat = resolveCsvNumberFormat(lastLine, file.getName(), format);
                LOGGER.debug("Detected CSV format: {} for file: {}", format.name(), file.getName());

                return format;
            } catch (Exception e) {
                LOGGER.trace("Format {} threw exception for value '{}': {}", format.name(), dateString, e.getMessage());
            }
        }

        LOGGER.warn("Could not detect CSV format for file: {}. First column value was: '{}'. Using DEFAULT_EKG_FORMAT.", 
            file.getName(), dateString);
        return DEFAULT_EKG_FORMAT; // not found
    }

    /**
     * Returns the number of milliseconds since January 1, 1970, 00:00:00 UTC
     * represented by this <tt>Date</tt> object.
     *
     * @param dateTime the datetime string to parse (may already an long as string if EKG NG format ist used)
     * @return the number of milliseconds since January 1, 1970, 00:00:00 GMT represented by the parsed datetime string.
     * @throws ParseException thrown if the dateTime doesn't match to the instantiated date-format
     */
    public long parseDateToTimestamp(String dateTime) throws ParseException {
        if (dateFormat == null) {
            return Long.parseLong(dateTime);
        }

        return getDateFormat().parse(dateTime).getTime();
    }

    /**
     * Returns a boolean flag which is true than the csv use serialized
     * JSON structures for metadata
     *
     * @return true if the csv use an JSON data structure in the first header column, otherwise false
     */
    public boolean useJsonMetaData() {
        return useJsonMetaData;
    }

    /**
     * The current date format string.
     *
     * @return the date format as string.
     */
    public String getDateFormatString() {
        return dateFormatString;
    }

    /**
     * Getter for property 'delimiter'.
     *
     * @return Value for property 'delimiter'.
     */
    public String getDelimiter() {
        return delimiter;
    }

    /**
     * Getter for property 'numberFormat'.
     *
     * @return Value for property 'numberFormat'.
     */
    public NumberFormat getNumberFormat() {
        return (NumberFormat) numberFormat.clone();
    }

    //=================================================================================================================
    // internal helper logic
    //=================================================================================================================

    private static NumberFormat resolveCsvNumberFormat(String lastLine, String fileName, CsvFormat format) {
        // check if german or en number format
        // heuristic - does not work if all colums are -1
        String[] splitLine = format.csvSplit(lastLine);
        for (int i = 1; i < splitLine.length; i++) {
            String s = splitLine[i];
            if (count(s, ',') > 1) {
                return DecimalFormat.getInstance(Locale.US);
            }
            if (count(s, '.') > 1) {
                return DecimalFormat.getInstance(Locale.GERMAN);
            }
            if (s.contains(".") && s.contains(",")) {
                if (s.indexOf('.') < s.indexOf(',')) {
                    return DecimalFormat.getInstance(Locale.GERMAN);
                } else {
                    return DecimalFormat.getInstance(Locale.US);
                }
            }
        }

        LOGGER.warn("Can not detect format of file: '" + fileName + "' file. Default is used.");
        return DecimalFormat.getInstance(Locale.US);
    }

    /**
     * Split the String.
     *
     * @param inputLine a csv line.
     * @return an array of substrings (without delimiter).
     */
    private String[] csvSplit(String inputLine) {
        String line = inputLine;

        if (this == MICROSOFT_PERFORMANCE_DATA_FORMAT) {
            line = line.replaceAll("\",", ";");
            line = line.replaceAll("\"", "");
        }

        List<String> result = new ArrayList<>();
        StringTokenizer tok = new StringTokenizer(line, delimiter);
        while (tok.hasMoreTokens()) {
            String token = tok.nextToken().trim();
//            if (this == MICROSOFT_PERFORMANCE_DATA_FORMAT) {
//                if (token.length() > 1 && token.charAt(0) == '\"' && token.charAt(token.length() - 1) == '\"') {
//                    token = token.substring(1, token.length() - 2);
//                }
//            }
            result.add(token);
        }

        String[] resultArray = new String[result.size()];
        return result.toArray(resultArray);
    }

    /**
     * Count a char. Used by autodetect.
     *
     * @param lastLine the lien.
     * @param c        a char.
     * @return the number of c chars in the string.
     */
    private static int count(String lastLine, char c) {
        int count = 0;
        for (int i = 0; i < lastLine.length(); i++) {
            if (lastLine.charAt(i) == c) {
                count++;
            }
        }
        return count;
    }

    /**
     * Getter for property 'dateFormat'.
     *
     * @return Value for property 'dateFormat'.
     */
    private DateFormat getDateFormat() {
        return (DateFormat) dateFormat.clone();
    }

    /**
     * Parses a date.
     *
     * @param dateString the date string..
     * @return a date;
     */
    private Instant parse(String dateString) {
        try {
            if (dateFormat == null) {
                return dateString.matches("[0-9]+") ? Instant.now() : null;
            }

            return dateFormat.parse(dateString).toInstant();
        } catch (Exception e) {
            return null;
        }
    }
}
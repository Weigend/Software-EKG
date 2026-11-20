/**
 * ______________________________________________________________________________
 *
 * Project: Software EKG
 * ______________________________________________________________________________
 *
 * created by: f.lautenschlager
 * creation date: 19.03.14 08:36
 * description: 
 * ______________________________________________________________________________
 *
 * Copyright: (c) Weigend AM, all rights reserved
 * ______________________________________________________________________________
 */
package io.softwareecg.collector.source.logevents.types;

import java.text.NumberFormat;
import java.text.ParseException;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 */
public class ValueConverter {

    /**
     * Parses the value for the given
     *
     * @param value       - the string with the value
     * @param valueType   - the type of the given value (TIME or NUMERIC)
     * @param valueFormat - the value format. If date, then we expect a date format. If numeric, then we expect a language tag like 'de'
     * @return the value as double
     */
    public Double parseValue(String value, ValueTypes valueType, String valueFormat) {
        try {
            switch (valueType) {
                case NUMERIC:
                    return parseDouble(value, valueFormat);
                case TIME:
                    return parseTimeToDouble(value, valueFormat);
                default:
                    //Should never happen
                    return -1D;
            }
        } catch (ParseException | IllegalArgumentException e) {
            throw new IllegalArgumentException("Could not determine the value format. Its nor NUMERIC or TIME: " + valueFormat, e);
        }
    }

    /**
     * Parses the value into a double. The value is represented in a numeric format like 82,3
     *
     * @param value       - the string with the value
     * @param valueFormat - the value format. We expect a language tag like 'de'
     * @return the value as double
     * @throws ParseException if something goes wrong
     */
    private Double parseDouble(String value, String valueFormat) throws ParseException {
        Locale formatLocal = Locale.forLanguageTag(valueFormat);
        NumberFormat numberFormat = NumberFormat.getNumberInstance(formatLocal);
        return numberFormat.parse(value).doubleValue();
    }

    /**
     * Parses the time value into a double that represents the parsed time in milliseconds.
     * The input value is represented in a time format 'hh:mm:ss'
     *
     * @param value       - the string with the value
     * @param valueFormat - the value format. We expect a date format string like 'hh:mm:ss'
     * @return the value as double
     */
    private Double parseTimeToDouble(String value, String valueFormat) {

        return (double) LocalTime.parse(value, DateTimeFormatter.ofPattern(valueFormat)).toNanoOfDay() / 1_000_000;
    }


}

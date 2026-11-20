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
package io.softwareecg.collector.source.unix.analyzer;

import org.apache.commons.lang3.StringUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Created by martin.binder on 29.04.14.
 */
public final class AnalyzerStringUtils {

    private AnalyzerStringUtils() {
        //empty
    }

    /**
     * Helper to extract all lines;.
     *
     * @param result a string with multiple lines.
     * @return a list of lines.
     */
    public static String[] extractLines(String result) {
        BufferedReader r = new BufferedReader(new StringReader(result));
        List<String> lines = new ArrayList<>();
        String line;
        try {
            while ((line = r.readLine()) != null) {
                lines.add(line);
            }
        } catch (IOException e) {
            throw new IllegalStateException("This should never happen", e);
        }
        return lines.toArray(new String[lines.size()]);
    }


    /**
     * Retrieve a sub-array from given source.
     *
     * @param pSource           Text source
     * @param pLookupBeginToken Token which contains the header (this line would NOT be copied)
     * @param pBlockName        Block name for logging.
     * @return sub-array of the given beginToken.
     */
    public static String[] getMatchingSubArray(String[] pSource, String pLookupBeginToken, String pBlockName) {
        int startLineIndex = searchLineIndex(pSource, pLookupBeginToken, 0);

        if (startLineIndex == -1) {
            throw new IllegalStateException("unable to find the " + pBlockName + " block in terminal output");
        }

        //skip the header
        startLineIndex++;

        int endLineIndex = searchLineIndex(pSource, null, startLineIndex);

        if (endLineIndex < 0) {
            //set to the last index of source
            endLineIndex = pSource.length - 1;
        } else {
            //endLineIndex points to a empty line -> set to the last value line
            endLineIndex--;
        }


        //calculate the correct amount of lines to be copied, add one for the first line (e.g. source[3] till source[3] is one line!)
        int linesToCopy = endLineIndex - startLineIndex + 1;

        String[] subArray = new String[linesToCopy];
        System.arraycopy(pSource, startLineIndex, subArray, 0, linesToCopy);
        return subArray;
    }


    /**
     * Search a given source for the appearance of a token. Search starts at given offset.
     *
     * @param source           The text source to be searched ("the haystack").
     * @param lookupBeginToken The token ("the needle")
     * @param offset           Start offset (line index)
     * @return the first line-index of the found token beginning from offset, -1 if not found.
     */
    public static int searchLineIndex(String[] source, String lookupBeginToken, int offset) {
        int searchedLineIndex = -1;

        if (StringUtils.isBlank(lookupBeginToken)) {
            for (int index = offset; index < source.length; index++) {
                if (StringUtils.isBlank(source[index])) {
                    searchedLineIndex = index;
                    break;
                }
            }
        } else {
            for (int index = offset; index < source.length; index++) {
                if (source[index].startsWith(lookupBeginToken)) {
                    searchedLineIndex = index;
                    break;
                }
            }
        }

        return searchedLineIndex;
    }


    /**
     * Convert to double.
     *
     * @param value a number, date, or name.
     * @return a double.
     */
    public static double convertToDouble(String value) {
        if (value == null || value.isEmpty()) {
            return -1;  // something was wrong by the calling function
        } else if (value.endsWith("K") || value.endsWith("k")) {
            return Double.parseDouble(value.substring(0, value.length() - 1)) * 1024;
        } else if (value.endsWith("M") || value.endsWith("m")) {
            return Double.parseDouble(value.substring(0, value.length() - 1)) * 1024 * 1024;
        } else if (value.endsWith("%")) {
            return Double.parseDouble(value.substring(0, value.length() - 1)) / 100.0;
        } else if (value.contains(":")) {
            int pos = value.indexOf(':');
            // the time in the format 2:42.750 will converted to seconds and milliseconds. In the example case to 162.750
            return 60 * Double.parseDouble(value.substring(0, pos)) + Double.parseDouble(value.substring(pos + 1));
        } else {
            if (value.matches("[0-9]*[\\.]?[0-9]*")) {
                return Double.valueOf(value);
            } else {
                //not a number
                return value.hashCode(); // simple trick for unique ids
            }
        }
    }

    /**
     * Removes empty splits.
     *
     * @param splits1 splits.
     * @return a array where all indexes are valid.
     */
    public static String[] removeEmptySplits(String[] splits1) {
        List<String> result = new ArrayList<>();
        for (String s : splits1) {
            if (!StringUtils.isEmpty(s)) {
                result.add(s);
            }
        }
        return result.toArray(new String[result.size()]);
    }

    /**
     * Calculate a fingerprint/hashcode  for the given String collection.
     *
     * @param valueCollection a string collection
     * @return the hashCode for the string collection dependig on it's values
     */
    public static long getFingerPrint(Collection<String> valueCollection) {
        long hash = 0;

        for (String value : valueCollection) {
            hash += value.hashCode();
        }

        return hash;
    }

    /**
     * Parses the unix uptime string and converts it into minutes
     * Examples are for uptime strings are:
     * - 23:47,
     * - 123 days, 20:53,
     * - 301 days,  1:35,
     * - 1 day, 54 min,
     * - 385 days, 14:00,
     *
     * @param uptimeResult the uptime string
     * @return the uptime in minutes
     */
    public static double parseUptimeToMinutes(String uptimeResult) {
        String[] timeParts = uptimeResult.trim().split(",");
        long minutes = 0;

        for (String timePart1 : timeParts) {

            String timePart = timePart1.trim();

            if (StringUtils.isNotEmpty(timePart)) {

                //hour:min representation
                if (timePart.contains(":")) {
                    String[] hourMint = timePart.split(":");
                    minutes += Double.valueOf(hourMint[0]) * 60 + Double.valueOf(hourMint[1]);
                }

                //value unit pair like 123 days, 20:53
                if (timePart.contains(" ")) {
                    String[] valueUnit = timePart.split("\\s");
                    double factor = unitToMinFactor(valueUnit[1]);
                    minutes += Double.valueOf(valueUnit[0]) * factor;
                }
            }
        }

        return minutes;
    }

    private static double unitToMinFactor(String timeUnit) {
        String shortUnit = timeUnit.substring(0, 1).toLowerCase();
        switch (shortUnit) {
            case "m":
                return 1;
            case "h":
                return 60;
            case "d":
                return 1440;
        }
        return 1;
    }


}

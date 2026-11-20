/*
 * ______________________________________________________________________________
 *
 * Project: Software EKG
 * ______________________________________________________________________________
 *
 * created by: f.lautenschlager
 * creation date: 27.08.13-08:59
 * description: CSVUtil class
 * ______________________________________________________________________________
 *
 * Copyright: (c) Weigend AM, all rights reserved
 * ______________________________________________________________________________
 */
package io.softwareecg.collector.repository.common;

import org.apache.commons.lang3.StringUtils;

/**
 * Simple class for escaping CSV column names.
 */
public final class CSVUtil {

    /**
     * Separator char for our csv files
     */
    public static final String CSV_SEPARATOR = ";";

    /**
     * Replacement char for our csv separator char.
     */
    public static final String CSV_SEPARATOR_REPLACEMENT = "/";


    /**
     * Utility class.
     */
    private CSVUtil() {

    }

    /**
     * Returns a csv valid counter name.
     * Checks if the counter name contains ";" and replaces with "/".
     *
     * @param csvColumnName - Name of the column.
     * @return CSV-valid counter name or empty / null if the counterName is empty / null.
     */
    public static String escapeCsvColumnName(String csvColumnName) {

        if (StringUtils.isEmpty(csvColumnName)) {
            return csvColumnName;
        }

        String result = csvColumnName;
        if (csvColumnName.contains(CSV_SEPARATOR)) {
            result = csvColumnName.replaceAll(CSV_SEPARATOR, CSV_SEPARATOR_REPLACEMENT);
        }

        return result;

    }

    /**
     * Escapes names of host, process, groups to not contain any special character from the real name.
     *
     * @param name a name.
     * @return a string where all non letter or digit chars are replaces with '-'.
     */
    public static String escape(String name) {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            if (Character.isLetterOrDigit(c)) {
                result.append(c);
            } else {
                result.append('-');
            }

        }
        return result.toString();
    }
}

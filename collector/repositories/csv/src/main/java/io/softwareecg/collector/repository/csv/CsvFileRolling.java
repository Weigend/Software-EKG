package io.softwareecg.collector.repository.csv;

import io.softwareecg.collector.common.CollectorProperties;

import java.time.DateTimeException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * When the CSV repository begins a new file, and how many files it keeps. Set in
 * {@code collector.properties}:
 * <pre>
 *   io.softwareecg.collector.source.rollingPolicyPattern=%d{yyyy-MM-dd}
 *   Csv4jRepository.MaxFileSize=30MB
 *   Csv4jRepository.MaxBackupIndex=0
 * </pre>
 * <ul>
 *   <li><b>By time.</b> The pattern names the period a file holds: {@code %d{yyyy-MM-dd}} a day
 *       (the default), {@code %d{YYYY-'W'ww}} a week, {@code %d{yyyy-MM}} a month,
 *       {@code %d{yyyy-MM-dd-HH}} an hour. The period is part of the name of the file. A row
 *       belongs to the period of its own time, not of the moment it is written - what a source
 *       delivers of yesterday goes into the file of yesterday. {@code none} turns this off.</li>
 *   <li><b>By size.</b> A file that would become larger than the maximum is followed by a
 *       further part of the same period.</li>
 *   <li><b>How many.</b> Nothing is deleted unless a number of files to keep is set. With one,
 *       the oldest files of a job are deleted when it has more.</li>
 * </ul>
 *
 * @param periodPattern  the date pattern of a period, {@code null} if files are not begun by time
 * @param periodFormat   the same as a formatter
 * @param maxFileBytes   the size from which on a further file is begun
 * @param maxClosedFiles how many files of a job are kept besides the one written; 0 for all
 * @param zone           the time zone periods begin and end in
 */
record CsvFileRolling(String periodPattern, DateTimeFormatter periodFormat, long maxFileBytes, int maxClosedFiles,
                      ZoneId zone) {

    static final String MAX_BACKUP_INDEX_KEY = "Csv4jRepository.MaxBackupIndex";

    static final String MAX_FILE_SIZE_KEY = "Csv4jRepository.MaxFileSize";

    static final String ROLLING_PATTERN_KEY = "io.softwareecg.collector.source.rollingPolicyPattern";

    private static final Pattern DATE_OF_PATTERN = Pattern.compile("%d\\{([^}]*)}");

    private static final Pattern SIZE = Pattern.compile("(\\d+)\\s*(B|KB|MB|GB)?", Pattern.CASE_INSENSITIVE);

    /** What must not be part of the name of a file, on any system. */
    private static final Pattern NOT_IN_FILE_NAMES = Pattern.compile("[/\\\\:*?\"<>|\\s]");

    /**
     * @return the settings of {@code collector.properties}
     * @throws IllegalStateException if a setting is not usable
     */
    static CsvFileRolling configured() {
        CollectorProperties properties = CollectorProperties.getProperties();
        return of(CollectorProperties.getRollingPolicyPattern(),
                properties.getProperty(MAX_FILE_SIZE_KEY, "30MB"),
                properties.getProperty(MAX_BACKUP_INDEX_KEY, "0"),
                ZoneId.systemDefault());
    }

    /**
     * @param rollingPattern {@code %d{...}} with a date pattern, or the date pattern alone; blank,
     *                       {@code none} or {@code off} for no files by time
     * @param maxFileSize    a number with B, KB, MB or GB; a number alone counts bytes
     * @param maxBackupIndex how many files of a job are kept besides the one written; 0 for all
     * @throws IllegalStateException if a setting is not usable
     */
    static CsvFileRolling of(String rollingPattern, String maxFileSize, String maxBackupIndex, ZoneId zone) {
        String pattern = datePattern(rollingPattern);
        DateTimeFormatter format = null;
        if (pattern != null) {
            try {
                // weeks as ISO 8601 counts them, and names in English, on every machine
                format = DateTimeFormatter.ofPattern(pattern, Locale.UK).withZone(zone);
                String sample = format.format(Instant.now());
                if (sample.isEmpty() || NOT_IN_FILE_NAMES.matcher(sample).find()) {
                    throw invalid(ROLLING_PATTERN_KEY, rollingPattern, "it becomes '" + sample + "', which cannot be "
                            + "part of the name of a file. Write it like %d{yyyy-MM-dd}.");
                }
            } catch (IllegalArgumentException | DateTimeException e) {
                throw invalid(ROLLING_PATTERN_KEY, rollingPattern, "it is not a pattern for dates (" + e.getMessage()
                        + "). Write it like %d{yyyy-MM-dd}, or none.");
            }
        }

        Matcher size = SIZE.matcher(maxFileSize == null ? "" : maxFileSize.strip());
        if (!size.matches() || Long.parseLong(size.group(1)) == 0) {
            throw invalid(MAX_FILE_SIZE_KEY, maxFileSize, "write a size like 30MB, 500KB or 2GB.");
        }
        long bytes = Long.parseLong(size.group(1));
        String unit = size.group(2) == null ? "B" : size.group(2).toUpperCase(Locale.ROOT);
        bytes *= switch (unit) {
            case "KB" -> 1L << 10;
            case "MB" -> 1L << 20;
            case "GB" -> 1L << 30;
            default -> 1L;
        };

        int keep;
        try {
            keep = Math.max(0, Integer.parseInt(maxBackupIndex == null ? "" : maxBackupIndex.strip()));
        } catch (NumberFormatException e) {
            throw invalid(MAX_BACKUP_INDEX_KEY, maxBackupIndex, "write the number of files to keep, or 0 to keep all.");
        }
        return new CsvFileRolling(pattern, format, bytes, keep, zone);
    }

    private static String datePattern(String rollingPattern) {
        if (rollingPattern == null) {
            return null;
        }
        String text = rollingPattern.strip();
        if (text.isEmpty() || "none".equalsIgnoreCase(text) || "off".equalsIgnoreCase(text)) {
            return null;
        }
        Matcher date = DATE_OF_PATTERN.matcher(text);
        return date.find() ? date.group(1) : text;
    }

    private static IllegalStateException invalid(String key, String value, String what) {
        return new IllegalStateException("The setting " + key + "=" + value + " of the CSV repository is not usable: "
                + what);
    }

    /**
     * @return the period a row of that time belongs to, as it is written in the name of the file;
     *         empty if files are not begun by time
     */
    String periodOf(long time) {
        return periodFormat == null ? "" : periodFormat.format(Instant.ofEpochMilli(time));
    }
}

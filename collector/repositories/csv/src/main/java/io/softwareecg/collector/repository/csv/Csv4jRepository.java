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
package io.softwareecg.collector.repository.csv;

import io.softwareecg.collector.common.LoggerFactory;
import io.softwareecg.collector.dt.TimeSeriesHeader;
import io.softwareecg.collector.repository.StaticTimeSeriesRepository;
import io.softwareecg.collector.repository.common.CommonConstants;
import org.slf4j.Logger;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.text.NumberFormat;
import java.util.ArrayDeque;
import java.util.Date;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Writes the values of one job - a host, a process and a metric group - as CSV files.
 * <p>
 * The files of a job tell by their names what time they hold, and each one can be read by
 * itself: it begins with the header.
 * <pre>
 *   db-01_global_os_2026-11-20.csv          the day before
 *   db-01_global_os_2026-11-21.csv          today
 *   db-01_global_os_2026-11-21_2.csv        ... and what did not fit into the first file of today
 * </pre>
 * A new file is begun
 * <ul>
 *   <li>when the period changes - a day by default ({@link CsvFileRolling}); a row goes into the
 *       file of the period of its own time;</li>
 *   <li>when the file would become larger than the maximum;</li>
 *   <li>when the series of the job changed, so that the header of the file would not fit the
 *       rows.</li>
 * </ul>
 * A file is never renamed, and none is deleted unless a number of files to keep is configured.
 * Every name ends with {@code .csv}, which is what the import of the workbench looks for.
 */
public class Csv4jRepository implements StaticTimeSeriesRepository {

    private static final Logger LOGGER = LoggerFactory.create();

    private static final String CSV = ".csv";

    /**
     * The delimiter between the csv fields.
     */
    private static final char CSV_DELIMITER = ';';

    private static final byte[] LINE_SEPARATOR = System.lineSeparator().getBytes(StandardCharsets.UTF_8);

    /**
     * The files a job has written and closed, oldest first, by the path its files begin with.
     * A job gets a new repository when its series change; what it wrote before still counts
     * when files are limited.
     */
    private static final Map<String, Deque<File>> CLOSED_FILES = new ConcurrentHashMap<>();

    /**
     * The path the files of the job begin with: their directory and what names the job.
     */
    private final String logFileName;

    private final String measurement;

    private final CsvFileRolling rolling;

    /** The first line of every file, without the end of the line. */
    private String header;

    private File file;

    private OutputStream output;

    private long fileBytes;

    /** The period of the file that is written. */
    private String period;

    /** The number of the file among those of its period, beginning with 1. */
    private int part;

    /**
     * @param measurement the measurement that identifies the current 'session'
     * @param logFileName the path the files begin with: the directory and the name of the job
     * @throws IllegalStateException if a setting of the repository is not usable
     */
    public Csv4jRepository(String measurement, String logFileName) {
        this(measurement, logFileName, CsvFileRolling.configured());
    }

    Csv4jRepository(String measurement, String logFileName, CsvFileRolling rolling) {
        this.logFileName = logFileName;
        this.measurement = measurement;
        this.rolling = rolling;
    }

    /**
     * Sets the header of the files. The first file is begun with the first row: its name holds
     * the period of that row.
     */
    @Override
    public synchronized void createMeasurement(TimeSeriesHeader timeSeriesHeader, String[] metricLabels) {
        close();
        header = new CsvLog4jLayout(measurement, timeSeriesHeader, metricLabels.clone()).getHeader().stripTrailing();
    }

    /* (non-Javadoc)
     * @see io.softwareecg.collector.storage.StaticTimeSeriesStorage#addValueSetToMeasurement(Date, Double[])
     */
    @Override
    public synchronized void addValueSetToMeasurement(Date date, Double[] metricValues) {
        if (header == null) {
            throw new IllegalStateException("The CSV repository got values before it was told their names.");
        }
        StringBuilder line = new StringBuilder();

        NumberFormat numberFormat = CommonConstants.getDecimalFormat();
        line.append(date.getTime()).append(CSV_DELIMITER);

        // Write one column in the CSV for each metric value. A value that is missing leaves its
        // column empty: left out, it would shift the values after it under the wrong names.
        for (Double metricValue : metricValues) {
            if (metricValue != null) {
                line.append(numberFormat.format((double) metricValue));
            }
            line.append(CSV_DELIMITER);
        }

        // remove the last colon (CSV_DELIMITER)
        line.deleteCharAt(line.length() - 1);

        byte[] row = line.toString().getBytes(StandardCharsets.UTF_8);
        try {
            String rowPeriod = rolling.periodOf(date.getTime());
            if (output == null || !rowPeriod.equals(period)) {
                open(rowPeriod);
            } else if (fileBytes + row.length + LINE_SEPARATOR.length > rolling.maxFileBytes() && hasRows()) {
                begin(rowPeriod, part + 1);
            }
            output.write(row);
            output.write(LINE_SEPARATOR);
            // every row is in the file when this returns: a collector that is killed loses nothing
            output.flush();
            fileBytes += row.length + LINE_SEPARATOR.length;
        } catch (IOException e) {
            throw new IllegalStateException("The CSV file " + file + " cannot be written: " + e.getMessage(), e);
        }
    }

    /**
     * Nothing to do: every row is written to the file when it is added.
     * <p>
     * A job that collects from a source with a history of its own flushes after every cycle. This
     * must not begin a new file.
     */
    @Override
    public void flush() {
        // every row is flushed when it is added
    }

    /**
     * The storage is given up, at the end of the job or because its series changed. The file
     * stays as it is; a storage that follows continues it if its header fits, and begins a
     * further file if not.
     */
    @Override
    public synchronized void cleanUp() {
        close();
    }

    // ---------------------------------------------------------------------------------------------
    // files
    // ---------------------------------------------------------------------------------------------

    private boolean hasRows() {
        return fileBytes > header.getBytes(StandardCharsets.UTF_8).length + LINE_SEPARATOR.length;
    }

    private File fileOf(String filePeriod, int filePart) {
        return new File(logFileName + (filePeriod.isEmpty() ? "" : "_" + filePeriod)
                + (filePart == 1 ? "" : "_" + filePart) + CSV);
    }

    /**
     * Opens the file of a period: the last one there is, if it has the header of this storage
     * and room; a further one otherwise.
     */
    private void open(String filePeriod) throws IOException {
        int last = 0;
        while (fileOf(filePeriod, last + 1).exists()) {
            last++;
        }
        if (last > 0) {
            File existing = fileOf(filePeriod, last);
            if (existing.length() < rolling.maxFileBytes() && header.equals(firstLineOf(existing))) {
                close();
                file = existing;
                output = new BufferedOutputStream(Files.newOutputStream(existing.toPath(), StandardOpenOption.APPEND));
                fileBytes = existing.length();
                period = filePeriod;
                part = last;
                forget(existing);
                return;
            }
        }
        begin(filePeriod, last + 1);
    }

    private void begin(String filePeriod, int filePart) throws IOException {
        close();
        int free = filePart;
        while (fileOf(filePeriod, free).exists()) {
            free++;
        }
        file = fileOf(filePeriod, free);
        output = new BufferedOutputStream(Files.newOutputStream(file.toPath(), StandardOpenOption.CREATE_NEW));
        output.write(header.getBytes(StandardCharsets.UTF_8));
        output.write(LINE_SEPARATOR);
        output.flush();
        fileBytes = file.length();
        period = filePeriod;
        part = free;
        LOGGER.debug("Begun the CSV file {}.", file);
        dropOldFiles();
    }

    private synchronized void close() {
        if (output == null) {
            return;
        }
        try {
            output.close();
        } catch (IOException e) {
            LOGGER.warn("The CSV file {} could not be closed: {}", file, e.getMessage());
        }
        Deque<File> closed = CLOSED_FILES.computeIfAbsent(logFileName, name -> new ArrayDeque<>());
        synchronized (closed) {
            closed.addLast(file);
        }
        output = null;
        file = null;
        period = null;
    }

    /** A file that is written again is not a closed one any more. */
    private void forget(File reopened) {
        Deque<File> closed = CLOSED_FILES.get(logFileName);
        if (closed != null) {
            synchronized (closed) {
                closed.remove(reopened);
            }
        }
    }

    /**
     * Deletes the oldest files of the job if a number to keep is configured and it has more.
     */
    private void dropOldFiles() {
        if (rolling.maxClosedFiles() <= 0) {
            return;
        }
        Deque<File> closed = CLOSED_FILES.get(logFileName);
        if (closed == null) {
            return;
        }
        synchronized (closed) {
            while (closed.size() > rolling.maxClosedFiles()) {
                File oldest = closed.removeFirst();
                if (oldest.delete()) {
                    LOGGER.info("Deleted the CSV file {}: the job keeps {} files besides the one it writes ({}).",
                            oldest, rolling.maxClosedFiles(), CsvFileRolling.MAX_BACKUP_INDEX_KEY);
                } else if (oldest.exists()) {
                    LOGGER.warn("The CSV file {} could not be deleted.", oldest);
                }
            }
        }
    }

    private static String firstLineOf(File existing) throws IOException {
        try (BufferedReader reader = Files.newBufferedReader(existing.toPath(), StandardCharsets.UTF_8)) {
            return reader.readLine();
        }
    }
}

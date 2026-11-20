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
package io.softwareecg.collector.repository.archive;

import io.softwareecg.collector.common.CollectorProperties;
import io.softwareecg.collector.common.LoggerFactory;
import io.softwareecg.collector.dt.TimeSeriesHeader;
import io.softwareecg.collector.repository.StaticTimeSeriesRepository;
import io.softwareecg.collector.repository.common.CSVUtil;
import io.softwareecg.collector.repository.common.CommonConstants;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.text.DateFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.zip.GZIPOutputStream;

/**
 * Storage which supports backup of old logs in an archive.
 * One storage is only used by one collector job. This class is not thread-safe.
 * WARNING: This is hardcore stuff. If you change anything please contact me first !!!
 *
 */
public class ArchiveRepository implements StaticTimeSeriesRepository {

    /**
     * The class logger
     */
    private static final Logger LOG = LoggerFactory.create();

    /**
     * The delimiter between the csv fields.
     */
    private static final char CSV_DELIMITER = ';';

    /**
     * Milliseconds in one day
     */
    private static final long MILLISECONDS_IN_ONE_DAY = 86400000;

    /**
     * Line separator
     */
    private static final String LINE_SEPARATOR = "line.separator";

    /**
     * The pool for archive threads.
     */
    @SuppressWarnings("all")
    private static ScheduledThreadPoolExecutor archiveExecutor;

    /**
     * The current archive Folder is shared;
     */
    @SuppressWarnings("all")
    private static volatile File archiveFolder;

    /**
     * This lock has to be static. It controls the archive thread which does the backup (zip and delete).
     */
    private static final ReadWriteLock rwLockForBackup = new ReentrantReadWriteLock(true);

    /**
     * The list of storages to rollover to the current archive
     */
    @SuppressWarnings("all")
    private static List<ArchiveRepository> storages = Collections.synchronizedList(new ArrayList<ArchiveRepository>());

    /**
     * Defines the appender and file name
     */
    private OutputStream outputStream;

    /**
     * Our current file.
     */
    private final File logFile;

    /**
     * Our header.
     */
    private String header;

    /**
     * Assert that only one thread is using this instance.
     */
    private final long usingThreadId;


    /**
     * Public Constructor
     *
     * @param type        the type.
     * @param storageName defines the log file name without extension.
     */
    @SuppressWarnings("all")
    public ArchiveRepository(String storageName) {

        if (StringUtils.isEmpty(storageName)) {
            throw new IllegalStateException("Invalid storage name or type.");
        }

        // add self to the storage list
        storages.add(this);

        // my id
        usingThreadId = Thread.currentThread().getId();


        // construct file name.
        String logFileName = storageName + ".csv.gz";

        // open stream for file.
        try {
            logFile = new File(logFileName);
            outputStream = new GZIPOutputStream(new FileOutputStream(logFile));

            // only once
            synchronized (ArchiveRepository.class) {
                if (archiveFolder == null) {
                    archiveFolder = new File(getArchiveRoot() + "/" + getCalculatedArchiveFolder());
                }
            }
        } catch (IOException e) {
            throw new IllegalStateException("Can not open output stream for logfile", e);
        }

        // only once
        synchronized (ArchiveRepository.class) {
            //schedule the rotation such that is done for the first time at midnight and
            //is repeated every day at midnight

            if (archiveExecutor == null) {
                archiveExecutor = new ScheduledThreadPoolExecutor(1);
                archiveExecutor.scheduleAtFixedRate(new ArchiveTask(), getMillisecondsFromNowUntilMidnight(), MILLISECONDS_IN_ONE_DAY, TimeUnit.MILLISECONDS);

                // close gracefully our streams during shutdown
                Runtime.getRuntime().addShutdownHook(new ShutdownHook());
            }
        }
    }

    /**
     * Gets the milliseconds from the current time until midnight
     *
     * @return Milliseconds
     */
    private long getMillisecondsFromNowUntilMidnight() {
        Calendar c = Calendar.getInstance();
        long now = c.getTimeInMillis();
        c.add(Calendar.DATE, 1);
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        return c.getTimeInMillis() - now;
    }

    /* (non-Javadoc)
     * @see io.softwareecg.collector.storage.StaticTimeSeriesStorage#createMeasurement(TimeSeriesHeader, String[])
     */
    @Override
    public void createMeasurement(TimeSeriesHeader timeSeriesHeader, String[] metricLabels) {
        try {
            rwLockForBackup.readLock().lock();
            validateThreadId();
            this.header = createHeader(metricLabels);
            writeString(header);
        } finally {
            rwLockForBackup.readLock().unlock();
        }
    }

    /**
     * Write the header of the log file
     *
     * @param counters the the counter names.
     */
    private String createHeader(String[] counters) {
        try {
            rwLockForBackup.readLock().lock();

            validateThreadId();

            StringBuilder legend = new StringBuilder();
            legend.append("Date" + CSV_DELIMITER);
            for (String counter : counters) {
                legend.append(CSVUtil.escapeCsvColumnName(counter));
                legend.append(CSV_DELIMITER);
            }
            legend.append(System.getProperty(LINE_SEPARATOR));
            return legend.toString();
        } finally {
            rwLockForBackup.readLock().unlock();
        }
    }

    /* (non-Javadoc)
     * @see io.softwareecg.collector.storage.StaticTimeSeriesStorage#addValueSetToMeasurement(Date, Double[])
     */
    @Override
    public synchronized void addValueSetToMeasurement(Date date, Double[] metricValues) {

        try {
            rwLockForBackup.readLock().lock();

            validateThreadId();

            StringBuilder line = new StringBuilder();
            DateFormat format = CommonConstants.getDateFormat();
            NumberFormat numberFormat = CommonConstants.getDecimalFormat();
            line.append(format.format(date)).append(CSV_DELIMITER);
            for (int j = 0; j < metricValues.length; j++) {
                if (metricValues[j] != null) {
                    line.append(numberFormat.format((double) metricValues[j]));
                }
                if (j < metricValues.length - 1) {
                    line.append(CSV_DELIMITER);
                }
            }
            line.append(System.getProperty(LINE_SEPARATOR));
            writeString(line.toString());
        } finally {
            rwLockForBackup.readLock().unlock();
        }
    }

    @Override
    public void flush() {
        try {
            outputStream.flush();
        } catch (IOException e) {
            throw new IllegalStateException("Error occured during flushing the data.");
        }
    }

    /*
     * Write raw data.
     *
     * @param buffer the buffer.
     * @param count  the count of used bytes.
     */
//    @Override
//    public synchronized void writeRawLogdata(byte[] buffer, int count) {
//        try {
//            rwLockForBackup.readLock().lock();
//            outputStream.write(buffer, 0, count);
//        } catch (IOException e) {
//            throw new IllegalStateException("Cant write log data to logfile: " + logFile, e);
//        } finally {
//            rwLockForBackup.readLock().unlock();
//        }
//    }

    /**
     * GZ + move to archive folder.
     * This method has to be synchronized with all other methods writing on the output stream (CollectorJob). Make also sure
     * all other methods which access the output stream are synchronized.
     */
    synchronized void rotate() throws IOException {
        try {
            // exclusion with the backup process - avoid parallel rotation and parallel backup
            rwLockForBackup.readLock().lock();

            synchronized (ArchiveRepository.class) {
                if (!archiveFolder.exists()) {
                    // make sure archive exists
                    boolean success = archiveFolder.mkdirs();
                    if (!success) {
                        LOG.error("Can not create archive folder: " + archiveFolder);
                        return;
                    }
                }
            }

            //rotate own file to archive - this is synchronized only per instance -> runs parallel
            LOG.debug("Rotating storage " + logFile + " to folder: " + archiveFolder);

            // make sure everything is written
            outputStream.flush();
            outputStream.close();

            // rename
            boolean result = logFile.renameTo(ArchiveUtil.getArchiveFile(logFile, archiveFolder));
            if (!result) {
                LOG.error("Could not rename logfile");
            }
            resetLogfile();


        } finally {
            rwLockForBackup.readLock().unlock();
        }
    }

    /**
     * Backup the archive folder.
     */
    synchronized void backup() {
        // start only the backup if the archive was created
        File newArchiveFolder = new File(getArchiveRoot() + "/" + getCalculatedArchiveFolder());
        if (newArchiveFolder.equals(archiveFolder)) {
            return; // done
        }

        boolean newArchiveCreated = newArchiveFolder.mkdirs();
        // only once -> this is atomic
        if (newArchiveCreated) {
            LOG.info("New archive folder created: " + newArchiveFolder);
            try {
                // wait to start
                LOG.debug("Waiting for write lock to start backup of: " + archiveFolder);
                rwLockForBackup.writeLock().lock();
                LOG.info("Starting backup of archive:" + archiveFolder);
                ArchiveUtil.backupOldArchiveFolder(archiveFolder);
                LOG.info("Backup finished. Archive: " + archiveFolder);
                // end backup

                // reset archive folder
                archiveFolder = new File(getArchiveRoot() + "/" + getCalculatedArchiveFolder());
            } catch (Exception e) {
                LOG.error("Error during backup of archive folder: " + archiveFolder, e);
            } finally {
                rwLockForBackup.writeLock().unlock();
            }
        }
    }

    /**
     * Method get the archive root directory. If this does not exists, the folder will be created.
     *
     * @return a file path to the archive root folder.
     */
    private String getArchiveRoot() {
        String archiveRootFolder = this.logFile.getParentFile().getParentFile() + "/archiv";
        synchronized (ArchiveRepository.class) {
            File folder = new File(archiveRootFolder);
            if (!folder.exists()) {
                boolean success = folder.mkdirs();
                if (!success) {
                    throw new IllegalStateException("Cant create archive root folder: " + archiveRootFolder);

                }
            }
        }
        return archiveRootFolder;
    }

    /**
     * Resolves a directory pattern to a directory name based on the current date
     *
     * @return the folder name
     */
    private String getCalculatedArchiveFolder() {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(CollectorProperties.getArchiveDirectoryPattern(), Locale.US);
        return simpleDateFormat.format(new Date());
    }

    /**
     * Writes a string to the logfile.
     *
     * @param line a string.
     */
    private void writeString(String line) {
        try {
            outputStream.write(line.getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new IllegalStateException("Cant write data to logfile: " + logFile, e);
        }
    }

    /**
     * Resets the current logfile (create a new output stream without append).
     */
    private void resetLogfile() {
        try {
            outputStream = new GZIPOutputStream(new FileOutputStream(logFile, false));
            if (header != null) {
                this.writeString(header);
            }
        } catch (IOException e) {
            throw new IllegalStateException("Cant reset data in logfile: " + logFile, e);
        }
    }

    /**
     * Make sure only one thread use this instance.
     */
    private void validateThreadId() {
        if (this.usingThreadId != Thread.currentThread().getId()) {
            throw new IllegalStateException("Race condition: Two Thread accessing this instance. TID:"
                    + this.usingThreadId + ". Other TID: " + Thread.currentThread().getId());
        }
    }

    /**
     * Shutdown Hook.
     */
    private static class ShutdownHook extends Thread {
        @Override
        public void run() {
            synchronized (this) {
                rwLockForBackup.writeLock().lock();
                LOG.info("Shutdown-Hook called. Trying to close streams. ");
                List<ArchiveRepository> storageList = new ArrayList<>(storages);
                for (ArchiveRepository next : storageList) {

                    try {
                        next.outputStream.close();
                    } catch (IOException e) {
                        // no-op
                    }
                }
                LOG.info("Success. All streams are closed. !!! - Shutdown NOW.");
            }
        }
    }

    /**
     * Archive task.
     */
    private class ArchiveTask implements Runnable {
        @Override
        public void run() {
            try {
                List<ArchiveRepository> storageList = new ArrayList<>(storages);
                for (ArchiveRepository next : storageList) {
                    next.rotate();
                }
                backup();
            } catch (IOException e) {
                throw new IllegalStateException("Problem in archive task detected.", e);
            }
        }
    }
}

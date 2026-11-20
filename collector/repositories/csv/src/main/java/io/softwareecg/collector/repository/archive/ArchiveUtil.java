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
import io.softwareecg.collector.common.ZipUtil;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

/**
 */
public final class ArchiveUtil {

    /**
     * Archiv constant
     */
    public static final String ARCHIV = "/archiv";

    /**
     * Our logger.
     */
    private static final Logger LOG = LoggerFactory.create();

    /**
     * Utility class.
     */
    private ArchiveUtil() {

    }

    /**
     * Helper to get a archive name for a given logfile.
     *
     * @param logFile             the original file.
     * @param targetArchiveFolder the archive folder.
     * @return a new unique name in the archive.
     */
    public static File getArchiveFile(File logFile, File targetArchiveFolder) {
        //Determine the file name of the target .gz file
        String extension = logFile.getName().substring(logFile.getName().lastIndexOf(".gz") - ".xxx".length());
        String fileNameWithoutExtension = logFile.getName().substring(0, logFile.getName().length() - extension.length());
        return new File(targetArchiveFolder.getAbsolutePath() + "/" + fileNameWithoutExtension + "_" + getArchiveFileExtension() + extension);
    }

    /**
     * Move a file to the archive.
     *
     * @param logFile             the logfile.
     * @param targetArchiveFolder the folder.
     */
    public static void copyFileToArchive(File logFile, File targetArchiveFolder) {

        //Determine the file name of the target .gz file
        File archiveFile = getArchiveFile(logFile, targetArchiveFolder);
        try {
            FileUtils.copyFile(logFile, archiveFile);
        }
        catch (IOException e) {
            LOG.error("Can not move file to archive.", e);
        }
    }

    /**
     * Moves files from the given folder to a backup folder according to the file modification date
     *
     * @param oldFolder        folder containing files to be backed up
     * @param creationCalendar creation calendar
     */
    public static void moveFilesOlderThanDateToArchiveFolder(File oldFolder, Calendar creationCalendar) {

        //set the milliseconds to 0, for compatibility with linux
        creationCalendar.set(Calendar.MILLISECOND, 0);

        // move all files older than the creation date to archive
        File[] files = oldFolder.listFiles(File::isFile);
        if (files == null) {
            return;
        }
        for (File file : files) {
            doArchivation(creationCalendar, file);
        }
    }

    /**
     * Does the real archivation.
     *
     * @param creationCalendar the caldender.
     * @param file             the file.
     */
    private static void doArchivation(Calendar creationCalendar, File file) {
        Date dateOfCurrentFileModification = new Date(file.lastModified());

        // only files older than specific date should be moved to archive
        if (!dateOfCurrentFileModification.before(creationCalendar.getTime())) {
            return;
        }

        String folder = getArchiveFolderForFile(file);
        String archiveRootFolder = file.getParentFile().getParentFile() + ARCHIV;
        File archiveRoot = new File(archiveRootFolder);

        // create root archive folder if not exists
        boolean folderCreated = archiveRoot.mkdirs();
        if (!folderCreated && !archiveRoot.exists()) {
            throw new IllegalStateException("Could not create archive root folder" + archiveRoot);
        }

        File currentArchiveFolder = new File(archiveRootFolder + "/" + folder);
        folderCreated = currentArchiveFolder.mkdirs();
        if (!folderCreated && !currentArchiveFolder.exists()) {
            throw new IllegalStateException("Could not create archive folder" + currentArchiveFolder);
        }

        copyFileToArchive(file, currentArchiveFolder);
        boolean deleted = file.delete();
        if (!deleted) {
            LOG.warn("Could not delete file: " + file);
        }
    }


    /**
     * Zip archive folder.
     *
     * @param archiveFolder the folder.
     * @throws java.io.IOException when a error during zip or deletion happens.
     */
    public static void backupOldArchiveFolder(File archiveFolder) throws IOException {
        ZipUtil.zipDirectoryFastest(archiveFolder);
        try {
            FileUtils.deleteDirectory(archiveFolder);
        }
        catch (Exception e) {
            LOG.error("Can not delete archive folder: " + archiveFolder, e);
        }
        LOG.debug("END DELETE" + archiveFolder.getName());
    }

    /**
     * Resolves a directory pattern to a directory name based on the current date
     *
     * @return the folder name
     */
    private static String getArchiveFolderForFile(File file) {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(CollectorProperties.getArchiveDirectoryPattern(), Locale.US);
        return simpleDateFormat.format(new Date(file.lastModified()));
    }

    /**
     * Gets the file extension to make names unique.
     *
     * @return a date.
     */
    private static String getArchiveFileExtension() {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy.MM.dd'_'HH.mm.ss", Locale.US);
        return simpleDateFormat.format(new Date());
    }

}

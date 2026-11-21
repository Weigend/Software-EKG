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
import io.softwareecg.collector.config.CounterDescriptor;
import io.softwareecg.collector.repository.RepositoryFactory;
import io.softwareecg.collector.repository.StaticTimeSeriesRepository;
import io.softwareecg.collector.repository.csv.CsvUtil;

import java.io.File;
import java.util.Calendar;

/**
 * Factory to create repositories capable of achivation
 *
 */
public class ArchiveRepositoryFactory implements RepositoryFactory {

    /**
     * this date is used when archivation of already generated files of a previos collector run is done. Archivation is done by comparing
     * the change dates of the files with this date. Calendar class is used instead of Date class since Date cannot be modified. The modification of
     * the Calendar date is required because in linux systems the miiliseconds of date creation are not saved and are therefore 0 when being requested
     * from java. For this reason the actual date must be set with milliseconds 0.
     */
    private static final Calendar creationCalendarDate = Calendar.getInstance();

    /**
     * determines whether the the csv files created before the current collector run have been archivated
     */
    private static boolean archivationOfOldFilesPerformed = false;

    @Override
    public synchronized StaticTimeSeriesRepository createStaticRepository(CounterDescriptor descriptor) {
        File dir = createLogfileDirectory();

        // todo SKR: create JSON Header here

        String filePath = dir + "/" + CsvUtil.escape(descriptor.getNodeAddress()) + "_" + CsvUtil.escape(descriptor.getProcessLabel()) + "_"
                + CsvUtil.escape(descriptor.getMetricGroupId());


        return new ArchiveRepository(filePath);
    }

    /**
     * Creates a logfile directory.
     *
     * @return the directory.
     */
    private static synchronized File createLogfileDirectory() {
        String log = CollectorProperties.getLogdir();
        log += log.endsWith("/") ? "ekg" : "/ekg";
        File logDir = new File(log);
        if (!logDir.exists()) {
            boolean result = logDir.mkdirs();
            if (!result) {
                throw new IllegalStateException("Can not create log dir: " + logDir.getAbsolutePath());
            }
        }

        File dir = new File(log + (log.endsWith("/") ? "" : "/") + "current");
        if (!dir.exists()) {
            boolean result = dir.mkdir();
            if (!result) {
                throw new IllegalStateException("Can not create current dir: " + dir.getAbsolutePath());
            }
        } else {
            if (!archivationOfOldFilesPerformed) {
                ArchiveUtil.moveFilesOlderThanDateToArchiveFolder(dir, creationCalendarDate);
                archivationOfOldFilesPerformed = true;
            }
        }
        return dir;
    }
}

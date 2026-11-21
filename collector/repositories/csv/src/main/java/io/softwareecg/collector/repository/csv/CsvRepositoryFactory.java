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

import io.softwareecg.collector.common.CollectorProperties;
import io.softwareecg.collector.config.CounterDescriptor;
import io.softwareecg.collector.repository.RepositoryFactory;
import io.softwareecg.collector.repository.StaticTimeSeriesRepository;

import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;


/**
 * Factory to create a storage.
 *
 */
public class CsvRepositoryFactory implements RepositoryFactory {

    /**
     * The date is used for the name of the logfile dir and will be initialized only once.
     */
    private static final Date START_DATE = new Date();



    @Override
    public synchronized StaticTimeSeriesRepository createStaticRepository(CounterDescriptor descriptor) {

        File dir = createLogfileDirectory();

        String filePath = dir + "/" + CsvUtil.escape(descriptor.getNodeAddress()) + "_" + CsvUtil.escape(descriptor.getProcessLabel()) + "_"
                + CsvUtil.escape(descriptor.getMetricGroupId());

        DateFormat dateFormat = new SimpleDateFormat("yyyy.MM.dd HH:mm:ss", Locale.US);
        String date = dateFormat.format(START_DATE).replaceAll(" ", "-").replaceAll(":", "-").replaceAll("\\.", "-");

        return new Csv4jRepository(date, filePath);
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
                throw new IllegalStateException("Can not create log root dir: " + logDir.getAbsolutePath());
            }
        }

        DateFormat dateFormat = new SimpleDateFormat("yyyy.MM.dd HH:mm:ss", Locale.US);
        String date = dateFormat.format(START_DATE).replaceAll(" ", "-").replaceAll(":", "-").replaceAll("\\.", "-");
        File dir = new File(log + (log.endsWith("/") ? "" : "/") + date);
        if (!dir.exists()) {
            boolean result = dir.mkdir();
            if (!result) {
                throw new IllegalStateException("Can not create log dir: " + dir.getAbsolutePath());
            }
        }
        return dir;
    }
}

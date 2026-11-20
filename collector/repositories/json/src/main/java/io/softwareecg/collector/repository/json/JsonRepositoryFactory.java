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
package io.softwareecg.collector.repository.json;

import io.softwareecg.collector.common.CollectorProperties;
import io.softwareecg.collector.config.CounterDescriptor;
import io.softwareecg.collector.repository.RepositoryFactory;
import io.softwareecg.collector.repository.StaticTimeSeriesRepository;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;


/**
 * Factory to create a storage.
 *
 */
public class JsonRepositoryFactory implements RepositoryFactory {


    @Override
    public StaticTimeSeriesRepository createStaticRepository(CounterDescriptor descriptor) {

        String nodeName = descriptor.getNodeName();

        String filePath = createLogfileDirectory() + "/" + nodeName;
        return new Json4jRepository(filePath, nodeName, descriptor.getProcessLabel(), descriptor.getMetricGroupId(),
                descriptor.getNodeGroupId());
    }

    /**
     * Creates a logfile directory.
     *
     * @return the directory.
     */

    private static synchronized File createLogfileDirectory() {
        Path log = Paths.get(CollectorProperties.getLogdir(), "ekg", "json");
        File logDir = log.toFile();
        if (!logDir.exists()) {
            boolean result = logDir.mkdirs();
            if (!result) {
                throw new IllegalStateException("Can not create log root dir: " + logDir.getAbsolutePath());
            }
        }
        return logDir;
    }

}

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

import io.softwareecg.collector.config.TestConfigFactory;
import io.softwareecg.collector.dt.CollectorJobConfiguration;
import io.softwareecg.collector.dt.TimeSeriesHeader;
import io.softwareecg.collector.repository.RepositoryFactory;
import io.softwareecg.collector.repository.StaticTimeSeriesRepository;
import org.junit.Assert;
import org.junit.Test;

import java.util.Date;

/**
 * Created by johannes.weigend on 18.03.14.
 */
public class CsvRepositoryTest {


    /**
     * Test csv storage.
     */
    @Test
    public void testCsvStorage() {
        RepositoryFactory factory = new CsvRepositoryFactory();

        CollectorJobConfiguration config = TestConfigFactory.createTestConfig();

        Assert.assertNotNull(factory);
        StaticTimeSeriesRepository storage = factory.createStaticRepository(config);
        storage.createMeasurement(new TimeSeriesHeader(), new String[]{"Eins;Zwei;Drei"});
        int counter = 0;
        while (counter < 500) {
            storage.addValueSetToMeasurement(new Date(), new Double[]{1.0, 2.0, 3.0});
            counter++;
        }
        storage = factory.createStaticRepository(config);
        storage.createMeasurement(new TimeSeriesHeader(), new String[]{"Eins;Zwei;Drei;Vier"});
        counter = 0;
        while (counter < 500) {
            storage.addValueSetToMeasurement(new Date(), new Double[]{1.0, 2.0, 3.0, 4.0});
            counter++;
        }
    }

    @Test
    public void testCsvStorageLogs() {

        // not implemented yet
        //        StorageFactory factory = new CsvRepositoryFactory();
        //        Assert.assertNotNull(factory);
        //        StaticTimeSeriesStorage storage = factory.createStorage(RepositoryType.LOGFILE, "a", "b", "c", "d", null);
        //        storage.writeRawLogdata(new byte[1024], 1024);
    }
}

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

import io.softwareecg.collector.dt.TimeSeriesHeader;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.util.Date;

/**
 */
public class JsonStorageTest {

    @Rule
    public final TemporaryFolder temporaryFolder = new TemporaryFolder();

    /**
     * Test csv storage.
     */
    @Test
    public void testJsonStorage() {
        File output = new File(temporaryFolder.getRoot(), "metrics");
        Json4jRepository storage = new Json4jRepository(
                output.getAbsolutePath(), "host", "process", "counterGroup", "hostGroup");
        storage.createMeasurement(new TimeSeriesHeader(), new String[]{"Eins", "Zwei", "Drei"});
        int counter = 0;
        while (counter < 500) {
            storage.addValueSetToMeasurement(new Date(), new Double[]{1.0, 2.0, 3.0});
            counter++;
        }
    }
}

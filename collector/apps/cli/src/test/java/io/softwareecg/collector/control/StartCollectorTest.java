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
package io.softwareecg.collector.control;

import io.softwareecg.collector.StartCollector;
import io.softwareecg.collector.common.TestSupport;
import org.junit.Test;

import java.io.File;
import java.net.URL;

/**
 * Created by johannes.weigend on 10.04.14.
 */
public class StartCollectorTest {

    @Test
    public void testMain() throws Exception {
        File[] files = listFiles("ok");
        for (final File file : files) {
            TestSupport.testPrivateConstructor(StartCollector.class);
            new Thread(new Runnable() {
                @Override
                public void run() {
                    StartCollector.main(new String[]{file.getAbsolutePath()});
                }
            }).start();

            Thread.sleep(5000);
            CollectorCore.stop();
        }
    }

    /**
     * List files.
     *
     * @param folder the folder
     * @return the file[]
     */
    private File[] listFiles(String folder) {
        URL url = this.getClass().getResource(folder);
        File dir = new File(url.getPath());
        assert dir.exists();
        return dir.listFiles();
    }
}

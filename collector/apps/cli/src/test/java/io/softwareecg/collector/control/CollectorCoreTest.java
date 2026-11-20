//______________________________________________________________________________
//
//          Project:    Software EKG
//______________________________________________________________________________
//
//       created by:    Johannes Weigend, Josef Adersberger
//       creation date: 2009
//       changed by:    $Author: weigend $
//       change date:   $Date: 2010-07-21 13:03:04 +0200 (Mi, 21 Jul 2010) $
//       revision:      $Revision: 1082 $
//       description:   Reads Windows Performance MetricDefinition by using a custom
//                      jni dll. Reads JMX Memory MetricDefinition.
//______________________________________________________________________________
//
//        Copyright:    Weigend AM
//______________________________________________________________________________
package io.softwareecg.collector.control;

import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.net.URL;


/**
 * Test CollectorCore.
 */
public class CollectorCoreTest {

    /**
     * Test core.
     */
    @Test
    public void testCore() {
        File[] files = listFiles("ok");
        for (final File file : files) {
            try {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            CollectorCore.main(new String[]{file.getAbsolutePath(), File.createTempFile("xbcd", "x").getParentFile().getAbsolutePath()});
                        } catch (Exception e) {
                            // will be checked later - see below
                        }
                    }
                }).start();
            } catch (Exception e) {
                Assert.fail(e.getMessage());
            }
            try {
                Thread.sleep(5000);
                CollectorCore.stop();
            } catch (InterruptedException e) {
                Assert.fail(e.getMessage());
            }
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

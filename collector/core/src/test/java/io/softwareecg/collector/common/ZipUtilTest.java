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
package io.softwareecg.collector.common;


import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.net.URL;

/**
 * Created by johannes.weigend on 18.03.14.
 */
public class ZipUtilTest {

    @Test
    public void testConstructor() {
        TestSupport.testPrivateConstructor(ZipUtil.class);
    }


    @Test
    public void testZipDirectoryFastest() throws Exception {
        URL url = ZipUtilTest.class.getResource("/");
        File dir = new File(url.getPath());
        Assert.assertTrue(dir.exists());
        Assert.assertTrue(dir.isDirectory());

        ZipUtil.zipDirectoryFastest(dir);
        File zipFile = new File(dir.getParentFile(), dir.getName() + ".zip");
        Assert.assertTrue(zipFile.exists());
        Assert.assertTrue(zipFile.isFile());

        boolean deleteFlag = zipFile.delete();
        Assert.assertTrue(deleteFlag);

        try {
            ZipUtil.zipDirectoryFastest(new File("abc"));
            Assert.fail();
        } catch (IllegalStateException e) {
            // ok
        }
    }
}

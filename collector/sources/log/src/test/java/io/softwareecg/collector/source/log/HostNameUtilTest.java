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
package io.softwareecg.collector.source.log;


import org.junit.Assert;
import org.junit.Test;

/**
 * Created by martin.binder on 10.05.2014.
 */
public class HostNameUtilTest {

    @Test
    public void testLocalhostTrue() {
        Assert.assertTrue(HostNameUtil.isLocalhost("127.0.0.1"));
        Assert.assertTrue(HostNameUtil.isLocalhost("localhost"));
    }

    @Test
    public void testLocalhostFalse() {
        Assert.assertFalse(HostNameUtil.isLocalhost(null));
        Assert.assertFalse(HostNameUtil.isLocalhost(""));
        Assert.assertFalse(HostNameUtil.isLocalhost("10.10.10.1"));
    }
}

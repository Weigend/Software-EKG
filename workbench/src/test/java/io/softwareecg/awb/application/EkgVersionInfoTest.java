//______________________________________________________________________________
//
//                  ProjectConfiguration:    Software EKG
//______________________________________________________________________________
//
//                   Author:    Weigend AM 2009 - 2017
//______________________________________________________________________________
//
// Notice: This piece of software was created, designed and implemented by
// experienced craftsmen and innovators in Munich, Germany.
// Changes should be done with respect to the original design.
//______________________________________________________________________________
package io.softwareecg.awb.application;

import io.softwareecg.awb.application.about.VersionInfo;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import io.softwareecg.awb.application.EkgVersionInfo;
/**
 * Unit test for the {@link EkgVersionInfo}.
 *
 */
public class EkgVersionInfoTest {
    private VersionInfo info;

    @Before
    public void setUp() throws Exception {
        EkgVersionInfo ekgVersionInfo = new EkgVersionInfo();
         info = ekgVersionInfo;
    }

    @Test
    public void testGetProperties() throws Exception {
        assertThat(info.getBuildTime(), is(notNullValue()));
        assertThat(info.getBuildRevision(), not(isEmptyString()));
        assertThat(info.getVersionString(), not(isEmptyString()));
    }
}

//______________________________________________________________________________
//
//                  Project:    Software EKG
//______________________________________________________________________________
//
//                   Author:    Weigend AM 2009 - 2017
//______________________________________________________________________________
//
// Notice: This piece of software was created, designed and implemented by
// experienced craftsmen and innovators in Munich, Germany.
// Changes should be done with respect to the original design.
//______________________________________________________________________________
package io.softwareecg.awb.da.solr._legacy;

import org.junit.AfterClass;
import org.junit.BeforeClass;

/**
 * Base class for integration testing of tsquery and types.
 * <p/>
 * Legacy base class for disabled Solr integration tests.
 *
 */
public abstract class AbstractSolrIntegrationTest {

    @BeforeClass
    public static void initLookup() {
    }

    @AfterClass
    public static void shutdownLookup() {
    }
}

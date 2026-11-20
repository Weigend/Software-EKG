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
package io.softwareecg.collector.repository.rdb.solr.config;

import io.softwareecg.collector.repository.rdb.api.RdbProperties;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Created by f.lautenschlager on 09.04.14.
 */
public class RdbPropertiesTest {

    @BeforeClass
    public static void beforeTestClass() {
        System.setProperty("collectorRepositoryPropertiesPath", "/io/softwareecg/collector/repository/rdb/solr/config/somePath.properties");
    }

    //TODO: how to test different property files?
    @Test
    public void testFallbackProperties() {
        assertThat(RdbProperties.getCommitMaxUpdateEvents(), is(2));
        assertThat(RdbProperties.getStorageCommitInterval(), is("00:00:04"));
    }
}

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
package io.softwareecg.awb.da.solr;


import io.softwareecg.awb.da.solr.embedded.DefaultSolrConfig;
import io.softwareecg.awb.da.solr.embedded.SolrConfiguration;

import java.io.File;

/**
 * Test implementation for {@link SolrConfiguration}.
 *
 */
public class TestSolrConfiguration implements SolrConfiguration {

    @Override
    public File getSolrConfig() {
        return new File(getSolrHomeDir(), "solr.xml");
    }

    @Override
    public File getSolrHomeDir() {
        return new File(getEkgSolrBaseDir());
    }

    public String getEkgSolrBaseDir() {
        return System.getProperty(DefaultSolrConfig.SOLR_BASE_DIR_PROPERTY, "build/solr/");
    }
}

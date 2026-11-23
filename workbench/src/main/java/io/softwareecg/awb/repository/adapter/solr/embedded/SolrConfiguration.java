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
package io.softwareecg.awb.repository.adapter.solr.embedded;

import java.io.File;

/**
 * Get the necessary configuration for the embedded solr.
 *
 */
public interface SolrConfiguration {

    /**
     * Get the path to the solr.xml config file.
     *
     * @return The solr.xml config file path.
     */
    File getSolrConfig();

    /**
     * Get the path to solr home.
     *
     * @return The path to solr home.
     */
    File getSolrHomeDir();
}

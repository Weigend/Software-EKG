//______________________________________________________________________________
//
//          Project:    Software EKG
//______________________________________________________________________________
//
//       created by:    Johannes Weigend
//       creation date: 8.5.2012
//       changed by:    $Author$
//       change date:   $Date$
//       revision:      $Revision$
//       description:   Type field for solr table scheme.
//______________________________________________________________________________
//
//        Copyright:    Weigend AM
//______________________________________________________________________________
package io.softwareecg.awb.timeseries.schema;


/**
 * An enumeration that represents the different types of
 * document types in the EKG repository/storage.
 * The DocType has the similar meaning and functionality as
 * the Discriminator known from traditional databases.
 *
 * @since 12.01.2017
 */
public enum DocumentType {

    /**
     * Time series data with a metadata block and
     * (in case of local project type) time-value tuples
     * that represent the series. Stored in Solr/SolrCloud.
     */
    TIME_SERIES,

    /**
     * @deprecated Bookmarks are now stored in the H2 metadata database, not in Solr.
     */
    @Deprecated
    METRIC_BOOKMARK,

    /**
     * @deprecated Bookmark groups are now stored in the H2 metadata database, not in Solr.
     */
    @Deprecated
    BOOKMARK_GROUP,

    /**
     * @deprecated Repository configurations are now stored in the H2 metadata database, not in Solr.
     */
    @Deprecated
    REPOSITORY,

    /**
     * @deprecated Projects are now stored in the H2 metadata database, not in Solr.
     */
    @Deprecated
    PROJECT,

    /**
     * All type of documents
     * (= equal behaviour to no doc type filter)
     */
    ALL
}

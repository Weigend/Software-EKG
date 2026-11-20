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
package io.softwareecg.collector.repository.rdb.api.schema;


/**
 * An enumeration that represents the different types of
 * document types in the EKG repository/storage.
 * The DocType has the similar meaning and functionality as
 * the Discriminator known from traditional databases.
 *
 * Attention: Changes to this class has an impact to the awb read functionality
 *
 * @since 12.01.2017
 */
public enum DocType {

    /**
     * time series data with a metadata block and
     * (in case of local project type) time-value tuples
     * that represent the series
     */
    TIME_SERIES,

    /**
     * A repository definition that contains the connection
     * data and some meta data of (remote) repositories
     * stored as config in the current storage.
     */
    REPOSITORY,

    /**
     * An EKG AWB project that gives a set of time series the
     * necessary context to display interpret them correctly.
     */
    PROJECT,

    /**
     * All type of documents
     * (= equal behaviour to no doc type filter)
     */
    ALL
}

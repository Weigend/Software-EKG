package io.softwareecg.awb.importer;

import io.softwareecg.awb.importer.framework.SplitSourceProjectImportStrategy;

/**
 * This interface represents EKG import modules that uses the EKG split source
 * feature to make time series data from remote source available.
 *
 * The split source feature is a mechanism that use local imported metadata to filter
 * and load time series records from a remote source. This is useful than the series
 * data is to much to retrieve it from the remote source. The records are loaded by the
 * {@link io.softwareecg.awb.query.spi.RemoteSeriesSource} of the importer.
 *
 * @since 13.07.2018
 */
public interface SplitSourceProjectSupport {

    /**
     * Returns the an implementation of SplitSourceProjectImportStrategy that defines the
     * execution plan how to retrieve, filter and convert the meta data from the remote source.
     *
     * @return the ImportStrategy with the defined execution pipeline
     */
    SplitSourceProjectImportStrategy getSplitSourceProjectImportStrategy();
}

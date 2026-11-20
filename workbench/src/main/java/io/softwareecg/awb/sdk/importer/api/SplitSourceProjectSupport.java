package io.softwareecg.awb.sdk.importer.api;

import io.softwareecg.awb.sdk.importer.tasks.SplitSourceProjectImportStrategy;

/**
 * This interface represents EKG import modules that uses the EKG split source
 * feature to make time series data from remote source available.
 *
 * The split source feature is a mechanism that use local imported metadata to filter
 * and load time series records from a remote source. This is useful than the series
 * data is to much to retrieve it from the remote source.
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

    /**
     * Returns the proxy client that will use by the EKG repository layer to load the
     * time series records on demand from a remote source instead the own EKG repository.
     *
     * @return the proxy that knows the remote source interfaces and required credentials to retrieve the data.
     */
    RemoteSeriesDataFetcher getSeriesDataFetcher();
}

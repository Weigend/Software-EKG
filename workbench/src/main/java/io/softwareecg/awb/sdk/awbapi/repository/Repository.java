package io.softwareecg.awb.sdk.awbapi.repository;

/**
 * Base API of each EKG repository and the only interface
 * that is visible for Importer that normally should not have
 * a need to interact with repositories directly.
 *
 * @since 30.01.2020
 */
public interface Repository {

    /**
     * Get the unique types id within the currently used types config storage.
     *
     * @return The unique types id.
     */
    String getId();

    /**
     * Returns the name of this types.
     * <p>
     * In some cases the name may be user defined.
     *
     * @return the types name.
     */
    String getRepositoryName();

}

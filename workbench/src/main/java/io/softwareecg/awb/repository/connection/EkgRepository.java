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
package io.softwareecg.awb.repository.connection;



import io.softwareecg.awb.repository.api.Repository;
import io.softwareecg.awb.repository.persistence.TimeSeriesRepository;
import io.softwareecg.awb.repository.api.ResourceAuthType;

/**
 * Defines a types to store data within and query that data from a backend storage.
 *
 */
public interface EkgRepository extends Repository {

    /**
     * Returns the uri to access this types.
     * <p>
     * In some cases the name may be user defined.
     *
     * @return The types uri.
     */
    String getConnectionUrl();

    String getDBIndexName();

    /**
     * Returns the type of this types.
     *
     * @return The types type.
     */
    EkgRepositoryDbType getEkgRepositoryDbType();

    /**
     * Returns the authentication type the types used
     * for client connections
     *
     * @return the authentication type used to connect with the types
     */
    ResourceAuthType getRepositoryAuthType();

    /**
     * Returns the optional username used for authentication
     * if authentication type is not "none".
     *
     * @return the user name used than an authentication is in use
     */
    String getUsername();

    /**
     * Returns the optional password used for authentication
     * if authentication type is not "none".
     *
     * @return password used than an authentication is in use
     */
    String getPassword();

    /**
     * Returns the a Repository client that can used to
     * read, query and write data from and to the types.
     *
     * @return a search client instance
     */
    TimeSeriesRepository getRepositoryClient();


    /**
     * Shutdown the repository to stop it gracefully if
     * required.
     */
    default void close() {

    }


    /**
     * Factory to initialize a concrete {@link EkgRepository}.
     */
    interface Factory {
        /**
         * Initialize the concrete {@link EkgRepository} with the given information.
         *
         * @param id   The unique types id.
         * @param name The types name.
         * @param connectionUrl  The uri to access the types.
         * @param dbIndexName  the name of the search index/table in the remote types/database
         * @param ekgRepositoryDbType The type of the types.
         * @param resourceAuthType The authentication type the types used for client connections
         * @param username The username used for authentication (optional)
         * @param password The password used for authentication (optional)
         *
         * @return An instance of the concrete types.
         * @throws IllegalArgumentException In case of the factory can not create an instance of the given {@link EkgRepositoryDbType}.
         */
        EkgRepository getInstance(String id, String name, String connectionUrl, String dbIndexName, EkgRepositoryDbType ekgRepositoryDbType,
                                  ResourceAuthType resourceAuthType, String username, String password);

        /**
         * Check if the types type is supported.
         *
         * @param type The type to check.
         * @return True if the types type is supported.
         */
        boolean isTypeSupported(EkgRepositoryDbType type);

        /**
         * The index name that is stored when a repository is added or changed.
         *
         * @param indexName what the user entered, may be blank
         * @param repositoryName the name of the repository
         * @return the name to store; what the user entered, unless the technology needs a name
         */
        default String storedIndexName(String indexName, String repositoryName) {
            return indexName;
        }
    }
}

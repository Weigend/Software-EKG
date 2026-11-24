package io.softwareecg.awb.repository.connection;

import io.softwareecg.awb.repository.api.EkgRepository;
import io.softwareecg.awb.repository.model.EkgRepositoryDbType;
import io.softwareecg.awb.secrets.model.ResourceAuthType;

import java.util.List;

/**
 * Reads and writes how the repositories are connected. The other components use
 * {@link io.softwareecg.awb.repository.api.RepositoryService}.
 */
public interface RepositoryConfiguration {

    /**
     * @return the configured repositories and the embedded one, each of them once
     */
    List<EkgRepository> listEkgRepositories();

    /**
     * Add a new {@link EkgRepository} that will persisted in the embedded data store.
     * To add an repository mean to create a link to an external database the repository is based on.
     * Only the connection data, credentials and further metadata of the repository will stored in EKG, not
     * the payload the external database store itself.
     *
     * @param name the alias name of the repository shown in the UI
     * @param connectionUrl The uri to connect to the types.
     * @param indexName the name of the search index like the Solr Collection (Cloud) or the Solr Core (Standalone)
     * @param repositoryDbType the type of the database behind the repository like SOLR_EMBEDDED or ELASTICSEARCH_STANDALONE
     * @param resourceAuthType the type of authentication need to communicate with the repository (none, user-pass, API-Key)
     * @param username the username to use in case of authType = 'USERNAME_PASSWORD'
     * @param password the password to use in case of authType = 'USERNAME_PASSWORD'
     *
     * @return The created types instance.
     */
    EkgRepository addEkgRepository(String name, String connectionUrl, String indexName,
                                   EkgRepositoryDbType repositoryDbType,
                                   ResourceAuthType resourceAuthType,
                                   String username, String password);

    /**
     * Changes the connection data of an existing repository. The repository keeps its id, so
     * the projects stored for it stay attached and remain visible.
     *
     * @param repository the repository to change; must not be the embedded one
     * @param name the alias name of the repository shown in the UI
     * @param connectionUrl The uri to connect to the types.
     * @param indexName the name of the search index like the Solr Collection (Cloud) or the Solr Core (Standalone)
     * @param repositoryDbType the type of the database the repository is based on
     * @param resourceAuthType the type of authentication need to communicate with the repository (none, user-pass, API-Key)
     * @param username the username to use in case of authType = 'USERNAME_PASSWORD'
     * @param password the password to use in case of authType = 'USERNAME_PASSWORD'
     *
     * @return The changed repository instance; it has the id of the given one.
     */
    EkgRepository updateEkgRepository(EkgRepository repository, String name, String connectionUrl, String indexName,
                                      EkgRepositoryDbType repositoryDbType, ResourceAuthType resourceAuthType,
                                      String username, String password);
}

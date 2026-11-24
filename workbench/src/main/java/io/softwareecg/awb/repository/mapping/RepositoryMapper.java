package io.softwareecg.awb.repository.mapping;

import io.softwareecg.awb.repository.api.EkgRepository;
import io.softwareecg.awb.repository.model.EkgRepositoryDbType;
import io.softwareecg.awb.repository.model.RepositoryDefinition;
import io.softwareecg.awb.repository.model.TimeSeriesImportSource;
import io.softwareecg.awb.secrets.model.ResourceAuthType;
import io.softwareecg.awb.runtime.logging.EkgLogger;
import io.softwareecg.wfx.lookup.api.Lookup;
import org.slf4j.Logger;

import java.util.List;

/**
 * Utils class for mapping RepositoryDefinition to EkgRepository and vice versa.
 *
 * @since 18.08.2018
 */
public class RepositoryMapper {

    private static final Logger LOGGER = EkgLogger.get();

    private RepositoryMapper() {
    }

    /**
     * Constructor which will a SolrClassicRepository instance based
     * on the given repository data.
     *
     * @param repository the alias name of the repository shown in the UI
     * @param url        the connection string to the database the repository is based on (URL or Host:Port list)
     * @param indexName  the name of the search index like the Solr Collection (Cloud) or the Solr Core (Standalone)
     * @param type       the type of the database behind the repository like SOLR_EMBEDDED or ELASTICSEARCH_STANDALONE
     * @param authType   the type of authentication need to communicate with the repository (none, user-pass, API-Key)
     * @param username   the username to use in case of authType = 'USERNAME_PASSWORD'
     * @param password   the password to use in case of authType = 'USERNAME_PASSWORD'
     *
     * @return an RepositoryDefinition instance initialized with the specified data
     */
    public static RepositoryDefinition createRepositoryEt(String repository, String url, String indexName, EkgRepositoryDbType type,
                                                  ResourceAuthType authType, String username, String password) {
        RepositoryDefinition et = new RepositoryDefinition();

        et.setName(repository);
        et.setUrl(url);
        et.setDbIndexName(indexName);
        et.setAuthType(authType.toString());
        et.setUsername(username);
        et.setPassword(password);
        et.setRepositoryTypeString(type.name());

        return et;
    }

    /**
     * Build a concrete {@link EkgRepository} from a stored types entry.
     *
     * @param repositoryEntity The stored types entry.
     *
     * @return A concrete {@link EkgRepository} instance.
     */
    public static EkgRepository buildRepository(RepositoryDefinition repositoryEntity) {

        EkgRepository.Factory factory = factoryFor(repositoryEntity.getRepositoryType());

        if (factory == null) {
            return null;
        }

        return factory.getInstance(
                repositoryEntity.getId(),
                repositoryEntity.getName(),
                repositoryEntity.getUrl(),
                repositoryEntity.getDbIndexName(),
                EkgRepositoryDbType.valueOf(repositoryEntity.getRepositoryTypeString()),
                ResourceAuthType.valueOf(repositoryEntity.getAuthType()),
                repositoryEntity.getUsername(),
                repositoryEntity.getPassword());
    }

    /**
     * @return the index name to store for a repository of the type, see
     *         {@link EkgRepository.Factory#storedIndexName(String, String)}
     */
    public static String storedIndexName(EkgRepositoryDbType type, String indexName, String repositoryName) {
        EkgRepository.Factory factory = factoryFor(type);
        return factory == null ? indexName : factory.storedIndexName(indexName, repositoryName);
    }

    private static EkgRepository.Factory factoryFor(EkgRepositoryDbType type) {

        List<EkgRepository.Factory> repositoryFactories = Lookup.lookupAll(EkgRepository.Factory.class);

        if (repositoryFactories == null) {
            LOGGER.error("Unable to retrieve any EkgRepository.Factory from EKG application context.");
            return null;
        }

        for (EkgRepository.Factory factory : repositoryFactories) {
            if (factory.isTypeSupported(type)) {
                return factory;
            }
        }

        return null;
    }

    public static RepositoryDefinition mapToRepositoryEt(TimeSeriesImportSource sourceRepository) {
        RepositoryDefinition entity = new RepositoryDefinition();
        entity.setId(sourceRepository.getId());
        entity.setAccordingProjectName(sourceRepository.getAccordingProjectName());
        entity.setUrl(sourceRepository.getRepositoryUrl());
        entity.setAuthType(sourceRepository.getAuthType().toString());
        entity.setUsername(sourceRepository.getUsername());
        entity.setPassword(sourceRepository.getPassword());
        entity.setOauthServerUrl(sourceRepository.getOAuthServerUrl());
        entity.setSsoRefreshToken(sourceRepository.getSsoRefreshToken());

        return entity;
    }

    public static TimeSeriesImportSource mapToImporterSourceRepository(RepositoryDefinition repositoryEt) {
        TimeSeriesImportSource entity = new TimeSeriesImportSource();
        entity.setId(repositoryEt.getId());
        entity.setAccordingProjectName(repositoryEt.getAccordingProjectName());
        entity.setRepositoryUrl(repositoryEt.getUrl());
        entity.setAuthType(ResourceAuthType.valueOf(repositoryEt.getAuthType()));
        entity.setUsername(repositoryEt.getUsername());
        entity.setPassword(repositoryEt.getPassword());
        entity.setOAuthServerUrl(repositoryEt.getOauthServerUrl());
        entity.setSsoRefreshToken(repositoryEt.getSsoRefreshToken());

        return entity;
    }
}

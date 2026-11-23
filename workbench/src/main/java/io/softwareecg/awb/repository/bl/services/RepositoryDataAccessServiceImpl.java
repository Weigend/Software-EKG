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
package io.softwareecg.awb.repository.bl.services;


import io.softwareecg.awb.da.api.config.RepositoryConfigRow;
import io.softwareecg.awb.da.api.config.RepositoryConfigStore;
import io.softwareecg.awb.repository.api.EkgRepository;
import io.softwareecg.awb.da.api.RepositoryClient;
import io.softwareecg.awb.repository.api.RepositoryService;
import io.softwareecg.awb.repository.api.events.RepositoryChangeEvent;
import io.softwareecg.awb.repository.api.model.EkgRepositoryDbType;
import io.softwareecg.awb.da.api.types.Embedded;
import io.softwareecg.awb.repository.bl.mapper.RepositoryMapper;
import io.softwareecg.awb.repository.bl.repositories.ClickHouseRepositoryNaming;
import io.softwareecg.awb.repository.bl.repositories.RepositoryEt;
import io.softwareecg.awb.timeseries.port.TimeSeriesImportSource;
import io.softwareecg.awb.sdk.awbapi.repository.Repository;
import io.softwareecg.awb.sdk.awbapi.repository.ResourceAuthType;
import io.softwareecg.awb.sdk.awbapi.repository.SourceRepositoryService;
import io.softwareecg.wfx.platform.api.EventBus;
import io.softwareecg.wfx.lookup.api.Lookup;
import io.avaje.inject.External;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static io.softwareecg.awb.runtime.events.ChangedEvent.Change.ADD;
import static io.softwareecg.awb.runtime.events.ChangedEvent.Change.DELETE;
import static io.softwareecg.awb.runtime.events.ChangedEvent.Change.UPDATE;

/**
 *  A data access service that provides functionality to read, write & query
 *  information's about repository configurations from the H2 metadata database.
 *
 * @since 24.06.2018
 */
@Singleton
@SuppressWarnings("unused") // used via DI / Reflection
public class RepositoryDataAccessServiceImpl implements RepositoryService, SourceRepositoryService {

    private static final Logger LOGGER = LoggerFactory.getLogger(RepositoryDataAccessServiceImpl.class);

    private final RepositoryConfigStore configStore;

    private final EkgRepository embeddedRepository;

    @Inject
    public RepositoryDataAccessServiceImpl(@Embedded EkgRepository embeddedRepository,
                                           @External RepositoryConfigStore configStore) {
        this.configStore = configStore;
        this.embeddedRepository = embeddedRepository;
    }

    public RepositoryDataAccessServiceImpl(Repository repository) {
        this.configStore = Lookup.lookup(RepositoryConfigStore.class);
        this.embeddedRepository = null;
    }

    //=================================================================================================================
    // implementation of RepositoryClientAware interface (base of the RepositoryService)
    //=================================================================================================================

    @Override
    public void initializeService(RepositoryClient client) {
        // no-op: H2 repository is initialized via CDI
    }

    //=================================================================================================================
    // implementation of the API defined by the RepositoryService interface itself
    //=================================================================================================================

    @Override
    public List<EkgRepository> listEkgRepositories() {

        List<EkgRepository> result = new ArrayList<>();

        List<RepositoryConfigRow> rows = configStore.listAll();

        for (RepositoryConfigRow row : rows) {
            // skip import source entries - those are not EKG target repositories
            if (row.isImportSource) {
                continue;
            }

            RepositoryEt et = toRepositoryEt(row);
            EkgRepository repo = RepositoryMapper.buildRepository(et);
            if (repo != null) {
                result.add(repo);
            }
        }

        if (!result.contains(embeddedRepository)) {
            result.add(embeddedRepository);
        }

        return result;
    }

    @Override
    public List<TimeSeriesImportSource> listImportSourceRepositories() {
        return configStore.listAll().stream()
                .filter(r -> r.isImportSource)
                .map(RepositoryDataAccessServiceImpl::toImporterSourceRepository)
                .collect(Collectors.toList());
    }

    @Override
    public TimeSeriesImportSource queryImportSourceRepository(String projectName) {
        RepositoryConfigRow row = configStore.findImportSourceByProject(projectName);
        if (row == null) {
            return null;
        }
        return toImporterSourceRepository(row);
    }

    @Override
    public void deleteImportSourceRepositoryByProjectName(String projectName) {
        configStore.deleteImportSourceByProject(projectName);
    }

    @Override
    public void persistImporterSourceRepository(TimeSeriesImportSource sourceRepository) {

        // check for null values and invalid repositories.
        if (sourceRepository == null || StringUtils.isBlank(sourceRepository.getRepositoryUrl())) {
            LOGGER.error("The given TimeSeriesImportSource is null or has no connection URL.");
            return;
        }

        if (StringUtils.isBlank(sourceRepository.getAccordingProjectName())) {
            LOGGER.error("The given TimeSeriesImportSource doesn't define the project it belongs to.");
            return;
        }

        RepositoryConfigRow row = fromImporterSourceRepository(sourceRepository);
        configStore.insert(row);
    }

    @Override
    @SuppressWarnings("unchecked")
    public EkgRepository addEkgRepository(String name, String connectionUrl, String indexName,
                                          EkgRepositoryDbType repositoryDbType, ResourceAuthType resourceAuthType,
                                          String username, String password) {

        RepositoryConfigRow row = toTargetRepositoryRow(null, name, connectionUrl, indexName,
                repositoryDbType, resourceAuthType, username, password);

        configStore.insert(row);

        EkgRepository repository = RepositoryMapper.buildRepository(toRepositoryEt(row));
        Lookup.lookup(EventBus.class).publish(new RepositoryChangeEvent(repository, ADD, this));
        return repository;
    }

    @Override
    @SuppressWarnings("unchecked")
    public EkgRepository updateEkgRepository(EkgRepository existing, String name, String connectionUrl,
                                             String indexName, EkgRepositoryDbType repositoryDbType,
                                             ResourceAuthType resourceAuthType, String username, String password) {
        if (existing.isEmbedded()) {
            // not a stored configuration
            throw new IllegalArgumentException("The embedded repository cannot be changed.");
        }

        RepositoryConfigRow row = toTargetRepositoryRow(existing.getId(), name, connectionUrl, indexName,
                repositoryDbType, resourceAuthType, username, password);

        if (!configStore.update(row)) {
            // the configuration vanished in the meantime: store it again under the same id,
            // so the projects of the repository stay attached
            configStore.insert(row);
        }

        EkgRepository repository = RepositoryMapper.buildRepository(toRepositoryEt(row));
        Lookup.lookup(EventBus.class).publish(new RepositoryChangeEvent(repository, UPDATE, this));
        return repository;
    }

    private static RepositoryConfigRow toTargetRepositoryRow(String id, String name, String connectionUrl,
                                                             String indexName, EkgRepositoryDbType repositoryDbType,
                                                             ResourceAuthType resourceAuthType,
                                                             String username, String password) {
        RepositoryConfigRow row = new RepositoryConfigRow();
        row.id = id;
        row.name = name;
        row.url = connectionUrl;
        row.dbIndexName = repositoryDbType == EkgRepositoryDbType.CLICKHOUSE_CLOUD
                ? ClickHouseRepositoryNaming.resolveTableName(indexName, name, null)
                : indexName;
        row.dbType = repositoryDbType.name();
        row.authType = resourceAuthType.toString();
        row.username = username;
        row.password = password;
        row.isImportSource = false;
        return row;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void deleteRepository(String id) {
        configStore.deleteById(id);
        Lookup.lookup(EventBus.class).publish(new RepositoryChangeEvent(null, DELETE, this));
    }

    @Override
    public void deleteRepository(EkgRepository repository) {
        deleteRepository(repository.getId());
    }

    @Override
    @SuppressWarnings("unchecked")
    public void removeRepositoryWithProjects(EkgRepository repository) {
        if (repository.isEmbedded()) {
            // not a stored configuration; removing it would only wipe the local projects
            throw new IllegalArgumentException("The embedded repository cannot be removed.");
        }
        configStore.deleteWithProjects(repository.getId());
        Lookup.lookup(EventBus.class).publish(new RepositoryChangeEvent(null, DELETE, this));
    }

    //=================================================================================================================
    // mapping helpers between H2 rows and domain objects
    //=================================================================================================================

    private static RepositoryEt toRepositoryEt(RepositoryConfigRow row) {
        RepositoryEt et = new RepositoryEt();
        et.setId(row.id);
        et.setName(row.name);
        et.setUrl(row.url);
        et.setDbIndexName(row.dbIndexName);
        et.setRepositoryTypeString(row.dbType);
        et.setAuthType(row.authType);
        et.setUsername(row.username);
        et.setPassword(row.password);
        et.setOauthServerUrl(row.oauthServerUrl);
        et.setSsoRefreshToken(row.ssoRefreshToken);
        et.setImportSource(row.isImportSource);
        et.setAccordingProjectName(row.accordingProjectName);
        return et;
    }

    private static TimeSeriesImportSource toImporterSourceRepository(RepositoryConfigRow row) {
        TimeSeriesImportSource entity = new TimeSeriesImportSource();
        entity.setId(row.id);
        entity.setAccordingProjectName(row.accordingProjectName);
        entity.setRepositoryUrl(row.url);
        if (StringUtils.isNotBlank(row.authType)) {
            entity.setAuthType(ResourceAuthType.valueOf(row.authType));
        }
        entity.setUsername(row.username);
        entity.setPassword(row.password);
        entity.setOAuthServerUrl(row.oauthServerUrl);
        entity.setSsoRefreshToken(row.ssoRefreshToken);
        return entity;
    }

    private static RepositoryConfigRow fromImporterSourceRepository(TimeSeriesImportSource src) {
        RepositoryConfigRow row = new RepositoryConfigRow();
        row.id = src.getId();
        row.accordingProjectName = src.getAccordingProjectName();
        row.url = src.getRepositoryUrl();
        row.authType = src.getAuthType() != null ? src.getAuthType().toString() : null;
        row.username = src.getUsername();
        row.password = src.getPassword();
        row.oauthServerUrl = src.getOAuthServerUrl();
        row.ssoRefreshToken = src.getSsoRefreshToken();
        row.isImportSource = true;
        return row;
    }
}

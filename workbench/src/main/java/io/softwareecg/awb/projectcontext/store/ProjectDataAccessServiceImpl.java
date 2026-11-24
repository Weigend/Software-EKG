package io.softwareecg.awb.projectcontext.store;

import io.softwareecg.awb.metadata.api.DimensionAliasRow;
import io.softwareecg.awb.metadata.api.DimensionAliasStore;
import io.softwareecg.awb.metadata.api.ProjectRow;
import io.softwareecg.awb.metadata.api.ProjectStore;
import io.softwareecg.awb.projectcontext.api.ProjectDataAccessService;
import io.softwareecg.awb.projectcontext.api.Project;
import io.softwareecg.awb.repository.api.Repository;
import io.avaje.inject.External;
import io.avaje.inject.Prototype;
import io.softwareecg.wfx.platform.api.EventBus;
import io.softwareecg.wfx.lookup.api.Lookup;
import jakarta.inject.Inject;
import org.apache.commons.lang3.StringUtils;

import java.util.EventObject;
import java.util.List;
import java.util.stream.Collectors;

/**
 * The default ProjectDataAccessServiceImpl implementation that reads and writes project
 * metadata to the local H2 database.
 *
 * @since 23.06.2018
 */
@Prototype
@SuppressWarnings("unused") // class and constructors are used by DI
public class ProjectDataAccessServiceImpl implements ProjectDataAccessService {

    private final ProjectStore projectStore;
    private final DimensionAliasStore aliasStore;
    private final EventBus<EventObject> eventBus;

    private String repositoryId;

    @Inject
    public ProjectDataAccessServiceImpl(@External ProjectStore projectStore,
                                        @External DimensionAliasStore aliasStore,
                                        @External EventBus<EventObject> eventBus) {
        this.projectStore = projectStore;
        this.aliasStore = aliasStore;
        this.eventBus = eventBus;
    }

    public ProjectDataAccessServiceImpl(Repository repository) {
        this.projectStore = Lookup.lookup(ProjectStore.class);
        this.aliasStore = Lookup.lookup(DimensionAliasStore.class);
        this.eventBus = Lookup.lookup(EventBus.class);
        if (repository != null) {
            this.repositoryId = repository.getId();
        }
    }

    //=================================================================================================================
    // implementation of ProjectDataAccessService interface
    //=================================================================================================================

    @Override
    public List<Project> listProjects() {
        List<ProjectRow> rows = (repositoryId != null)
                ? projectStore.listByRepository(repositoryId)
                : projectStore.listAll();

        return rows.stream()
                .map(this::toProject)
                .collect(Collectors.toList());
    }

    @Override
    public List<Project> queryProjects(String projectName, boolean searchRemoteProjects) {
        return projectStore.findByName(projectName, searchRemoteProjects).stream()
                .map(this::toProject)
                .collect(Collectors.toList());
    }

    @Override
    public Project persistProject(Project project) {
        if (repositoryId != null && StringUtils.isBlank(project.getTargetRepositoryId())) {
            project.setTargetRepositoryId(repositoryId);
        }
        ProjectRow row = toRow(project);
        projectStore.insert(row);
        project.setId(row.id);
        persistAliases(project);
        return project;
    }

    @Override
    public Project updateExistingProject(Project project) {
        if (StringUtils.isBlank(project.getId())) {
            throw new IllegalArgumentException("The project '" + project.getName() + "' that should be update " +
                    "doesn't has an id. Check that the project was persisted before!");
        }

        projectStore.update(toRow(project));
        persistAliases(project);

        List<Project> projects = queryProjects(project.getName(), project.useSplitSource());
        if (projects.isEmpty()) {
            throw new IllegalStateException("Unable to find the project in the repository that was stored just before.");
        }

        return projects.get(0);
    }

    @Override
    public void deleteProjectByName(String projectName) {
        projectStore.deleteByName(projectName);
    }

    //=================================================================================================================
    // mapping between Project domain object and H2 row DTO
    //=================================================================================================================

    private Project toProject(ProjectRow row) {
        Project p = new Project();
        p.setId(row.id);
        p.setName(row.name);
        p.setDescription(row.description);

        if (StringUtils.isNotBlank(row.projectFlavor)) {
            p.setProjectFlavor(io.softwareecg.awb.projectcontext.api.ProjectFlavor.valueOf(row.projectFlavor));
        }
        if (StringUtils.isNotBlank(row.cloudPlatformType)) {
            p.setCloudPlatformType(row.cloudPlatformType);
        }

        p.setIsSplitSourceProject(row.isSplitSource);
        p.setImporterId(row.importerId);
        p.setTargetRepositoryId(row.targetRepositoryId);
        p.setCreatedDate(row.createdDate);
        p.setLastModifiedDate(row.lastModifiedDate);

        DimensionAliasRow alias = aliasStore.findByProject(row.name);
        if (alias != null) {
            p.setDimensionAliasHostGroup(alias.aliasHostGroup);
            p.setDimensionAliasHost(alias.aliasHost);
            p.setDimensionAliasNamespace(alias.aliasNamespace);
            p.setDimensionAliasService(alias.aliasService);
            p.setDimensionAliasPod(alias.aliasPod);
            p.setDimensionAliasContainer(alias.aliasContainer);
            p.setDimensionAliasMeasurement(alias.aliasMeasurement);
            p.setDimensionAliasProcess(alias.aliasProcess);
            p.setDimensionAliasMetricGroup(alias.aliasMetricGroup);
            p.setDimensionAliasMetricName(alias.aliasMetricName);
        }

        return p;
    }

    static ProjectRow toRow(Project p) {
        ProjectRow row = new ProjectRow();
        row.id = p.getId();
        row.name = p.getName();
        row.description = p.getDescription();
        row.projectFlavor = p.getProjectFlavor() != null ? p.getProjectFlavor().name() : null;
        row.cloudPlatformType = p.getCloudPlatformType() != null ? p.getCloudPlatformType().name() : null;
        row.isSplitSource = p.useSplitSource();
        row.importerId = p.getImporterId();
        row.targetRepositoryId = p.getTargetRepositoryId();
        row.createdDate = p.getCreatedDate();
        row.lastModifiedDate = p.getLastModifiedDate();
        return row;
    }

    private void persistAliases(Project p) {
        DimensionAliasRow alias = new DimensionAliasRow();
        alias.projectName = p.getName();
        alias.aliasHostGroup = p.getDimensionAliasHostGroup();
        alias.aliasHost = p.getDimensionAliasHost();
        alias.aliasNamespace = p.getDimensionAliasNamespace();
        alias.aliasService = p.getDimensionAliasService();
        alias.aliasPod = p.getDimensionAliasPod();
        alias.aliasContainer = p.getDimensionAliasContainer();
        alias.aliasMeasurement = p.getDimensionAliasMeasurement();
        alias.aliasProcess = p.getDimensionAliasProcess();
        alias.aliasMetricGroup = p.getDimensionAliasMetricGroup();
        alias.aliasMetricName = p.getDimensionAliasMetricName();
        aliasStore.insertOrUpdate(alias);
    }
}

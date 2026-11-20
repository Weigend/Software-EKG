package io.softwareecg.awb.da.api.projects;

import java.util.List;

/**
 * Persistenz-Schnittstelle fuer Projekt-Metadaten.
 */
public interface ProjectStore {

    List<ProjectRow> listAll();

    List<ProjectRow> listByRepository(String repositoryId);

    List<ProjectRow> findByName(String namePattern, boolean splitSource);

    ProjectRow insert(ProjectRow project);

    ProjectRow update(ProjectRow project);

    void deleteByName(String projectName);
}

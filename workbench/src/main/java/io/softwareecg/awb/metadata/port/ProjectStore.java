package io.softwareecg.awb.metadata.port;

import java.util.List;

import io.softwareecg.awb.metadata.model.ProjectRow;
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

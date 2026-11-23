package io.softwareecg.awb.metadata.adapter.h2;

import io.softwareecg.awb.metadata.model.ProjectRow;
import io.softwareecg.awb.metadata.port.ProjectStore;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * JDBC-based repository for project metadata stored in H2.
 * Provides CRUD operations for projects.
 *
 */
@Singleton
public class H2ProjectStore implements ProjectStore {

    private static final Logger LOGGER = LoggerFactory.getLogger(H2ProjectStore.class);

    private final H2MetadataDataSource dataSource;

    @Inject
    public H2ProjectStore(H2MetadataDataSource dataSource) {
        this.dataSource = dataSource;
    }

    /**
     * Lists all projects ordered by name.
     *
     * @return list of ProjectRow instances
     */
    public List<ProjectRow> listAll() {
        List<ProjectRow> result = new ArrayList<>();
        String sql = "SELECT * FROM projects ORDER BY name ASC";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                result.add(mapRow(rs));
            }

        } catch (SQLException e) {
            LOGGER.error("Failed to list projects from H2", e);
        }

        return result;
    }

    /**
     * Lists projects filtered by target repository ID.
     *
     * @param repositoryId the target repository ID
     * @return list of ProjectRow instances belonging to this repository
     */
    public List<ProjectRow> listByRepository(String repositoryId) {
        List<ProjectRow> result = new ArrayList<>();
        String sql = "SELECT * FROM projects WHERE target_repository_id = ? ORDER BY name ASC";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, repositoryId);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    result.add(mapRow(rs));
                }
            }

        } catch (SQLException e) {
            LOGGER.error("Failed to list projects by repository from H2", e);
        }

        return result;
    }

    /**
     * Assigns all projects without a target repository to the given repository ID.
     * This is a one-time migration for existing data that was created before
     * repository-based project filtering was introduced.
     *
     * @param repositoryId the repository ID to assign to unassigned projects
     * @return number of updated rows
     */
    public int assignUnassignedToRepository(String repositoryId) {
        String sql = "UPDATE projects SET target_repository_id = ? WHERE target_repository_id IS NULL";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, repositoryId);
            return stmt.executeUpdate();

        } catch (SQLException e) {
            LOGGER.error("Failed to assign unassigned projects to repository {}", repositoryId, e);
            return 0;
        }
    }

    /**
     * Queries projects by name pattern and split-source flag.
     *
     * @param namePattern the project name (supports * wildcard)
     * @param splitSource the split source flag
     * @return matching projects
     */
    public List<ProjectRow> findByName(String namePattern, boolean splitSource) {
        List<ProjectRow> result = new ArrayList<>();
        String sqlPattern = namePattern.replace("*", "%");
        String sql = "SELECT * FROM projects WHERE name LIKE ? AND is_split_source = ? ORDER BY name ASC";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, sqlPattern);
            stmt.setBoolean(2, splitSource);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    result.add(mapRow(rs));
                }
            }

        } catch (SQLException e) {
            LOGGER.error("Failed to query projects from H2", e);
        }

        return result;
    }

    /**
     * Inserts a new project. If the project has no id, one is generated.
     *
     * @param project the project to insert
     * @return the inserted project with id set
     */
    public ProjectRow insert(ProjectRow project) {
        if (project.id == null || project.id.isBlank()) {
            project.id = UUID.randomUUID().toString();
        }

        LocalDateTime now = LocalDateTime.now();
        project.createdDate = now;
        project.lastModifiedDate = now;

        String sql = "INSERT INTO projects (id, name, description, project_flavor, cloud_platform_type, " +
                "is_split_source, importer_id, target_repository_id, created_date, last_modified_date) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            setProjectParams(stmt, project);
            stmt.setTimestamp(9, Timestamp.valueOf(now));
            stmt.setTimestamp(10, Timestamp.valueOf(now));
            stmt.executeUpdate();

        } catch (SQLException e) {
            throw new IllegalStateException("Failed to insert project into H2", e);
        }

        return project;
    }

    /**
     * Updates an existing project identified by its id.
     *
     * @param project the project to update
     * @return the updated project
     */
    public ProjectRow update(ProjectRow project) {
        if (project.id == null || project.id.isBlank()) {
            throw new IllegalArgumentException("Cannot update project without id");
        }

        LocalDateTime now = LocalDateTime.now();
        project.lastModifiedDate = now;

        String sql = "UPDATE projects SET name = ?, description = ?, project_flavor = ?, " +
                "cloud_platform_type = ?, is_split_source = ?, importer_id = ?, target_repository_id = ?, " +
                "last_modified_date = ? WHERE id = ?";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, project.name);
            stmt.setString(2, project.description);
            stmt.setString(3, project.projectFlavor);
            stmt.setString(4, project.cloudPlatformType);
            stmt.setBoolean(5, project.isSplitSource);
            stmt.setString(6, project.importerId);
            stmt.setString(7, project.targetRepositoryId);
            stmt.setTimestamp(8, Timestamp.valueOf(now));
            stmt.setString(9, project.id);

            stmt.executeUpdate();

        } catch (SQLException e) {
            throw new IllegalStateException("Failed to update project in H2", e);
        }

        return project;
    }

    /**
     * Deletes a project by name. Associated entities are removed via FK CASCADE.
     *
     * @param projectName the project name
     */
    public void deleteByName(String projectName) {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement("DELETE FROM projects WHERE name = ?")) {

            stmt.setString(1, projectName);
            stmt.executeUpdate();

        } catch (SQLException e) {
            throw new IllegalStateException("Failed to delete project from H2", e);
        }
    }

    private void setProjectParams(PreparedStatement stmt, ProjectRow p) throws SQLException {
        stmt.setString(1, p.id);
        stmt.setString(2, p.name);
        stmt.setString(3, p.description);
        stmt.setString(4, p.projectFlavor);
        stmt.setString(5, p.cloudPlatformType);
        stmt.setBoolean(6, p.isSplitSource);
        stmt.setString(7, p.importerId);
        stmt.setString(8, p.targetRepositoryId);
    }

    private ProjectRow mapRow(ResultSet rs) throws SQLException {
        ProjectRow p = new ProjectRow();
        p.id = rs.getString("id");
        p.name = rs.getString("name");
        p.description = rs.getString("description");
        p.projectFlavor = rs.getString("project_flavor");
        p.cloudPlatformType = rs.getString("cloud_platform_type");
        p.isSplitSource = rs.getBoolean("is_split_source");
        p.importerId = rs.getString("importer_id");
        p.targetRepositoryId = rs.getString("target_repository_id");

        Timestamp created = rs.getTimestamp("created_date");
        p.createdDate = created != null ? created.toLocalDateTime() : null;

        Timestamp modified = rs.getTimestamp("last_modified_date");
        p.lastModifiedDate = modified != null ? modified.toLocalDateTime() : null;

        return p;
    }

}

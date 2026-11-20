package io.softwareecg.awb.da.h2;

import io.softwareecg.awb.da.api.config.RepositoryConfigRow;
import io.softwareecg.awb.da.api.config.RepositoryConfigStore;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * JDBC-based repository for repository configuration metadata stored in H2.
 * Handles both EKG repositories and importer source repositories.
 *
 */
@Singleton
public class H2RepositoryConfigRepository implements RepositoryConfigStore {

    private static final Logger LOGGER = LoggerFactory.getLogger(H2RepositoryConfigRepository.class);

    private final H2DataSource dataSource;

    @Inject
    public H2RepositoryConfigRepository(H2DataSource dataSource) {
        this.dataSource = dataSource;
    }

    /**
     * Lists all repository configurations (both EKG repos and importer sources).
     *
     * @return list of RepositoryConfigRow instances
     */
    public List<RepositoryConfigRow> listAll() {
        List<RepositoryConfigRow> result = new ArrayList<>();
        String sql = "SELECT * FROM repositories ORDER BY name DESC";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                result.add(mapRow(rs));
            }

        } catch (SQLException e) {
            LOGGER.error("Failed to list repositories from H2", e);
        }

        return result;
    }

    /**
     * Lists only non-import-source repositories (EKG target repositories).
     *
     * @return list of EKG repository configurations
     */
    public List<RepositoryConfigRow> listEkgRepositories() {
        List<RepositoryConfigRow> result = new ArrayList<>();
        String sql = "SELECT * FROM repositories WHERE is_import_source = FALSE ORDER BY name DESC";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                result.add(mapRow(rs));
            }

        } catch (SQLException e) {
            LOGGER.error("Failed to list EKG repositories from H2", e);
        }

        return result;
    }

    /**
     * Finds a single import source repository for the given project name.
     *
     * @param projectName the project name
     * @return the import source repository, or null if not found
     */
    public RepositoryConfigRow findImportSourceByProject(String projectName) {
        String sql = "SELECT * FROM repositories WHERE is_import_source = TRUE " +
                "AND according_project_name = ? ORDER BY name DESC";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, projectName);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    RepositoryConfigRow first = mapRow(rs);
                    if (rs.next()) {
                        throw new IllegalStateException(
                                "Found more than one source repository for project " + projectName);
                    }
                    return first;
                }
            }

        } catch (SQLException e) {
            throw new IllegalStateException("Failed to query import source repository from H2", e);
        }

        return null;
    }

    /**
     * Inserts a new repository configuration.
     *
     * @param row the repository to insert
     * @return the inserted row with id set
     */
    public RepositoryConfigRow insert(RepositoryConfigRow row) {
        if (row.id == null || row.id.isBlank()) {
            row.id = UUID.randomUUID().toString();
        }

        String sql = "INSERT INTO repositories (id, name, url, db_index_name, db_type, auth_type, " +
                "username, password, oauth_server_url, sso_refresh_token, is_import_source, " +
                "according_project_name) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, row.id);
            stmt.setString(2, row.name);
            stmt.setString(3, row.url);
            stmt.setString(4, row.dbIndexName);
            stmt.setString(5, row.dbType);
            stmt.setString(6, row.authType);
            stmt.setString(7, row.username);
            stmt.setString(8, row.password);
            stmt.setString(9, row.oauthServerUrl);
            stmt.setString(10, row.ssoRefreshToken);
            stmt.setBoolean(11, row.isImportSource);
            stmt.setString(12, row.accordingProjectName);

            stmt.executeUpdate();

        } catch (SQLException e) {
            throw new IllegalStateException("Failed to insert repository into H2", e);
        }

        return row;
    }

    /**
     * Updates the connection data of a target repository in place, keeping its id.
     *
     * @param row the new connection data; {@code row.id} names the repository to update
     * @return {@code false} if no repository with that id exists
     */
    public boolean update(RepositoryConfigRow row) {
        String sql = "UPDATE repositories SET name = ?, url = ?, db_index_name = ?, db_type = ?, " +
                "auth_type = ?, username = ?, password = ? WHERE id = ? AND is_import_source = FALSE";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, row.name);
            stmt.setString(2, row.url);
            stmt.setString(3, row.dbIndexName);
            stmt.setString(4, row.dbType);
            stmt.setString(5, row.authType);
            stmt.setString(6, row.username);
            stmt.setString(7, row.password);
            stmt.setString(8, row.id);

            return stmt.executeUpdate() > 0;

        } catch (SQLException e) {
            throw new IllegalStateException("Failed to update repository " + row.id + " in H2", e);
        }
    }

    /**
     * Deletes a repository by its id.
     *
     * @param id the repository id
     */
    public void deleteById(String id) {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement("DELETE FROM repositories WHERE id = ?")) {

            stmt.setString(1, id);
            stmt.executeUpdate();

        } catch (SQLException e) {
            LOGGER.warn("Failed to delete repository with id {} from H2", id, e);
        }
    }

    /**
     * Deletes a target repository and the metadata of its projects in one transaction.
     * Aliases, bookmarks and import sources follow their project through the foreign keys.
     *
     * @param id the repository id
     */
    public void deleteWithProjects(String id) {
        try (Connection conn = dataSource.getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement projects = conn.prepareStatement(
                         "DELETE FROM projects WHERE target_repository_id = ?");
                 PreparedStatement repository = conn.prepareStatement(
                         "DELETE FROM repositories WHERE id = ?")) {

                projects.setString(1, id);
                int deletedProjects = projects.executeUpdate();

                repository.setString(1, id);
                repository.executeUpdate();

                conn.commit();
                LOGGER.info("Deleted repository {} and the metadata of {} project(s) from H2", id, deletedProjects);

            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to delete repository " + id + " and its projects from H2", e);
        }
    }

    /**
     * Deletes all import source repositories for the given project name.
     *
     * @param projectName the project name
     */
    public void deleteImportSourceByProject(String projectName) {
        String sql = "DELETE FROM repositories WHERE is_import_source = TRUE AND according_project_name = ?";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, projectName);
            stmt.executeUpdate();

        } catch (SQLException e) {
            throw new IllegalStateException("Failed to delete import source repository from H2", e);
        }
    }

    private RepositoryConfigRow mapRow(ResultSet rs) throws SQLException {
        RepositoryConfigRow r = new RepositoryConfigRow();
        r.id = rs.getString("id");
        r.name = rs.getString("name");
        r.url = rs.getString("url");
        r.dbIndexName = rs.getString("db_index_name");
        r.dbType = rs.getString("db_type");
        r.authType = rs.getString("auth_type");
        r.username = rs.getString("username");
        r.password = rs.getString("password");
        r.oauthServerUrl = rs.getString("oauth_server_url");
        r.ssoRefreshToken = rs.getString("sso_refresh_token");
        r.isImportSource = rs.getBoolean("is_import_source");
        r.accordingProjectName = rs.getString("according_project_name");
        return r;
    }
}

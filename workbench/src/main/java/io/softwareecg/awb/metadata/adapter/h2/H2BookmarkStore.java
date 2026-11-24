package io.softwareecg.awb.metadata.adapter.h2;

import io.softwareecg.awb.metadata.model.BookmarkGroupRow;
import io.softwareecg.awb.metadata.model.MetricBookmarkRow;
import io.softwareecg.awb.metadata.model.MetricQueryBookmarkRow;
import io.softwareecg.awb.metadata.port.BookmarkStore;
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
 * JDBC-based repository for bookmark and bookmark group metadata stored in H2.
 *
 */
@Singleton
public class H2BookmarkStore implements BookmarkStore {

    private static final Logger LOGGER = LoggerFactory.getLogger(H2BookmarkStore.class);

    private final H2MetadataDataSource dataSource;

    @Inject
    public H2BookmarkStore(H2MetadataDataSource dataSource) {
        this.dataSource = dataSource;
    }

    // =========================================================================
    // Metric Bookmarks
    // =========================================================================

    /**
     * Lists metric bookmarks, optionally filtered by group id.
     *
     * @param bookmarkGroupId the group id to filter by, or null/blank for all
     * @return list of MetricBookmarkRow instances
     */
    public List<MetricBookmarkRow> listBookmarks(String bookmarkGroupId) {
        List<MetricBookmarkRow> result = new ArrayList<>();

        String sql;
        if (bookmarkGroupId != null && !bookmarkGroupId.isBlank()) {
            sql = "SELECT * FROM metric_bookmarks WHERE bookmark_group_id = ? ORDER BY name DESC";
        } else {
            sql = "SELECT * FROM metric_bookmarks ORDER BY name DESC";
        }

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            if (bookmarkGroupId != null && !bookmarkGroupId.isBlank()) {
                stmt.setString(1, bookmarkGroupId);
            }

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    result.add(mapBookmarkRow(rs));
                }
            }

        } catch (SQLException e) {
            LOGGER.error("Failed to list bookmarks from H2", e);
        }

        return result;
    }

    /**
     * Lists metric bookmarks filtered by project name.
     *
     * @param projectName the project name to filter by
     * @return list of MetricBookmarkRow instances for the given project
     */
    public List<MetricBookmarkRow> listBookmarksByProject(String projectName) {
        List<MetricBookmarkRow> result = new ArrayList<>();
        String sql = "SELECT * FROM metric_bookmarks WHERE project_name = ? ORDER BY name DESC";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, projectName);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    result.add(mapBookmarkRow(rs));
                }
            }

        } catch (SQLException e) {
            LOGGER.error("Failed to list bookmarks by project from H2", e);
        }

        return result;
    }

    /**
     * Inserts a new metric bookmark.
     *
     * @param row the bookmark to insert
     */
    public void insertBookmark(MetricBookmarkRow row) {
        if (row.id == null || row.id.isBlank()) {
            row.id = UUID.randomUUID().toString();
        }

        String sql = "INSERT INTO metric_bookmarks (id, name, bookmark_group_id, command_protocol, project_name) " +
                "VALUES (?, ?, ?, ?, ?)";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, row.id);
            stmt.setString(2, row.name);
            stmt.setString(3, row.bookmarkGroupId);
            stmt.setString(4, row.commandProtocol);
            stmt.setString(5, row.projectName);

            stmt.executeUpdate();

        } catch (SQLException e) {
            throw new IllegalStateException("Failed to insert bookmark into H2", e);
        }
    }

    /**
     * Deletes a metric bookmark by its id.
     *
     * @param id the bookmark id
     */
    public void deleteBookmark(String id) {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement("DELETE FROM metric_bookmarks WHERE id = ?")) {

            stmt.setString(1, id);
            stmt.executeUpdate();

        } catch (SQLException e) {
            throw new IllegalStateException("Failed to delete bookmark from H2", e);
        }
    }

    // =========================================================================
    // Metric Query Bookmarks
    // =========================================================================

    /**
     * Lists metric query bookmarks, optionally filtered by group id.
     *
     * @param bookmarkGroupId the group id to filter by, or null/blank for all
     * @return list of MetricQueryBookmarkRow instances
     */
    public List<MetricQueryBookmarkRow> listQueryBookmarks(String bookmarkGroupId) {
        List<MetricQueryBookmarkRow> result = new ArrayList<>();

        String sql;
        if (bookmarkGroupId != null && !bookmarkGroupId.isBlank()) {
            sql = "SELECT * FROM metric_query_bookmarks WHERE bookmark_group_id = ? ORDER BY name ASC";
        } else {
            sql = "SELECT * FROM metric_query_bookmarks ORDER BY name ASC";
        }

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            if (bookmarkGroupId != null && !bookmarkGroupId.isBlank()) {
                stmt.setString(1, bookmarkGroupId);
            }

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    result.add(mapQueryBookmarkRow(rs));
                }
            }

        } catch (SQLException e) {
            LOGGER.error("Failed to list query bookmarks from H2", e);
        }

        return result;
    }

    /**
     * Inserts a new metric query bookmark.
     *
     * @param row the query bookmark to insert
     */
    public void insertQueryBookmark(MetricQueryBookmarkRow row) {
        if (row.id == null || row.id.isBlank()) {
            row.id = UUID.randomUUID().toString();
        }

        String sql = "INSERT INTO metric_query_bookmarks " +
                "(id, name, bookmark_group_id, project_name, query_data) " +
                "VALUES (?, ?, ?, ?, ?)";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, row.id);
            stmt.setString(2, row.name);
            stmt.setString(3, row.bookmarkGroupId);
            stmt.setString(4, row.projectName);
            stmt.setString(5, row.queryData);

            stmt.executeUpdate();

        } catch (SQLException e) {
            throw new IllegalStateException("Failed to insert query bookmark into H2", e);
        }
    }

    /**
     * Deletes a metric query bookmark by its id.
     *
     * @param id the query bookmark id
     */
    public void deleteQueryBookmark(String id) {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement("DELETE FROM metric_query_bookmarks WHERE id = ?")) {

            stmt.setString(1, id);
            stmt.executeUpdate();

        } catch (SQLException e) {
            throw new IllegalStateException("Failed to delete query bookmark from H2", e);
        }
    }

    // =========================================================================
    // Bookmark Groups
    // =========================================================================

    /**
     * Lists bookmark groups where project_name is NULL (global groups).
     *
     * @return list of global BookmarkGroupRow instances
     */
    public List<BookmarkGroupRow> listGlobalGroups() {
        List<BookmarkGroupRow> result = new ArrayList<>();
        String sql = "SELECT * FROM bookmark_groups WHERE project_name IS NULL ORDER BY name ASC";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                result.add(mapGroupRow(rs));
            }

        } catch (SQLException e) {
            LOGGER.error("Failed to list global bookmark groups from H2", e);
        }

        return result;
    }

    /**
     * Lists bookmark groups, optionally filtered by project name.
     *
     * @param projectName the project name to filter by, or null/blank for all
     * @return list of BookmarkGroupRow instances
     */
    public List<BookmarkGroupRow> listGroups(String projectName) {
        List<BookmarkGroupRow> result = new ArrayList<>();

        String sql;
        if (projectName != null && !projectName.isBlank()) {
            sql = "SELECT * FROM bookmark_groups WHERE project_name = ? ORDER BY name ASC";
        } else {
            sql = "SELECT * FROM bookmark_groups ORDER BY name ASC";
        }

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            if (projectName != null && !projectName.isBlank()) {
                stmt.setString(1, projectName);
            }

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    result.add(mapGroupRow(rs));
                }
            }

        } catch (SQLException e) {
            LOGGER.error("Failed to list bookmark groups from H2", e);
        }

        return result;
    }

    /**
     * Inserts a new bookmark group.
     *
     * @param row the group to insert
     */
    public void insertGroup(BookmarkGroupRow row) {
        if (row.id == null || row.id.isBlank()) {
            row.id = UUID.randomUUID().toString();
        }

        String sql = "INSERT INTO bookmark_groups (id, name, project_name) VALUES (?, ?, ?)";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, row.id);
            stmt.setString(2, row.name);
            stmt.setString(3, row.projectName);

            stmt.executeUpdate();

        } catch (SQLException e) {
            throw new IllegalStateException("Failed to insert bookmark group into H2", e);
        }
    }

    /**
     * Deletes a bookmark group by its id. Associated bookmarks are removed via FK CASCADE.
     *
     * @param groupId the group's id
     */
    public void deleteGroup(String groupId) {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement("DELETE FROM bookmark_groups WHERE id = ?")) {

            stmt.setString(1, groupId);
            stmt.executeUpdate();

        } catch (SQLException e) {
            throw new IllegalStateException("Failed to delete bookmark group from H2", e);
        }
    }

    private MetricBookmarkRow mapBookmarkRow(ResultSet rs) throws SQLException {
        MetricBookmarkRow r = new MetricBookmarkRow();
        r.id = rs.getString("id");
        r.name = rs.getString("name");
        r.bookmarkGroupId = rs.getString("bookmark_group_id");
        r.commandProtocol = rs.getString("command_protocol");
        r.projectName = rs.getString("project_name");
        return r;
    }

    private MetricQueryBookmarkRow mapQueryBookmarkRow(ResultSet rs) throws SQLException {
        MetricQueryBookmarkRow r = new MetricQueryBookmarkRow();
        r.id = rs.getString("id");
        r.name = rs.getString("name");
        r.bookmarkGroupId = rs.getString("bookmark_group_id");
        r.projectName = rs.getString("project_name");
        r.queryData = rs.getString("query_data");
        return r;
    }

    private BookmarkGroupRow mapGroupRow(ResultSet rs) throws SQLException {
        BookmarkGroupRow r = new BookmarkGroupRow();
        r.id = rs.getString("id");
        r.name = rs.getString("name");
        r.projectName = rs.getString("project_name");
        return r;
    }

}

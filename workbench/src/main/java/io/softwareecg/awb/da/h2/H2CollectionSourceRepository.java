package io.softwareecg.awb.da.h2;

import io.softwareecg.awb.da.api.collection.CollectionSourceEvent;
import io.softwareecg.awb.da.api.collection.CollectionSourceRow;
import io.softwareecg.awb.da.api.collection.CollectionSourceStore;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * H2 storage of the data sources, tables {@code collection_sources} and
 * {@code collection_source_events}.
 */
@Singleton
public class H2CollectionSourceRepository implements CollectionSourceStore {

    /**
     * How many history entries are kept per source.
     */
    private static final int KEPT_EVENTS = 200;

    private static final String COLUMNS =
            "id, project_name, source_type, execution_mode, endpoint, config, secret_ref, series_namespace, "
                    + "enabled, created_date";

    private final H2DataSource dataSource;

    @Inject
    public H2CollectionSourceRepository(H2DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public List<CollectionSourceRow> listAll() {
        return query("SELECT " + COLUMNS + " FROM collection_sources ORDER BY project_name, created_date, id", null);
    }

    @Override
    public List<CollectionSourceRow> listByProject(String projectName) {
        return query("SELECT " + COLUMNS + " FROM collection_sources WHERE project_name = ? ORDER BY created_date, id",
                projectName);
    }

    @Override
    public CollectionSourceRow find(String id) {
        List<CollectionSourceRow> rows = query("SELECT " + COLUMNS + " FROM collection_sources WHERE id = ?", id);
        return rows.isEmpty() ? null : rows.get(0);
    }

    @Override
    public CollectionSourceRow insert(CollectionSourceRow row) {
        if (row.id == null || row.id.isBlank()) {
            row.id = UUID.randomUUID().toString();
        }
        String sql = "INSERT INTO collection_sources (id, project_name, source_type, execution_mode, endpoint, "
                + "config, secret_ref, enabled, series_namespace) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = dataSource.getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, row.id);
            stmt.setString(2, row.projectName);
            stmt.setString(3, row.sourceType);
            stmt.setString(4, row.executionMode);
            stmt.setString(5, row.endpoint);
            stmt.setString(6, row.config);
            stmt.setString(7, row.secretRef);
            stmt.setBoolean(8, row.enabled);
            stmt.setString(9, row.seriesNamespace);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to store the data source of '" + row.projectName + "' in H2", e);
        }
        return row;
    }

    @Override
    public boolean update(CollectionSourceRow row) {
        String sql = "UPDATE collection_sources SET endpoint = ?, config = ?, secret_ref = ?, enabled = ? WHERE id = ?";
        try (Connection conn = dataSource.getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, row.endpoint);
            stmt.setString(2, row.config);
            stmt.setString(3, row.secretRef);
            stmt.setBoolean(4, row.enabled);
            stmt.setString(5, row.id);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to update data source " + row.id + " in H2", e);
        }
    }

    @Override
    public void delete(String id) {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement("DELETE FROM collection_sources WHERE id = ?")) {
            stmt.setString(1, id);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to delete data source " + id + " from H2", e);
        }
    }

    @Override
    public void addEvent(String sourceId, String state, String message) {
        try (Connection conn = dataSource.getConnection()) {
            try (PreparedStatement insert = conn.prepareStatement(
                    "INSERT INTO collection_source_events (source_id, state, message) VALUES (?, ?, ?)")) {
                insert.setString(1, sourceId);
                insert.setString(2, state);
                insert.setString(3, message == null ? null : message.substring(0, Math.min(message.length(), 2048)));
                insert.executeUpdate();
            }
            try (PreparedStatement trim = conn.prepareStatement(
                    "DELETE FROM collection_source_events WHERE source_id = ? AND id NOT IN "
                            + "(SELECT id FROM collection_source_events WHERE source_id = ? ORDER BY id DESC LIMIT ?)")) {
                trim.setString(1, sourceId);
                trim.setString(2, sourceId);
                trim.setInt(3, KEPT_EVENTS);
                trim.executeUpdate();
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to record the state of data source " + sourceId + " in H2", e);
        }
    }

    @Override
    public List<CollectionSourceEvent> listEvents(String sourceId, int limit) {
        List<CollectionSourceEvent> events = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "SELECT event_time, state, message FROM collection_source_events WHERE source_id = ? "
                             + "ORDER BY id DESC LIMIT ?")) {
            stmt.setString(1, sourceId);
            stmt.setInt(2, limit);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Timestamp time = rs.getTimestamp(1);
                    events.add(new CollectionSourceEvent(time == null ? null : time.toInstant(),
                            rs.getString(2), rs.getString(3)));
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to read the history of data source " + sourceId + " from H2", e);
        }
        return events;
    }

    private List<CollectionSourceRow> query(String sql, String parameter) {
        List<CollectionSourceRow> rows = new ArrayList<>();
        try (Connection conn = dataSource.getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            if (parameter != null) {
                stmt.setString(1, parameter);
            }
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    CollectionSourceRow row = new CollectionSourceRow();
                    row.id = rs.getString("id");
                    row.projectName = rs.getString("project_name");
                    row.sourceType = rs.getString("source_type");
                    row.executionMode = rs.getString("execution_mode");
                    row.endpoint = rs.getString("endpoint");
                    row.config = rs.getString("config");
                    row.secretRef = rs.getString("secret_ref");
                    row.seriesNamespace = rs.getString("series_namespace");
                    row.enabled = rs.getBoolean("enabled");
                    Timestamp created = rs.getTimestamp("created_date");
                    row.createdDate = created == null ? null : created.toInstant();
                    rows.add(row);
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to read the data sources from H2", e);
        }
        return rows;
    }
}

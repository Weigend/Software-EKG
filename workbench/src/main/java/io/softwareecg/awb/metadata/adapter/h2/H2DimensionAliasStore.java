package io.softwareecg.awb.metadata.adapter.h2;

import io.softwareecg.awb.metadata.model.DimensionAliasRow;
import io.softwareecg.awb.metadata.port.DimensionAliasStore;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

/**
 * JDBC-based repository for project dimension aliases stored in H2.
 * Provides CRUD operations for the project_dimension_aliases table.
 *
 */
@Singleton
public class H2DimensionAliasStore implements DimensionAliasStore {

    private static final Logger LOGGER = LoggerFactory.getLogger(H2DimensionAliasStore.class);

    private final H2MetadataDataSource dataSource;

    @Inject
    public H2DimensionAliasStore(H2MetadataDataSource dataSource) {
        this.dataSource = dataSource;
    }

    /**
     * Finds a dimension alias row by project name.
     *
     * @param projectName the project name to look up
     * @return the matching DimensionAliasRow, or null if not found
     */
    public DimensionAliasRow findByProject(String projectName) {
        String sql = "SELECT * FROM project_dimension_aliases WHERE project_name = ?";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, projectName);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }

        } catch (SQLException e) {
            LOGGER.error("Failed to find dimension aliases for project '{}' from H2", projectName, e);
        }

        return null;
    }

    /**
     * Inserts or updates a dimension alias row. Uses delete + insert as upsert strategy.
     *
     * @param row the dimension alias row to persist
     */
    public void insertOrUpdate(DimensionAliasRow row) {
        if (row.id == null || row.id.isBlank()) {
            row.id = UUID.randomUUID().toString();
        }

        try (Connection conn = dataSource.getConnection()) {
            // delete existing entry for the project
            try (PreparedStatement del = conn.prepareStatement(
                    "DELETE FROM project_dimension_aliases WHERE project_name = ?")) {
                del.setString(1, row.projectName);
                del.executeUpdate();
            }

            // insert new entry
            String sql = "INSERT INTO project_dimension_aliases (id, project_name, alias_host_group, " +
                    "alias_host, alias_namespace, alias_service, alias_pod, alias_container, " +
                    "alias_measurement, alias_process, alias_metric_group, alias_metric_name) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

            try (PreparedStatement ins = conn.prepareStatement(sql)) {
                ins.setString(1, row.id);
                ins.setString(2, row.projectName);
                ins.setString(3, row.aliasHostGroup);
                ins.setString(4, row.aliasHost);
                ins.setString(5, row.aliasNamespace);
                ins.setString(6, row.aliasService);
                ins.setString(7, row.aliasPod);
                ins.setString(8, row.aliasContainer);
                ins.setString(9, row.aliasMeasurement);
                ins.setString(10, row.aliasProcess);
                ins.setString(11, row.aliasMetricGroup);
                ins.setString(12, row.aliasMetricName);
                ins.executeUpdate();
            }

        } catch (SQLException e) {
            throw new IllegalStateException("Failed to insert/update dimension aliases in H2", e);
        }
    }

    /**
     * Deletes a dimension alias row by project name.
     *
     * @param projectName the project name
     */
    public void deleteByProject(String projectName) {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "DELETE FROM project_dimension_aliases WHERE project_name = ?")) {

            stmt.setString(1, projectName);
            stmt.executeUpdate();

        } catch (SQLException e) {
            throw new IllegalStateException("Failed to delete dimension aliases from H2", e);
        }
    }

    private DimensionAliasRow mapRow(ResultSet rs) throws SQLException {
        DimensionAliasRow r = new DimensionAliasRow();
        r.id = rs.getString("id");
        r.projectName = rs.getString("project_name");
        r.aliasHostGroup = rs.getString("alias_host_group");
        r.aliasHost = rs.getString("alias_host");
        r.aliasNamespace = rs.getString("alias_namespace");
        r.aliasService = rs.getString("alias_service");
        r.aliasPod = rs.getString("alias_pod");
        r.aliasContainer = rs.getString("alias_container");
        r.aliasMeasurement = rs.getString("alias_measurement");
        r.aliasProcess = rs.getString("alias_process");
        r.aliasMetricGroup = rs.getString("alias_metric_group");
        r.aliasMetricName = rs.getString("alias_metric_name");
        return r;
    }

}

package io.softwareecg.awb.repository.adapter.clickhouse;

import io.softwareecg.awb.repository.port.TimeSeriesAdmin;
import io.softwareecg.awb.repository.port.CollectionProperties;
import io.softwareecg.awb.repository.port.RepositoryException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.*;
import java.util.*;

/**
 * An implementation of {@link TimeSeriesAdmin} for ClickHouse.
 *
 * <p>Maps collection operations to ClickHouse DDL:
 * <ul>
 *   <li>{@code create} → CREATE TABLE ... ON CLUSTER with ReplicatedMergeTree engine</li>
 *   <li>{@code delete} → DROP TABLE ... ON CLUSTER</li>
 *   <li>{@code listCollections} → system.tables query</li>
 *   <li>{@code setAlias/deleteAlias} → RENAME TABLE (ClickHouse has no native alias concept)</li>
 * </ul>
 *
 * <p>The table schema is optimized for EKG time series storage with:
 * <ul>
 *   <li>Array columns for time series data with DoubleDelta/Gorilla codecs</li>
 *   <li>LowCardinality for dimension strings</li>
 *   <li>Monthly partitioning on ts_start</li>
 * </ul>
 *
 * <p>Each operation obtains a connection from the pool and returns it after completion.</p>
 *
 */
public class ClickHouseRepositoryAdminClient implements TimeSeriesAdmin {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClickHouseRepositoryAdminClient.class);

    private final DataSource dataSource;
    private final String clusterName;
    private final String database;

    ClickHouseRepositoryAdminClient(DataSource dataSource, String clusterName, String database) {
        this.dataSource = Objects.requireNonNull(dataSource, "DataSource must not be null.");
        this.clusterName = clusterName;
        this.database = database;
    }

    @Override
    public void create(CollectionProperties collectionProperties) throws RepositoryException {
        String tableName = collectionProperties.getCollectionName();

        int numRetries = 3;
        int secondsBetweenRetries = 5;

        while (true) {
            --numRetries;
            try {
                createTable(tableName, collectionProperties);
                LOGGER.info("Successfully created ClickHouse table '{}'", tableName);
                return;
            } catch (SQLException e) {
                if (numRetries <= 0) {
                    throw new RepositoryException("Failed to create ClickHouse table " + tableName, e);
                }
                LOGGER.warn("Could not create table {}. Retrying in {} seconds. {} retries left.",
                        tableName, secondsBetweenRetries, numRetries);
                try {
                    Thread.sleep(secondsBetweenRetries * 1000L);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }

    @Override
    public void delete(String collectionName) throws RepositoryException {
        try {
            String sql = "DROP TABLE IF EXISTS " + qualifiedTableName(collectionName);
            if (clusterName != null) {
                sql += " ON CLUSTER " + quoteIdentifier(clusterName);
            }
            LOGGER.info("Dropping ClickHouse table: {}", sql);
            executeStatement(sql);
        } catch (SQLException e) {
            throw new RepositoryException("Failed to delete ClickHouse table " + collectionName, e);
        }
    }

    @Override
    public void setAlias(String alias, String collectionName) throws RepositoryException {
        // ClickHouse doesn't have native table aliases like Solr.
        // We use a view as an alias.
        try {
            String viewSql = "CREATE OR REPLACE VIEW " + qualifiedTableName(alias);
            if (clusterName != null) {
                viewSql += " ON CLUSTER " + quoteIdentifier(clusterName);
            }
            viewSql += " AS SELECT * FROM " + qualifiedTableName(collectionName);

            LOGGER.info("Creating ClickHouse alias view: {} -> {}", alias, collectionName);
            executeStatement(viewSql);
        } catch (SQLException e) {
            throw new RepositoryException("Failed to set alias " + alias + " for table " + collectionName, e);
        }
    }

    @Override
    public void deleteAlias(String alias) throws RepositoryException {
        try {
            String sql = "DROP VIEW IF EXISTS " + qualifiedTableName(alias);
            if (clusterName != null) {
                sql += " ON CLUSTER " + quoteIdentifier(clusterName);
            }
            executeStatement(sql);
        } catch (SQLException e) {
            throw new RepositoryException("Failed to delete alias " + alias, e);
        }
    }

    @Override
    public void rebalanceShardLeaders(String collectionName) throws RepositoryException {
        // ClickHouse ReplicatedMergeTree handles shard leadership automatically via ZooKeeper/ClickHouse Keeper.
        LOGGER.info("rebalanceShardLeaders() called for {} - no-op for ClickHouse (automatic via Keeper)", collectionName);
    }

    @Override
    public String getCollectionNameForAlias(String alias) throws RepositoryException {
        try (Connection conn = dataSource.getConnection()) {
            String sql = "SELECT as_select FROM system.tables WHERE database = ? AND name = ? AND engine = 'View'";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, database != null ? database : "default");
                ps.setString(2, alias);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        String asSelect = rs.getString("as_select");
                        if (asSelect != null && asSelect.contains("FROM")) {
                            String afterFrom = asSelect.substring(asSelect.lastIndexOf("FROM") + 5).trim();
                            return afterFrom.split("\\s")[0].replace("`", "");
                        }
                    }
                }
            }
            return null;
        } catch (SQLException e) {
            throw new RepositoryException("Failed to resolve alias " + alias, e);
        }
    }

    @Override
    public Collection<CollectionProperties> listCollections() throws RepositoryException {
        try (Connection conn = dataSource.getConnection()) {
            String db = database != null ? database : "default";
            String sql = "SELECT name, engine, total_rows, partition_key FROM system.tables "
                    + "WHERE database = ? AND engine NOT IN ('View', 'MaterializedView')";

            List<CollectionProperties> collections = new ArrayList<>();

            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, db);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        CollectionProperties props = new CollectionProperties();
                        props.setCollectionName(rs.getString("name"));
                        props.setConfigIdentifier(rs.getString("engine"));
                        props.setAmountOfShards(1);
                        props.setAmountOfReplicas(1);
                        collections.add(props);
                    }
                }
            }

            return collections;
        } catch (SQLException e) {
            throw new RepositoryException("Failed to list ClickHouse tables", e);
        }
    }

    @Override
    public void close() {
        // No-op: connections are obtained per-operation from the pool
    }

    // ==========================================================================================================
    //  DDL for table creation
    // ==========================================================================================================

    private void createTable(String tableName, CollectionProperties props) throws SQLException {
        String onCluster = clusterName != null ? " ON CLUSTER " + quoteIdentifier(clusterName) : "";

        // Determine the engine based on cluster configuration
        String engine;
        if (clusterName != null) {
            engine = "ReplicatedMergeTree("
                    + "'/clickhouse/tables/{shard}/" + database + "/" + tableName + "', "
                    + "'{replica}')";
        } else {
            engine = "MergeTree()";
        }

        String sql = "CREATE TABLE IF NOT EXISTS " + qualifiedTableName(tableName) + onCluster + " (\n"
                // Common fields
                + "    id String,\n"
                + "    type LowCardinality(String) DEFAULT '',\n"
                + "    project_name LowCardinality(Nullable(String)),\n"
                + "\n"
                // Classic dimensions
                + "    ts_hostGroup LowCardinality(Nullable(String)),\n"
                + "    ts_host LowCardinality(Nullable(String)),\n"
                + "\n"
                // Cloud native dimensions
                + "    ts_namespace LowCardinality(Nullable(String)),\n"
                + "    ts_service LowCardinality(Nullable(String)),\n"
                + "    ts_pod LowCardinality(Nullable(String)),\n"
                + "    ts_container LowCardinality(Nullable(String)),\n"
                + "\n"
                // Generic dimensions
                + "    ts_processGroup LowCardinality(Nullable(String)),\n"
                + "    ts_process LowCardinality(Nullable(String)),\n"
                + "    ts_measurement LowCardinality(Nullable(String)),\n"
                + "    ts_metricGroup LowCardinality(Nullable(String)),\n"
                + "    ts_metricName LowCardinality(Nullable(String)),\n"
                + "\n"
                // Time series metadata
                + "    ts_start DateTime64(3) DEFAULT now64(3),\n"
                + "    ts_end DateTime64(3) DEFAULT now64(3),\n"
                + "    ts_importDate DateTime64(3) DEFAULT now64(3),\n"
                + "    ts_remoteSeriesKey Nullable(String),\n"
                + "    ts_ag LowCardinality(Nullable(String)),\n"
                + "\n"
                // Time series data: arrays with specialized codecs for optimal compression
                + "    ts_timestamps Array(Int64) CODEC(DoubleDelta, ZSTD(1)),\n"
                + "    ts_values Array(Float64) CODEC(Gorilla, ZSTD(1)),\n"
                + "    ts_data_amountValues UInt32 DEFAULT 0,\n"
                + "\n"
                // Control fields
                + "    ts_isMerged UInt8 DEFAULT 0,\n"
                + "    ts_groupKeyHash Nullable(Int32),\n"
                + "\n"
                // Repository metadata fields
                + "    repository_accordingProject Nullable(String),\n"
                + "    repository_name Nullable(String),\n"
                + "    repository_dbType Nullable(String),\n"
                + "    repository_url Nullable(String),\n"
                + "    repository_dbIndexName Nullable(String),\n"
                + "    repository_isSource Nullable(String),\n"
                + "    repository_authType Nullable(String),\n"
                + "    repository_user Nullable(String),\n"
                + "    repository_pass Nullable(String),\n"
                + "    repository_ssoToken Nullable(String),\n"
                + "    repository_oauthServerUrl Nullable(String),\n"
                + "\n"
                // Bookmark fields
                + "    bookmarkName Nullable(String),\n"
                + "    bookmarkGroupName Nullable(String),\n"
                + "    bookmarkGroupId Nullable(String),\n"
                + "    bookmarkDescription Nullable(String),\n"
                + "    bookmarkProtocol Nullable(String),\n"
                + "    bookmarkTimeZone Nullable(String),\n"
                + "\n"
                // Project fields
                + "    project_description Nullable(String),\n"
                + "    project_useSplitSource Nullable(String),\n"
                + "    project_flavor Nullable(String),\n"
                + "    project_cn_type Nullable(String),\n"
                + "    project_importerId Nullable(String),\n"
                + "    project_timeseries_type Nullable(String),\n"
                + "    project_dimension_alias_hostGroup Nullable(String),\n"
                + "    project_dimension_alias_host Nullable(String),\n"
                + "    project_dimension_alias_namespace Nullable(String),\n"
                + "    project_dimension_alias_service Nullable(String),\n"
                + "    project_dimension_alias_pod Nullable(String),\n"
                + "    project_dimension_alias_container Nullable(String),\n"
                + "    project_dimension_alias_measurement Nullable(String),\n"
                + "    project_dimension_alias_process Nullable(String),\n"
                + "    project_dimension_alias_metricGroup Nullable(String),\n"
                + "    project_dimension_alias_metricName Nullable(String)\n"
                + ")\n"
                + "ENGINE = " + engine + "\n"
                + "PARTITION BY toYYYYMM(ts_start)\n"
                + "ORDER BY (id)\n"
                + "SETTINGS index_granularity = 8192";

        LOGGER.info("Creating ClickHouse table: {}", tableName);
        LOGGER.debug("DDL: {}", sql);

        executeStatement(sql);
    }

    private String qualifiedTableName(String tableName) {
        if (database != null && !database.isEmpty()) {
            return quoteIdentifier(database) + "." + quoteIdentifier(tableName);
        }
        return quoteIdentifier(tableName);
    }

    private String quoteIdentifier(String identifier) {
        if (identifier.matches("[a-zA-Z_][a-zA-Z0-9_]*")) {
            return identifier;
        }
        return "`" + identifier.replace("`", "``") + "`";
    }

    private void executeStatement(String sql) throws SQLException {
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
        }
    }
}

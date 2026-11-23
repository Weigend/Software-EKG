package io.softwareecg.awb.repository.adapter.clickhouse;

import java.util.Collection;

/**
 * Connection parameters for a ClickHouse cluster.
 *
 */
public interface ClickHouseConnectionInfo {

    /**
     * Returns the JDBC URL for the ClickHouse connection.
     * Example: {@code jdbc:ch://host1:8123/database}
     *
     * @return the JDBC URL
     */
    String getJdbcUrl();

    /**
     * Returns the ClickHouse hosts as a collection.
     *
     * @return the host addresses
     */
    Collection<String> getHosts();

    /**
     * Returns the database name.
     *
     * @return the database name
     */
    String getDatabase();

    /**
     * Returns the optional cluster name for distributed DDL operations.
     * If set, CREATE/DROP TABLE statements will use ON CLUSTER.
     *
     * @return the cluster name, or null if not clustered
     */
    String getClusterName();

    /**
     * Returns the username for authentication.
     *
     * @return the username, or null
     */
    String getUsername();

    /**
     * Returns the password for authentication.
     *
     * @return the password, or null
     */
    String getPassword();
}

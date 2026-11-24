package io.softwareecg.awb.repository.adapter.clickhouse;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import io.softwareecg.awb.repository.persistence.TimeSeriesRepository;
import io.softwareecg.awb.repository.connection.TimeSeriesRepositoryProvider;
import io.softwareecg.awb.repository.connection.RepositoryTypeDescriptor;
import io.softwareecg.awb.repository.persistence.TimeSeriesAdmin;
import io.softwareecg.awb.repository.persistence.TimeSeriesAdminProvider;
import jakarta.annotation.PreDestroy;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Locale;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Provider that creates and manages {@link ClickHouseRepositoryClient}s.
 *
 * <p>Analogous to {@code SolrRepositoryClientProvider}, this provider manages
 * a HikariCP connection pool to the ClickHouse cluster. It implements both
 * {@link TimeSeriesAdminProvider} for collection-scoped access and
 * {@link TimeSeriesRepositoryProvider} for creating repository clients.</p>
 *
 * <p>The ClickHouse JDBC driver supports multi-host URLs for round-robin
 * load balancing across cluster nodes:
 * {@code jdbc:ch://host1:8123,host2:8123,host3:8123/database}</p>
 *
 * <p>Connection parameters are passed either via constructor (programmatic use)
 * or via {@link ClickHouseConnectionInfo} (CDI-managed use).</p>
 *
 */
@Singleton
@ClickHouse
public class ClickHouseRepositoryClientProvider implements TimeSeriesAdminProvider, TimeSeriesRepositoryProvider {

    private static final Pattern JDBC_URL = Pattern.compile(
            "^jdbc:c(h|lickhouse)://[a-zA-Z0-9._\\-]+(:[1-9][0-9]{1,4})?"
                    + "(,[a-zA-Z0-9._\\-]+(:[1-9][0-9]{1,4})?)*"
                    + "(/[a-zA-Z0-9_]*)?(\\?.*)?$", Pattern.CASE_INSENSITIVE);

    @Override
    public List<RepositoryTypeDescriptor> repositoryTypes() {
        return List.of(new RepositoryTypeDescriptor("CLICKHOUSE_CLOUD", "ClickHouse Cloud",
                "icons/repository-db-type-clickhouse.svg", true, "ClickHouse JDBC URL:",
                "jdbc:clickhouse://node1:8123/database", "Table Name:", "Optional table name", false,
                value -> JDBC_URL.matcher(value).matches()));
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(ClickHouseRepositoryClientProvider.class);

    private static final int DEFAULT_MAX_POOL_SIZE = 10;
    private static final long DEFAULT_CONNECTION_TIMEOUT_MS = 30_000;
    private static final long DEFAULT_IDLE_TIMEOUT_MS = 600_000;
    private static final long DEFAULT_MAX_LIFETIME_MS = 1_800_000;
    private static final int DEFAULT_HTTP_PORT = 8123;
    /** Sehr kurzer Timeout: damit ein nicht erreichbarer Server den Ping-Thread nicht
     *  blockiert. ClickHouse antwortet auf TCP-SYN typisch innerhalb weniger ms. */
    private static final int REACHABILITY_PROBE_TIMEOUT_MS = 1_000;

    private ClickHouseConnectionInfo connectionInfo;
    private String jdbcUrl;
    private String database;
    private String clusterName;
    private String username;
    private String password;

    private volatile HikariDataSource dataSource;

    /**
     * Default constructor for DI-managed usage. Marked {@code @Inject} so Avaje
     * picks this constructor among the three available; CDI is happy with any.
     */
    @Inject
    public ClickHouseRepositoryClientProvider() {
    }

    /**
     * Constructs a provider with explicit connection info.
     *
     * @param connectionInfo ClickHouse connection parameters
     * @param username       the username for authentication
     * @param password       the password for authentication
     */
    public ClickHouseRepositoryClientProvider(ClickHouseConnectionInfo connectionInfo,
                                              String username, String password) {
        this.connectionInfo = connectionInfo;
        this.jdbcUrl = connectionInfo.getJdbcUrl();
        this.database = connectionInfo.getDatabase();
        this.clusterName = connectionInfo.getClusterName();
        this.username = username;
        this.password = password;
    }

    /**
     * Constructs a provider with a direct JDBC URL.
     *
     * @param jdbcUrl  the JDBC URL (e.g., jdbc:ch://host1:8123,host2:8123/database)
     * @param username the username for authentication
     * @param password the password for authentication
     */
    public ClickHouseRepositoryClientProvider(String jdbcUrl, String username, String password) {
        this.jdbcUrl = jdbcUrl;
        this.username = username;
        this.password = password;
        this.database = extractDatabaseFromUrl(jdbcUrl);
    }

    // ==========================================================================================================
    //  TimeSeriesRepositoryProvider implementation
    // ==========================================================================================================

    @Override
    public TimeSeriesRepository createRepositoryClient(String connectionUrl, String userName, String password) {
        String url = toJdbcUrl(connectionUrl);
        DataSource ds = createPooledDataSource(url, userName, password);
        return new ClickHouseRepositoryClient(ds, url, null);
    }

    @Override
    public TimeSeriesRepository createRepositoryClient(String clusterManagerUrl, String repositoryName,
                                                   String userName, String password) {
        String url = toJdbcUrl(clusterManagerUrl);
        DataSource ds = createPooledDataSource(url, userName, password);
        return new ClickHouseRepositoryClient(ds, url, repositoryName);
    }

    // ==========================================================================================================
    //  TimeSeriesAdminProvider implementation
    // ==========================================================================================================

    @Override
    public TimeSeriesRepository getClient(String collection) {
        return new ClickHouseRepositoryClient(getOrCreateDataSource(), getJdbcUrl(), collection);
    }

    @Override
    public TimeSeriesAdmin getAdminClient() {
        String db = database != null ? database : "default";
        return new ClickHouseRepositoryAdminClient(getOrCreateDataSource(), clusterName, db);
    }

    // ==========================================================================================================
    //  Lifecycle management
    // ==========================================================================================================

    @PreDestroy
    public void shutdown() {
        if (dataSource != null && !dataSource.isClosed()) {
            LOGGER.info("Shutting down ClickHouse HikariCP connection pool.");
            dataSource.close();
        }
    }

    // ==========================================================================================================
    //  Internal connection pool management
    // ==========================================================================================================

    private DataSource getOrCreateDataSource() {
        if (dataSource == null) {
            synchronized (this) {
                if (dataSource == null) {
                    String url = getJdbcUrl();
                    ensureDatabaseExists(url, username, password);
                    dataSource = buildHikariDataSource(url, username, password);
                    LOGGER.info("ClickHouse HikariCP pool created for URL: {}", url);
                }
            }
        }
        return dataSource;
    }

    private String getJdbcUrl() {
        if (jdbcUrl != null) {
            return jdbcUrl;
        }
        if (connectionInfo != null) {
            return connectionInfo.getJdbcUrl();
        }
        throw new IllegalStateException("No ClickHouse connection URL configured");
    }

    /**
     * Creates a new HikariCP DataSource for ad-hoc connections (e.g., from the factory API).
     * Ensures the target database exists before creating the connection pool.
     */
    private static HikariDataSource createPooledDataSource(String url, String user, String pass) {
        ensureDatabaseExists(url, user, pass);
        return buildHikariDataSource(url, user, pass);
    }

    /**
     * Connects to ClickHouse without a database and creates the target database if it does not exist.
     * This is necessary because HikariCP connections will fail if the database in the JDBC URL
     * has not been created yet (e.g., after a fresh ClickHouse installation).
     *
     * <p>Macht zuerst einen leichtgewichtigen TCP-Pre-Check (Socket-Connect mit kurzem
     * Timeout). Erst wenn der Port erreichbar ist, wird der JDBC-Driver bemueht. Dadurch
     * sehen Driver und HikariPool im Offline-Fall keinen fehlgeschlagenen Connect-Versuch
     * und produzieren keine Stacktraces.</p>
     *
     * <p>Loggt nur Zustandswechsel pro Datenbank: einmal "ensured" bei Erfolg, einmal
     * "not reachable" bei Wechsel auf offline.</p>
     */
    private static void ensureDatabaseExists(String jdbcUrl, String user, String pass) {
        String database = extractDatabaseFromJdbcUrl(jdbcUrl);
        if (database == null || database.isEmpty()) {
            return;
        }

        String baseUrl = removeDatabaseFromUrl(jdbcUrl);

        if (!isHostReachable(jdbcUrl)) {
            if (DATABASE_SETUP_STATE.put(database, Boolean.FALSE) != Boolean.FALSE) {
                LOGGER.warn("ClickHouse host not reachable; skipping database setup for '{}'.", database);
            }
            return;
        }

        try (Connection conn = createRawConnection(baseUrl, user, pass);
             Statement stmt = conn.createStatement()) {
            stmt.execute("CREATE DATABASE IF NOT EXISTS " + database);
            if (DATABASE_SETUP_STATE.put(database, Boolean.TRUE) != Boolean.TRUE) {
                LOGGER.info("Ensured ClickHouse database '{}' exists.", database);
            }
        } catch (SQLException e) {
            if (DATABASE_SETUP_STATE.put(database, Boolean.FALSE) != Boolean.FALSE) {
                LOGGER.warn("Could not ensure database '{}' exists: {}", database, rootCauseMessage(e));
            }
        }
    }

    /**
     * Leichtgewichtiger TCP-Erreichbarkeits-Check: parst Host und Port aus der JDBC-URL
     * und versucht einen Socket-Connect mit kurzem Timeout. Loggt nichts, wirft nichts -
     * komplettes Schweigen ist hier wertvoll, weil das Ergebnis vom State-Tracking-Layer
     * weiterverarbeitet wird.
     *
     * Multi-Host-URLs (jdbc:ch://h1:8123,h2:8123/db) werden mit dem ersten Host gepruft -
     * fuer den UI-Heartbeat reicht das.
     */
    static boolean isHostReachable(String jdbcUrl) {
        HostPort hostPort = extractFirstHostPort(jdbcUrl);
        if (hostPort == null) {
            return false;
        }
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(hostPort.host, hostPort.port), REACHABILITY_PROBE_TIMEOUT_MS);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private static HostPort extractFirstHostPort(String jdbcUrl) {
        if (jdbcUrl == null) {
            return null;
        }
        String url = jdbcUrl;
        String[] schemes = {"jdbc:ch://", "jdbc:clickhouse://"};
        for (String s : schemes) {
            if (url.startsWith(s)) {
                url = url.substring(s.length());
                break;
            }
        }
        // Cut off database/path and query string
        int slash = url.indexOf('/');
        if (slash >= 0) url = url.substring(0, slash);
        int qmark = url.indexOf('?');
        if (qmark >= 0) url = url.substring(0, qmark);
        // First host of a comma-separated multi-host list
        int comma = url.indexOf(',');
        String first = comma >= 0 ? url.substring(0, comma) : url;
        // Optional credentials prefix (user:pass@host:port) — strip
        int at = first.indexOf('@');
        if (at >= 0) first = first.substring(at + 1);
        int colon = first.lastIndexOf(':');
        String host;
        int port = DEFAULT_HTTP_PORT;
        if (colon > 0) {
            host = first.substring(0, colon);
            try {
                port = Integer.parseInt(first.substring(colon + 1));
            } catch (NumberFormatException ignore) {
                // fall back to default port
            }
        } else {
            host = first;
        }
        if (host.isEmpty()) {
            return null;
        }
        return new HostPort(host, port);
    }

    private static final class HostPort {
        final String host;
        final int port;
        HostPort(String host, int port) { this.host = host; this.port = port; }
    }

    /** Tracked Setup-Zustand pro Datenbank-Name (true = exists, false = nicht erreichbar) -
     *  damit wiederholte Aufrufe bei stabilem Zustand nicht erneut loggen. */
    private static final java.util.concurrent.ConcurrentHashMap<String, Boolean> DATABASE_SETUP_STATE
            = new java.util.concurrent.ConcurrentHashMap<>();

    private static String rootCauseMessage(Throwable t) {
        Throwable cur = t;
        while (cur.getCause() != null && cur.getCause() != cur) {
            cur = cur.getCause();
        }
        String msg = cur.getMessage();
        return cur.getClass().getSimpleName() + (msg != null ? ": " + msg : "");
    }

    private static Connection createRawConnection(String url, String user, String pass) throws SQLException {
        java.util.Properties props = new java.util.Properties();
        if (StringUtils.isNotBlank(user)) {
            props.setProperty("user", user);
        }
        if (StringUtils.isNotBlank(pass)) {
            props.setProperty("password", pass);
        }
        return DriverManager.getConnection(url, props);
    }

    /**
     * Removes the database path segment from a JDBC URL.
     * E.g. {@code jdbc:ch://host:8123/mydb} becomes {@code jdbc:ch://host:8123}
     */
    private static String removeDatabaseFromUrl(String url) {
        int queryStart = url.indexOf('?');
        String base = queryStart > 0 ? url.substring(0, queryStart) : url;
        String query = queryStart > 0 ? url.substring(queryStart) : "";

        int lastSlash = base.lastIndexOf('/');
        // Make sure we don't cut into the protocol part (jdbc:ch://)
        String protocolEnd = "://";
        int protocolSlash = base.indexOf(protocolEnd);
        if (protocolSlash >= 0 && lastSlash > protocolSlash + protocolEnd.length()) {
            base = base.substring(0, lastSlash);
        }

        return base + query;
    }

    private static String extractDatabaseFromJdbcUrl(String url) {
        if (url == null) {
            return null;
        }
        int queryStart = url.indexOf('?');
        String base = queryStart > 0 ? url.substring(0, queryStart) : url;
        int lastSlash = base.lastIndexOf('/');
        String protocolEnd = "://";
        int protocolSlash = base.indexOf(protocolEnd);
        if (protocolSlash >= 0 && lastSlash > protocolSlash + protocolEnd.length()) {
            return base.substring(lastSlash + 1);
        }
        return null;
    }

    private static HikariDataSource buildHikariDataSource(String url, String user, String pass) {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(url);
        config.setPoolName("ClickHouse-EKG");
        config.setMaximumPoolSize(DEFAULT_MAX_POOL_SIZE);
        config.setConnectionTimeout(DEFAULT_CONNECTION_TIMEOUT_MS);
        config.setIdleTimeout(DEFAULT_IDLE_TIMEOUT_MS);
        config.setMaxLifetime(DEFAULT_MAX_LIFETIME_MS);
        // Pool-Erzeugung darf nicht scheitern, wenn der Server gerade offline ist - sonst
        // werden bei jedem periodischen Ping erneut DataSource + Driver-Stacktraces erzeugt.
        // Verbindungen werden lazy gebaut; Erreichbarkeit pruefen wir explizit ueber ping().
        config.setInitializationFailTimeout(-1);
        // minimumIdle=0 verhindert, dass Hikari's connection-adder im Hintergrund staendig
        // versucht den Pool zu fuellen. Sonst entstehen WARN-Logs mit Stacktrace, sobald
        // der Server offline ist - unabhaengig vom Anwendungs-Code.
        config.setMinimumIdle(0);

        if (StringUtils.isNotBlank(user)) {
            config.setUsername(user);
        }
        if (StringUtils.isNotBlank(pass)) {
            config.setPassword(pass);
        }

        return new HikariDataSource(config);
    }

    /**
     * Ensures the connection URL is in JDBC format.
     * If the URL doesn't start with "jdbc:", prepend "jdbc:ch://".
     * Also converts legacy "jdbc:clickhouse://" URLs to the "jdbc:ch://" format
     * required by clickhouse-jdbc 0.7.x.
     */
    private String toJdbcUrl(String url) {
        if (url == null) {
            return null;
        }
        String normalizedUrl = url;
        if (normalizedUrl.startsWith("jdbc:clickhouse://")) {
            normalizedUrl = "jdbc:ch://" + normalizedUrl.substring("jdbc:clickhouse://".length());
        } else if (!normalizedUrl.startsWith("jdbc:")) {
            normalizedUrl = "jdbc:ch://" + normalizedUrl;
        }
        return ensureDefaultQueryParameter(normalizedUrl, "compress", "0");
    }

    /**
     * Extracts the database name from a JDBC URL.
     */
    private String extractDatabaseFromUrl(String url) {
        if (url == null) {
            return null;
        }
        int lastSlash = url.lastIndexOf('/');
        if (lastSlash > 0 && lastSlash < url.length() - 1) {
            String dbPart = url.substring(lastSlash + 1);
            int questionMark = dbPart.indexOf('?');
            if (questionMark > 0) {
                return dbPart.substring(0, questionMark);
            }
            return dbPart;
        }
        return null;
    }

    private static String ensureDefaultQueryParameter(String url, String key, String value) {
        String lowerKey = key.toLowerCase(Locale.ROOT);
        int queryIndex = url.indexOf('?');
        if (queryIndex >= 0) {
            String query = url.substring(queryIndex + 1);
            for (String part : query.split("&")) {
                int equalsIndex = part.indexOf('=');
                String existingKey = equalsIndex >= 0 ? part.substring(0, equalsIndex) : part;
                if (existingKey.toLowerCase(Locale.ROOT).equals(lowerKey)) {
                    return url;
                }
            }
            return url + "&" + key + "=" + value;
        }
        return url + "?" + key + "=" + value;
    }
}

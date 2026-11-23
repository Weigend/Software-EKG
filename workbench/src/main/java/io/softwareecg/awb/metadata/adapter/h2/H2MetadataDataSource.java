package io.softwareecg.awb.metadata.adapter.h2;

import jakarta.annotation.PreDestroy;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.h2.jdbcx.JdbcDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Singleton that manages the H2 embedded database connection for metadata storage.
 * The database file is stored in the user's home directory under {@code ~/.ekg/metadata}.
 *
 * <p>The actual JDBC connection (and the {@code schema.sql} bootstrap) is opened
 * lazily on the first {@link #getConnection()} call, not in the constructor or a
 * {@code @PostConstruct} hook. That keeps Avaje {@code BeanScope.build()} cheap
 * during JavaFX {@code init()} so the WFX preloader can appear before any
 * disk/database work happens. A WFX module can force the open by calling
 * {@link #getConnection()} from its {@code preload()}.
 */
@Singleton
public class H2MetadataDataSource {

    private static final Logger LOGGER = LoggerFactory.getLogger(H2MetadataDataSource.class);

    private static final String DB_DIRECTORY = ".ekg";
    private static final String DB_NAME = "metadata";

    private volatile JdbcDataSource dataSource;

    /** Connection shared by all stores while the current thread executes one backend transaction. */
    private final ThreadLocal<Connection> transactionConnection = new ThreadLocal<>();

    /**
     * The JDBC URL to use instead of the database in the user's home; {@code null} in production.
     */
    private final String jdbcUrl;

    @Inject
    public H2MetadataDataSource() {
        this(null);
    }

    /**
     * For tests: runs the schema and the stores against the given database, e.g. an in-memory one,
     * instead of the user's metadata.
     */
    H2MetadataDataSource(String jdbcUrl) {
        this.jdbcUrl = jdbcUrl;
    }

    /**
     * Returns a new connection from the data source, opening the database
     * (and running {@code schema.sql}) on first access.
     *
     * @return a JDBC connection
     * @throws SQLException if a connection cannot be obtained
     */
    public Connection getConnection() throws SQLException {
        Connection current = transactionConnection.get();
        if (current != null) {
            return nonClosingView(current);
        }
        if (dataSource == null) {
            ensureInitialized();
        }
        return dataSource.getConnection();
    }

    /** Executes all store calls made by this thread on one JDBC transaction. */
    <T> T inTransaction(Supplier<T> work) {
        if (transactionConnection.get() != null) {
            return work.get();
        }
        try (Connection connection = physicalConnection()) {
            connection.setAutoCommit(false);
            transactionConnection.set(connection);
            try {
                T result = work.get();
                connection.commit();
                return result;
            } catch (RuntimeException | Error failure) {
                rollback(connection, failure);
                throw failure;
            } finally {
                transactionConnection.remove();
            }
        } catch (SQLException e) {
            throw new IllegalStateException("H2 metadata transaction failed", e);
        }
    }

    private Connection physicalConnection() throws SQLException {
        if (dataSource == null) {
            ensureInitialized();
        }
        return dataSource.getConnection();
    }

    private static void rollback(Connection connection, Throwable failure) {
        try {
            connection.rollback();
        } catch (SQLException rollbackFailure) {
            failure.addSuppressed(rollbackFailure);
        }
    }

    private static Connection nonClosingView(Connection connection) {
        return (Connection) Proxy.newProxyInstance(Connection.class.getClassLoader(),
                new Class<?>[]{Connection.class}, (proxy, method, arguments) -> {
                    if (method.getName().equals("close")) return null;
                    if (method.getName().equals("isClosed")) return false;
                    try {
                        return method.invoke(connection, arguments);
                    } catch (InvocationTargetException e) {
                        throw e.getCause();
                    }
                });
    }

    private synchronized void ensureInitialized() {
        if (dataSource != null) {
            return;
        }
        try {
            String dbUrl = jdbcUrl;
            if (dbUrl == null) {
                Path dbDir = Paths.get(System.getProperty("user.home"), DB_DIRECTORY);
                Files.createDirectories(dbDir);
                dbUrl = "jdbc:h2:" + dbDir.resolve(DB_NAME) + ";AUTO_SERVER=TRUE";
            }

            JdbcDataSource ds = new JdbcDataSource();
            ds.setURL(dbUrl);
            ds.setUser("sa");
            ds.setPassword("");

            initializeSchema(ds);
            dataSource = ds;

            LOGGER.info("H2 metadata database initialized at {}", dbUrl);

        } catch (IOException e) {
            throw new IllegalStateException("Failed to create H2 database directory", e);
        }
    }

    @PreDestroy
    public void shutdown() {
        if (dataSource == null) {
            return;
        }
        try (Connection conn = getConnection(); Statement stmt = conn.createStatement()) {
            stmt.execute("SHUTDOWN");
            LOGGER.info("H2 metadata database shut down.");
        } catch (SQLException e) {
            LOGGER.warn("Error during H2 shutdown", e);
        }
    }

    private void initializeSchema(JdbcDataSource ds) {
        String sql = loadSchemaScript();
        try (Connection conn = ds.getConnection(); Statement stmt = conn.createStatement()) {
            for (String statement : sql.split(";")) {
                String trimmed = statement.trim();
                if (!trimmed.isEmpty()) {
                    stmt.execute(trimmed);
                }
            }
            LOGGER.info("H2 schema initialized successfully.");
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to initialize H2 schema", e);
        }
    }

    private String loadSchemaScript() {
        try (InputStream is = getClass().getClassLoader().getResourceAsStream("schema.sql")) {
            if (is == null) {
                throw new IllegalStateException("schema.sql not found on classpath");
            }
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                return reader.lines().collect(Collectors.joining("\n"));
            }
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read schema.sql", e);
        }
    }
}

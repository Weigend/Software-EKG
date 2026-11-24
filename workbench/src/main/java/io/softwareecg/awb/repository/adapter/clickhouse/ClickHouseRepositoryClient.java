package io.softwareecg.awb.repository.adapter.clickhouse;

import io.softwareecg.awb.repository.adapter.clickhouse.expression.ClickHouseExpressionRenderer;
import io.softwareecg.awb.repository.adapter.clickhouse.util.ClickHouseObjectBinder;
import io.softwareecg.awb.repository.persistence.TimeSeriesRepository;
import io.softwareecg.awb.repository.api.RepositoryNotAvailableException;
import io.softwareecg.awb.repository.persistence.CollectionProperties;
import io.softwareecg.awb.repository.persistence.DeleteParams;
import io.softwareecg.awb.repository.persistence.search.expr.Expression;
import io.softwareecg.awb.repository.persistence.search.facet.Facet;
import io.softwareecg.awb.repository.persistence.search.facet.FacetEntry;
import io.softwareecg.awb.repository.persistence.search.facet.FacetParams;
import io.softwareecg.awb.repository.persistence.search.facet.FacetResult;
import io.softwareecg.awb.repository.persistence.search.ExportParams;
import io.softwareecg.awb.repository.persistence.search.SearchParams;
import io.softwareecg.awb.repository.persistence.search.SearchResult;
import io.softwareecg.awb.repository.persistence.search.SortField;
import io.softwareecg.awb.repository.persistence.search.QueryStatus;
import io.softwareecg.awb.repository.persistence.schema.EkgSchemaField;
import io.softwareecg.awb.repository.persistence.schema.Field;
import io.softwareecg.awb.repository.api.RepositoryException;
import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.time.StopWatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.net.ConnectException;
import java.sql.*;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * A {@link TimeSeriesRepository} providing access to a ClickHouse database.
 *
 * <p>This implementation stores time series data using ClickHouse-optimized array columns
 * instead of binary blobs, achieving significantly better compression through specialized
 * codecs (DoubleDelta for timestamps, Gorilla for float values).</p>
 *
 * <p>Connections are obtained per-operation from a HikariCP-backed {@link DataSource}
 * and returned to the pool after each operation. This enables parallel query execution
 * and round-robin load balancing across ClickHouse cluster nodes.</p>
 *
 */
public final class ClickHouseRepositoryClient implements TimeSeriesRepository {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClickHouseRepositoryClient.class);
    private static final String IO_ERROR_MESSAGE = "I/O error accessing ClickHouse.";

    private final DataSource dataSource;
    /** JDBC-URL fuer leichtgewichtigen TCP-Erreichbarkeits-Check vor jedem Ping;
     *  vermeidet Driver-/Pool-Exceptions waehrend der Server offline ist. */
    private final String jdbcUrl;
    private final String tableName;
    private final ClickHouseQueryFactory queryFactory;
    private final ClickHouseExpressionRenderer expressionRenderer;

    private volatile boolean tableEnsured;

    /** Letzter beobachteter Erreichbarkeits-Zustand. null = noch nie geprueft, damit
        der erste ping() einen State-Transition-Log erzeugt. */
    private volatile Boolean lastReachable;

    ClickHouseRepositoryClient(DataSource dataSource, String jdbcUrl, String tableName) {
        this(dataSource, jdbcUrl, tableName, new ClickHouseQueryFactory(), new ClickHouseExpressionRenderer());
    }

    ClickHouseRepositoryClient(DataSource dataSource, String jdbcUrl, String tableName,
                               ClickHouseQueryFactory queryFactory, ClickHouseExpressionRenderer expressionRenderer) {
        this.dataSource = Objects.requireNonNull(dataSource, "DataSource must not be null");
        this.jdbcUrl = jdbcUrl;
        this.tableName = tableName;
        this.queryFactory = queryFactory;
        this.expressionRenderer = expressionRenderer;
    }

    @Override
    public <T> SearchResult<T> search(Class<T> type, SearchParams searchParams) throws RepositoryException {
        try {
            Validate.notNull(type);
            Validate.notNull(searchParams);
            Validate.notNull(searchParams.getFilterQueries());

            StopWatch stopWatch = StopWatch.createStarted();

            String sql = queryFactory.createSelectQuery(tableName, type, searchParams);
            String countSql = queryFactory.createCountQuery(tableName, searchParams.getFilterQueries());

            LOGGER.debug("Executing ClickHouse query: {}", sql);

            List<T> rows = executeSelectQuery(type, sql);
            long totalHits = executeCountQuery(countSql);

            long queryTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
            LOGGER.debug("Response: found {} documents, returned {} documents, in {}ms",
                    totalHits, rows.size(), queryTime);

            QueryStatus queryStatus = new QueryStatus(totalHits, (int) queryTime, null);
            return new SearchResult<>(rows, queryStatus);

        } catch (SQLException e) {
            throw translateException(e);
        }
    }

    @Override
    public <T> Stream<T> export(Class<T> type, ExportParams exportParams) throws RepositoryException {
        Validate.notNull(type);
        Validate.notNull(exportParams);
        Validate.notNull(exportParams.getFilterQueries());
        Validate.notEmpty(exportParams.getSortFields(), "Exporting requires at least one sort field");

        try {
            String sql = queryFactory.createExportQuery(tableName, type, exportParams);
            LOGGER.debug("Executing ClickHouse export query: {}", sql);

            // Connection stays open for the lifetime of the stream.
            // ResultSetIterator closes connection + statement + resultset when iteration completes.
            Connection conn = getConnection();
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(sql);

            Iterator<T> iterator = new ResultSetIterator<>(type, rs, stmt, conn);
            return StreamSupport.stream(
                    Spliterators.spliteratorUnknownSize(iterator, Spliterator.ORDERED), false);

        } catch (SQLException e) {
            throw translateException(e);
        }
    }

    @Override
    public <T> SearchResult<T> collapse(Class<T> type, SearchParams searchParams,
                                        Field groupingField, SortField groupSortField) throws RepositoryException {
        try {
            Validate.notNull(type);
            Validate.notNull(searchParams);
            Validate.notNull(searchParams.getFilterQueries());
            Validate.notNull(groupingField);

            StopWatch stopWatch = StopWatch.createStarted();

            String sql = queryFactory.createCollapseQuery(tableName, type, searchParams,
                    groupingField, groupSortField);
            LOGGER.debug("Executing ClickHouse collapse query: {}", sql);

            List<T> rows = executeSelectQuery(type, sql);
            long queryTime = stopWatch.getTime(TimeUnit.MILLISECONDS);

            QueryStatus queryStatus = new QueryStatus(rows.size(), (int) queryTime, null);
            return new SearchResult<>(rows, queryStatus);

        } catch (SQLException e) {
            throw translateException(e);
        }
    }

    @Override
    public long sumFieldValue(EkgSchemaField field, List<Expression> filterQueries) throws RepositoryException {
        try {
            String sql = queryFactory.createSumQuery(tableName, field, filterQueries);
            LOGGER.debug("Executing ClickHouse sum query: {}", sql);

            try (Connection conn = getConnection();
                 Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(sql)) {
                if (rs.next()) {
                    return rs.getLong("total");
                }
                return 0;
            }

        } catch (SQLException e) {
            throw translateException(e);
        }
    }

    @Override
    public FacetResult facet(FacetParams facetParams) throws RepositoryException {
        try {
            Validate.notNull(facetParams);
            Validate.notEmpty(facetParams.getFacetFields());
            Validate.notNull(facetParams.getFacetMissing());
            Validate.notNull(facetParams.getFilterQueries());

            StopWatch stopWatch = StopWatch.createStarted();

            List<Facet> facets = new ArrayList<>();

            try (Connection conn = getConnection()) {
                for (Field facetField : facetParams.getFacetFields()) {
                    String sql = buildSingleFacetQuery(facetField, facetParams);
                    LOGGER.debug("Executing ClickHouse facet query: {}", sql);

                    List<FacetEntry> entries = new ArrayList<>();
                    try (Statement stmt = conn.createStatement();
                         ResultSet rs = stmt.executeQuery(sql)) {
                        while (rs.next()) {
                            String value = rs.getString("facet_value");
                            long count = rs.getLong("facet_count");
                            entries.add(new FacetEntry(value, count));
                        }
                    }
                    facets.add(new Facet(facetField.getName(), entries));
                }
            }

            long queryTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
            QueryStatus queryStatus = new QueryStatus(0, (int) queryTime, null);
            return new FacetResult(facets, queryStatus);

        } catch (SQLException e) {
            throw translateException(e);
        }
    }

    @Override
    public void add(Iterator<?> entities) throws RepositoryException {
        try (Connection conn = getConnection()) {
            boolean first = true;
            String insertSql = null;
            PreparedStatement ps = null;

            while (entities.hasNext()) {
                Object entity = entities.next();
                if (entity == null) continue;

                Map<String, Object> columnValues = ClickHouseObjectBinder.mapToColumnValues(entity);

                if (first) {
                    insertSql = buildInsertSql(columnValues);
                    ps = conn.prepareStatement(insertSql);
                    first = false;
                }

                setInsertParameters(ps, columnValues);
                ps.addBatch();
            }

            if (ps != null) {
                ps.executeBatch();
                ps.close();
            }

        } catch (SQLException e) {
            LOGGER.error("Exception at ClickHouse persistence layer occurred: ", e);
            throw new RepositoryException(IO_ERROR_MESSAGE, e);
        }
    }

    @Override
    public void add(Object entity) throws RepositoryException {
        try {
            Map<String, Object> columnValues = ClickHouseObjectBinder.mapToColumnValues(entity);
            String insertSql = buildInsertSql(columnValues);

            try (Connection conn = getConnection();
                 PreparedStatement ps = conn.prepareStatement(insertSql)) {
                setInsertParameters(ps, columnValues);
                ps.executeUpdate();
            }

        } catch (SQLException e) {
            throw new RepositoryException(IO_ERROR_MESSAGE, e);
        }
    }

    @Override
    public void deleteAll() throws RepositoryException {
        try {
            String sql = queryFactory.createDeleteAllQuery(tableName);
            LOGGER.debug("Executing ClickHouse delete all: {}", sql);

            try (Connection conn = getConnection();
                 Statement stmt = conn.createStatement()) {
                stmt.execute(sql);
            }

        } catch (SQLException e) {
            throw new RepositoryException(IO_ERROR_MESSAGE, e);
        }
    }

    @Override
    public void delete(DeleteParams deleteParams) throws RepositoryException {
        try {
            String filterQuery = renderDeleteFilter(deleteParams);
            String sql = queryFactory.createDeleteQuery(tableName, filterQuery);
            LOGGER.debug("Executing ClickHouse delete: {}", sql);

            try (Connection conn = getConnection();
                 Statement stmt = conn.createStatement()) {
                stmt.execute(sql);
            }

        } catch (SQLException e) {
            throw new RepositoryException(IO_ERROR_MESSAGE, e);
        }
    }

    @Override
    public void commit() throws RepositoryException {
        // ClickHouse does not use transactions in the traditional sense.
        // Writes are visible immediately after insertion.
        LOGGER.trace("commit() called - no-op for ClickHouse (writes are immediately visible)");
    }

    @Override
    public boolean ping() {
        // Schritt 1: leichtgewichtiger TCP-Pre-Check. Wenn der Port nicht erreichbar ist,
        // wird kein JDBC-Aufruf gemacht - dadurch sehen Driver und Hikari im Offline-Fall
        // keinen Connect-Versuch und produzieren keine Stacktraces.
        if (jdbcUrl != null && !ClickHouseHost.isReachable(jdbcUrl)) {
            logReachabilityChange(false, "host not reachable (TCP probe)");
            return false;
        }

        // Schritt 2: TCP ist offen -> echter SELECT-1-Health-Check ueber JDBC.
        boolean reachable = false;
        String failureSummary = null;
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery("SELECT 1")) {
            reachable = resultSet.next() && resultSet.getInt(1) == 1;
        } catch (Exception e) {
            failureSummary = rootCauseSummary(e);
        }
        logReachabilityChange(reachable, failureSummary);
        return reachable;
    }

    /**
     * Loggt nur Zustandswechsel: erstmaliger Ping, online -> offline, offline -> online.
     * Dadurch bleibt das Log ruhig waehrend der Server stabil offline (oder online) ist.
     * Ohne Stacktrace - nur die kurze Ursache, da ein nicht erreichbarer Server fuer
     * den Aufrufer (Explorer-Heartbeat) erwartbares Verhalten ist.
     */
    private void logReachabilityChange(boolean reachable, String failureSummary) {
        Boolean prev = lastReachable;
        if (prev != null && prev == reachable) {
            return;
        }
        lastReachable = reachable;
        if (reachable) {
            LOGGER.info("ClickHouse repository is reachable.");
        } else {
            LOGGER.warn("ClickHouse repository is unreachable: {}", failureSummary);
        }
    }

    /**
     * Liefert eine kompakte Beschreibung der Wurzel-Ursache (Klasse + Message)
     * statt der vollstaendigen Exception-Kette.
     */
    private static String rootCauseSummary(Throwable t) {
        Throwable cur = t;
        while (cur.getCause() != null && cur.getCause() != cur) {
            cur = cur.getCause();
        }
        return cur.getClass().getSimpleName() + ": " + cur.getMessage();
    }

    @Override
    public void close() {
        // Connection lifecycle is managed by the DataSource pool.
        // Nothing to close here — individual connections are returned per-operation.
    }

    // ==========================================================================================================
    //  Internal helper methods
    // ==========================================================================================================

    /**
     * Obtains a connection from the pool. On first access, ensures the table exists.
     */
    private Connection getConnection() throws SQLException {
        ensureTableExists();
        return dataSource.getConnection();
    }

    /**
     * Creates the ClickHouse table if it does not already exist.
     * Uses the {@link ClickHouseRepositoryAdminClient} so the DDL is defined in one place only.
     */
    private void ensureTableExists() {
        if (tableEnsured || tableName == null) {
            return;
        }
        synchronized (this) {
            if (tableEnsured) {
                return;
            }
            try {
                CollectionProperties props = new CollectionProperties();
                props.setCollectionName(tableName);
                props.setAmountOfShards(1);
                props.setAmountOfReplicas(1);

                ClickHouseRepositoryAdminClient adminClient =
                        new ClickHouseRepositoryAdminClient(dataSource, null, extractDatabase());
                adminClient.create(props);
            } catch (Exception e) {
                LOGGER.debug("Table '{}' may already exist or could not be auto-created: {}", tableName, e.getMessage());
            }
            tableEnsured = true;
        }
    }

    private String extractDatabase() {
        // Extract database from the DataSource's JDBC URL via metadata
        try (Connection conn = dataSource.getConnection()) {
            String url = conn.getMetaData().getURL();
            if (url != null) {
                int lastSlash = url.lastIndexOf('/');
                if (lastSlash > 0 && lastSlash < url.length() - 1) {
                    String dbPart = url.substring(lastSlash + 1);
                    int q = dbPart.indexOf('?');
                    return q > 0 ? dbPart.substring(0, q) : dbPart;
                }
            }
        } catch (SQLException e) {
            LOGGER.debug("Could not extract database name from DataSource", e);
        }
        return null;
    }

    private <T> List<T> executeSelectQuery(Class<T> type, String sql) throws SQLException {
        List<T> results = new ArrayList<>();
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                results.add(ClickHouseObjectBinder.mapToBean(type, rs));
            }
        }
        return results;
    }

    private long executeCountQuery(String sql) throws SQLException {
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                return rs.getLong("cnt");
            }
            return 0;
        }
    }

    private String buildInsertSql(Map<String, Object> columnValues) {
        Set<String> columns = columnValues.keySet();
        String columnList = String.join(", ", columns);
        String placeholders = columns.stream().map(c -> "?").collect(java.util.stream.Collectors.joining(", "));
        return "INSERT INTO " + tableName + " (" + columnList + ") VALUES (" + placeholders + ")";
    }

    private void setInsertParameters(PreparedStatement ps, Map<String, Object> columnValues) throws SQLException {
        int index = 1;
        for (Map.Entry<String, Object> entry : columnValues.entrySet()) {
            Object value = entry.getValue();

            if (value == null) {
                ps.setNull(index, Types.NULL);
            } else if (value instanceof String s) {
                ps.setString(index, s);
            } else if (value instanceof Integer i) {
                ps.setInt(index, i);
            } else if (value instanceof Long l) {
                ps.setLong(index, l);
            } else if (value instanceof Double d) {
                ps.setDouble(index, d);
            } else if (value instanceof Boolean b) {
                ps.setBoolean(index, b);
            } else if (value instanceof Timestamp ts) {
                ps.setTimestamp(index, ts);
            } else if (value instanceof Long[] arr) {
                ps.setObject(index, arr);
            } else if (value instanceof Double[] arr) {
                ps.setObject(index, arr);
            } else if (value instanceof byte[] bytes) {
                ps.setBytes(index, bytes);
            } else {
                ps.setObject(index, value);
            }
            index++;
        }
    }

    private String buildSingleFacetQuery(Field facetField, FacetParams facetParams) {
        boolean includeMissing = facetParams.getFacetMissing().contains(facetField);
        StringBuilder sql = new StringBuilder("SELECT ");

        if (includeMissing) {
            sql.append("COALESCE(toString(").append(facetField.getName()).append("), '<NULL>') AS facet_value");
        } else {
            sql.append("toString(").append(facetField.getName()).append(") AS facet_value");
        }

        sql.append(", count() AS facet_count FROM ").append(tableName);

        // WHERE clause
        List<String> conditions = new ArrayList<>();
        if (facetParams.getFilterQueries() != null) {
            for (Expression expr : facetParams.getFilterQueries()) {
                conditions.add(expressionRenderer.render(expr));
            }
        }
        if (!includeMissing) {
            conditions.add(facetField.getName() + " IS NOT NULL");
        }

        if (!conditions.isEmpty()) {
            sql.append(" WHERE ").append(String.join(" AND ", conditions));
        }

        sql.append(" GROUP BY facet_value");

        if (facetParams.getMinCount() > 0) {
            sql.append(" HAVING facet_count >= ").append(facetParams.getMinCount());
        }

        String sortOrder = "count".equals(facetParams.getFacetSort())
                ? " ORDER BY facet_count DESC"
                : " ORDER BY facet_value ASC";
        sql.append(sortOrder);

        if (facetParams.getLimit() > 0) {
            sql.append(" LIMIT ").append(facetParams.getLimit());
        }

        return sql.toString();
    }

    private String renderDeleteFilter(DeleteParams deleteParams) {
        Map<EkgSchemaField, String> filterMap = deleteParams.getFilterExpMap();
        List<String> conditions = new ArrayList<>();

        for (Map.Entry<EkgSchemaField, String> entry : filterMap.entrySet()) {
            conditions.add(entry.getKey().getName() + " = '"
                    + entry.getValue().replace("'", "\\'") + "'");
        }

        return String.join(" AND ", conditions);
    }

    private RepositoryException translateException(SQLException e) {
        Throwable rootCause = e;
        while (rootCause.getCause() != null) {
            rootCause = rootCause.getCause();
        }

        if (rootCause instanceof ConnectException) {
            return new RepositoryNotAvailableException(
                    RepositoryNotAvailableException.Cause.HOST_NOT_AVAILABLE, e);
        }

        String message = e.getMessage();
        if (message != null && (message.contains("UNKNOWN_TABLE") || message.contains("doesn't exist"))) {
            return new RepositoryNotAvailableException(
                    RepositoryNotAvailableException.Cause.INDEX_NOT_AVAILABLE, e);
        }

        return new RepositoryException(IO_ERROR_MESSAGE, e);
    }

    // ==========================================================================================================
    //  ResultSet iterator for streaming export
    // ==========================================================================================================

    /**
     * Iterator that reads rows from a JDBC ResultSet and maps them to entities.
     * Closes the ResultSet, Statement, and Connection when iteration is complete,
     * returning the connection back to the pool.
     */
    private static class ResultSetIterator<T> implements Iterator<T> {
        private final Class<T> type;
        private final ResultSet rs;
        private final Statement stmt;
        private final Connection conn;
        private Boolean hasNext;

        ResultSetIterator(Class<T> type, ResultSet rs, Statement stmt, Connection conn) {
            this.type = type;
            this.rs = rs;
            this.stmt = stmt;
            this.conn = conn;
        }

        @Override
        public boolean hasNext() {
            if (hasNext == null) {
                try {
                    hasNext = rs.next();
                    if (!hasNext) {
                        closeResources();
                    }
                } catch (SQLException e) {
                    closeResources();
                    throw new IllegalStateException("Error reading from ClickHouse ResultSet", e);
                }
            }
            return hasNext;
        }

        @Override
        public T next() {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }
            try {
                T result = ClickHouseObjectBinder.mapToBean(type, rs);
                hasNext = null;
                return result;
            } catch (SQLException e) {
                closeResources();
                throw new IllegalStateException("Error mapping ClickHouse row to entity", e);
            }
        }

        private void closeResources() {
            try { rs.close(); } catch (SQLException ignored) {}
            try { stmt.close(); } catch (SQLException ignored) {}
            try { conn.close(); } catch (SQLException ignored) {}
        }
    }
}

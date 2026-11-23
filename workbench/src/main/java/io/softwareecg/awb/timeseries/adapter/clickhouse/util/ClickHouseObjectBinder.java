package io.softwareecg.awb.timeseries.adapter.clickhouse.util;

import io.softwareecg.awb.timeseries.schema.EkgSchemaField;
import io.softwareecg.awb.timeseries.schema.FieldReader;
import org.apache.commons.lang3.reflect.ConstructorUtils;
import org.apache.commons.lang3.reflect.FieldUtils;

import java.lang.reflect.Field;
import java.sql.Array;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.*;

/**
 * Maps between EKG Java entity objects and ClickHouse table rows.
 *
 * <p>The key difference from the Solr ObjectBinder: the binary time series data field
 * ({@code ts_data}, a byte[] of 16-byte tuples) is stored in ClickHouse as two separate
 * array columns: {@code ts_timestamps Array(Int64)} and {@code ts_values Array(Float64)}.
 * This enables ClickHouse-specific compression codecs for optimal storage.</p>
 *
 */
public class ClickHouseObjectBinder {

    /** The ClickHouse column name for the timestamps array (replaces binary ts_data). */
    public static final String COL_TS_TIMESTAMPS = "ts_timestamps";

    /** The ClickHouse column name for the values array (replaces binary ts_data). */
    public static final String COL_TS_VALUES = "ts_values";

    /** The schema field name for the binary time series data. */
    private static final String FIELD_TS_DATA = EkgSchemaField.TS_DATA.getName();

    /** Schema field names that are Date types in the Solr schema. */
    private static final Set<String> DATE_FIELDS = Set.of(
            EkgSchemaField.TS_START.getName(),
            EkgSchemaField.TS_STOP.getName(),
            EkgSchemaField.TS_IMPORT_DATE.getName()
    );

    /**
     * Extracts column values from a Java entity for a ClickHouse INSERT.
     * Returns a map of column name → value, with special handling for the ts_data field.
     *
     * @param ekgEntity the entity to extract values from
     * @return map of column name to value
     */
    public static Map<String, Object> mapToColumnValues(Object ekgEntity) {
        Map<String, Field> fieldMap = FieldReader.getSchemaFields(ekgEntity.getClass());

        if (fieldMap.isEmpty()) {
            throw new IllegalStateException("Class " + ekgEntity.getClass().getName()
                    + " does not define any fields.");
        }

        Map<String, Object> columns = new LinkedHashMap<>();

        for (Map.Entry<String, Field> entry : fieldMap.entrySet()) {
            try {
                String schemaFieldName = entry.getKey();
                Object value = FieldUtils.readField(entry.getValue(), ekgEntity, true);

                if (value == null) {
                    continue;
                }

                if (FIELD_TS_DATA.equals(schemaFieldName)) {
                    // Convert byte[] to two separate arrays for ClickHouse
                    byte[] data = (byte[]) value;
                    long[] timestamps = TimeSeriesConverter.extractTimestamps(data);
                    double[] values = TimeSeriesConverter.extractValues(data);
                    columns.put(COL_TS_TIMESTAMPS, TimeSeriesConverter.boxTimestamps(timestamps));
                    columns.put(COL_TS_VALUES, TimeSeriesConverter.boxValues(values));
                } else if (DATE_FIELDS.contains(schemaFieldName)) {
                    // Convert java.util.Date to java.sql.Timestamp for JDBC
                    if (value instanceof Date dateValue) {
                        columns.put(schemaFieldName, new Timestamp(dateValue.getTime()));
                    } else {
                        columns.put(schemaFieldName, value);
                    }
                } else {
                    columns.put(schemaFieldName, value);
                }

            } catch (IllegalAccessException e) {
                throw new IllegalStateException("Unable to read field '" + entry.getValue().getName()
                        + "' of class '" + ekgEntity.getClass().getName() + "' because access is forbidden.", e);
            }
        }

        return columns;
    }

    /**
     * Maps a ClickHouse ResultSet row to a Java entity object.
     *
     * @param type the target entity class
     * @param rs   the ResultSet positioned at the current row
     * @param <T>  the entity type
     * @return the mapped entity
     */
    public static <T> T mapToBean(Class<T> type, ResultSet rs) throws SQLException {
        try {
            Map<String, Field> fieldMap = FieldReader.getSchemaFields(type);
            T bean = ConstructorUtils.invokeConstructor(type);

            for (Map.Entry<String, Field> beanFieldEntry : fieldMap.entrySet()) {
                String schemaFieldName = beanFieldEntry.getKey();
                Field beanField = beanFieldEntry.getValue();

                if (FIELD_TS_DATA.equals(schemaFieldName)) {
                    // Reconstruct byte[] from the two array columns
                    Object timestampsObj = getArrayData(rs, COL_TS_TIMESTAMPS);
                    Object valuesObj = getArrayData(rs, COL_TS_VALUES);

                    if (timestampsObj != null && valuesObj != null) {
                        long[] timestamps = toLongArray(timestampsObj);
                        double[] values = toDoubleArray(valuesObj);
                        byte[] data = TimeSeriesConverter.toByteArray(timestamps, values);
                        FieldUtils.writeField(beanField, bean, data, true);
                    }
                } else if (DATE_FIELDS.contains(schemaFieldName)) {
                    Timestamp ts = rs.getTimestamp(schemaFieldName);
                    if (ts != null && !rs.wasNull()) {
                        FieldUtils.writeField(beanField, bean, new Date(ts.getTime()), true);
                    }
                } else {
                    Object value = getColumnValue(rs, schemaFieldName, beanField.getType());
                    if (value != null && !rs.wasNull()) {
                        FieldUtils.writeField(beanField, bean, value, true);
                    }
                }
            }

            return bean;
        } catch (SQLException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("Unable to map ClickHouse result to entity of type '"
                    + type.getName() + "'", e);
        }
    }

    /**
     * Returns the set of ClickHouse column names needed for a SELECT query,
     * based on the @PersistedField annotations of the entity class.
     * Replaces ts_data with ts_timestamps and ts_values.
     */
    public static Set<String> getSelectColumns(Class<?> type) {
        Map<String, Field> fieldMap = FieldReader.getSchemaFields(type);
        Set<String> columns = new LinkedHashSet<>();

        for (String schemaFieldName : fieldMap.keySet()) {
            if (FIELD_TS_DATA.equals(schemaFieldName)) {
                columns.add(COL_TS_TIMESTAMPS);
                columns.add(COL_TS_VALUES);
            } else {
                columns.add(schemaFieldName);
            }
        }

        return columns;
    }

    /**
     * Returns the set of ClickHouse column names for an INSERT statement,
     * based on the entity's non-null field values.
     */
    public static Set<String> getInsertColumns(Map<String, Object> columnValues) {
        return columnValues.keySet();
    }

    /**
     * Converts an array object (primitive long[] or boxed Long[]/Object[]) to long[].
     * The ClickHouse JDBC driver may return either format depending on version.
     */
    private static long[] toLongArray(Object arrayObj) {
        if (arrayObj instanceof long[] primitiveArr) {
            return primitiveArr;
        }
        if (arrayObj instanceof Object[] boxedArr) {
            long[] result = new long[boxedArr.length];
            for (int i = 0; i < boxedArr.length; i++) {
                result[i] = ((Number) boxedArr[i]).longValue();
            }
            return result;
        }
        return new long[0];
    }

    /**
     * Converts an array object (primitive double[] or boxed Double[]/Object[]) to double[].
     * The ClickHouse JDBC driver may return either format depending on version.
     */
    private static double[] toDoubleArray(Object arrayObj) {
        if (arrayObj instanceof double[] primitiveArr) {
            return primitiveArr;
        }
        if (arrayObj instanceof Object[] boxedArr) {
            double[] result = new double[boxedArr.length];
            for (int i = 0; i < boxedArr.length; i++) {
                result[i] = ((Number) boxedArr[i]).doubleValue();
            }
            return result;
        }
        return new double[0];
    }

    private static Object getArrayData(ResultSet rs, String columnName) throws SQLException {
        try {
            Array array = rs.getArray(columnName);
            if (array != null) {
                return array.getArray();
            }
        } catch (SQLException e) {
            // Column may not exist in this result set
        }
        return null;
    }

    private static Object getColumnValue(ResultSet rs, String columnName, Class<?> targetType) throws SQLException {
        try {
            if (targetType == String.class) {
                return rs.getString(columnName);
            } else if (targetType == Integer.class || targetType == int.class) {
                int val = rs.getInt(columnName);
                return rs.wasNull() ? null : val;
            } else if (targetType == Long.class || targetType == long.class) {
                long val = rs.getLong(columnName);
                return rs.wasNull() ? null : val;
            } else if (targetType == Double.class || targetType == double.class) {
                double val = rs.getDouble(columnName);
                return rs.wasNull() ? null : val;
            } else if (targetType == Boolean.class || targetType == boolean.class) {
                boolean val = rs.getBoolean(columnName);
                return rs.wasNull() ? null : val;
            } else if (targetType == byte[].class) {
                return rs.getBytes(columnName);
            } else {
                return rs.getObject(columnName);
            }
        } catch (SQLException e) {
            // Column may not exist; return null
            return null;
        }
    }
}

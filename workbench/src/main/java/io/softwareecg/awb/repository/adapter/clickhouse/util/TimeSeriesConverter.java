package io.softwareecg.awb.repository.adapter.clickhouse.util;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Converts between the binary time series format used in {@code BinaryTimeSeries.data}
 * (16-byte tuples: 8 bytes long timestamp + 8 bytes double value) and the ClickHouse-optimized
 * array representation ({@code Array(Int64)} for timestamps + {@code Array(Float64)} for values).
 *
 * <p>The array-based storage in ClickHouse benefits from specialized codecs:
 * <ul>
 *   <li><b>DoubleDelta</b> for timestamps: sequential timestamps compress to near-zero deltas</li>
 *   <li><b>Gorilla</b> for float values: XOR-based encoding similar to Facebook's Gorilla paper</li>
 *   <li><b>ZSTD</b> on top for additional compression</li>
 * </ul>
 * This achieves 5-10x better compression than storing raw binary blobs.</p>
 *
 */
public final class TimeSeriesConverter {

    private static final int TUPLE_SIZE = 16;

    private TimeSeriesConverter() {
        throw new UnsupportedOperationException();
    }

    /**
     * Extracts timestamps from the binary time series data.
     *
     * @param data the binary data (16-byte tuples: long timestamp + double value)
     * @return array of timestamps in milliseconds since epoch
     */
    public static long[] extractTimestamps(byte[] data) {
        if (data == null || data.length == 0) {
            return new long[0];
        }

        int count = data.length / TUPLE_SIZE;
        long[] timestamps = new long[count];
        ByteBuffer buf = ByteBuffer.wrap(data).order(ByteOrder.BIG_ENDIAN);

        for (int i = 0; i < count; i++) {
            timestamps[i] = buf.getLong();
            buf.position(buf.position() + 8); // skip value
        }

        return timestamps;
    }

    /**
     * Extracts values from the binary time series data.
     *
     * @param data the binary data (16-byte tuples: long timestamp + double value)
     * @return array of measurement values
     */
    public static double[] extractValues(byte[] data) {
        if (data == null || data.length == 0) {
            return new double[0];
        }

        int count = data.length / TUPLE_SIZE;
        double[] values = new double[count];
        ByteBuffer buf = ByteBuffer.wrap(data).order(ByteOrder.BIG_ENDIAN);

        for (int i = 0; i < count; i++) {
            buf.position(buf.position() + 8); // skip timestamp
            values[i] = buf.getDouble();
        }

        return values;
    }

    /**
     * Reconstructs the binary time series data from separate timestamp and value arrays.
     *
     * @param timestamps array of timestamps in milliseconds since epoch
     * @param values     array of measurement values
     * @return the binary data (16-byte tuples: long timestamp + double value)
     * @throws IllegalArgumentException if arrays have different lengths
     */
    public static byte[] toByteArray(long[] timestamps, double[] values) {
        if (timestamps == null || values == null) {
            return new byte[0];
        }

        if (timestamps.length != values.length) {
            throw new IllegalArgumentException("Timestamps and values arrays must have the same length. " +
                    "Got " + timestamps.length + " timestamps and " + values.length + " values.");
        }

        byte[] data = new byte[timestamps.length * TUPLE_SIZE];
        ByteBuffer buf = ByteBuffer.wrap(data).order(ByteOrder.BIG_ENDIAN);

        for (int i = 0; i < timestamps.length; i++) {
            buf.putLong(timestamps[i]);
            buf.putDouble(values[i]);
        }

        return data;
    }

    /**
     * Converts a long[] to Long[] for use with ClickHouse JDBC arrays.
     */
    public static Long[] boxTimestamps(long[] timestamps) {
        Long[] boxed = new Long[timestamps.length];
        for (int i = 0; i < timestamps.length; i++) {
            boxed[i] = timestamps[i];
        }
        return boxed;
    }

    /**
     * Converts a double[] to Double[] for use with ClickHouse JDBC arrays.
     */
    public static Double[] boxValues(double[] values) {
        Double[] boxed = new Double[values.length];
        for (int i = 0; i < values.length; i++) {
            boxed[i] = values[i];
        }
        return boxed;
    }

    /**
     * Converts a Long[] (from ClickHouse JDBC) back to long[].
     */
    public static long[] unboxTimestamps(Object[] boxed) {
        long[] timestamps = new long[boxed.length];
        for (int i = 0; i < boxed.length; i++) {
            timestamps[i] = ((Number) boxed[i]).longValue();
        }
        return timestamps;
    }

    /**
     * Converts a Double[] (from ClickHouse JDBC) back to double[].
     */
    public static double[] unboxValues(Object[] boxed) {
        double[] values = new double[boxed.length];
        for (int i = 0; i < boxed.length; i++) {
            values[i] = ((Number) boxed[i]).doubleValue();
        }
        return values;
    }
}

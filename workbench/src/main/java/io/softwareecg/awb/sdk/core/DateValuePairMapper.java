package io.softwareecg.awb.sdk.core;

import io.softwareecg.awb.sdk.datamodel.Value;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

/**
 * Utility class that convert time series entries ({@link Value}) to
 * byte arrays and vice versa.
 *
 * @since 18.08.2018
 */
public final class DateValuePairMapper {
    /**
     * Private constructor
     */
    private DateValuePairMapper() {
        //utility class
    }

    /**
     * Helper to encode and compress one time series.
     *
     * @param data the data.
     * @return a compressed string.
     */
    public static byte[] compressAndEncodeDataPlainBytes(List<Value> data) {
        if (data == null) {
            throw new IllegalArgumentException("Could not compress and encode points");
        }

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            writeValuesAsPlainBytes(baos, data);
            return baos.toByteArray();

        } catch (IOException e) {
            throw new IllegalArgumentException("Could not compress and encode points", e);
        }
    }

    private static void writeValuesAsPlainBytes(ByteArrayOutputStream baos, List<Value> data) throws IOException {
        for (Value tsValueTuple : data) {
            byte[] buffer = new byte[16];
            longToByteArray(Double.doubleToLongBits(tsValueTuple.getValue()), buffer, 0);
            longToByteArray(tsValueTuple.getTimestamp(), buffer, 8);
            baos.write(buffer);
        }
    }

    public static void uncompressAndDecodePlainBytes(byte[] data, ValuePairConsumer filterFunction) {
        if (data == null || data.length % 16 != 0) {
            throw new IllegalArgumentException("Could not uncompress and decode points");
        }

        for (int offset = 0; offset < data.length; offset += 16) {
            filterFunction.processValuePair(
                    byteArrayToLong(data, offset + 8),
                    Double.longBitsToDouble(byteArrayToLong(data, offset))
            );
        }
    }

    /**
     * Decodes byte[] directly to primitive arrays without creating Value objects.
     * Returns a two-element array: [0] = long[] timestamps, [1] = double[] values.
     */
    public static Object[] decodeToPrimitiveArrays(byte[] data) {
        if (data == null || data.length % 16 != 0) {
            throw new IllegalArgumentException("Could not uncompress and decode points");
        }

        int count = data.length / 16;
        long[] timestamps = new long[count];
        double[] values = new double[count];

        for (int i = 0; i < count; i++) {
            int offset = i * 16;
            timestamps[i] = byteArrayToLong(data, offset + 8);
            values[i] = Double.longBitsToDouble(byteArrayToLong(data, offset));
        }

        return new Object[]{timestamps, values};
    }

    // ------------------------- low level convert function for better performance ----------------------------------

    private static void longToByteArray(long l, byte[] b, int offset) {
        b[7 + offset] = (byte) (l);
        l >>>= 8;
        b[6 + offset] = (byte) (l);
        l >>>= 8;
        b[5 + offset] = (byte) (l);
        l >>>= 8;
        b[4 + offset] = (byte) (l);
        l >>>= 8;
        b[3 + offset] = (byte) (l);
        l >>>= 8;
        b[2 + offset] = (byte) (l);
        l >>>= 8;
        b[1 + offset] = (byte) (l);
        l >>>= 8;
        b[offset] = (byte) (l);
    }

    private static long byteArrayToLong(byte[] b, int offset){
        return ( ( (long) b[7 + offset]) & 0xFF) +
                ( ( ( (long) b[6 + offset]) & 0xFF) << 8) +
                ( ( ( (long) b[5 + offset]) & 0xFF) << 16) +
                ( ( ( (long) b[4 + offset]) & 0xFF) << 24) +
                ( ( ( (long) b[3 + offset]) & 0xFF) << 32) +
                ( ( ( (long) b[2 + offset]) & 0xFF) << 40) +
                ( ( ( (long) b[1 + offset]) & 0xFF) << 48) +
                ( ( ( (long) b[offset]) & 0xFF) << 56);
    }


    public interface ValuePairConsumer {

        void processValuePair(long timeStamp, double value);
    }
}
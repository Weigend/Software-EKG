package io.softwareecg.collector.repository.rdb.api.datamanagement;

import io.softwareecg.collector.repository.rdb.api.TimeSeries;

/**
 * todo insert comment here
 *
 * @since 12.08.2018
 */
public class TimeSeriesValueHelper {


    public static byte[] addToByteArray(byte[] existingData, long timestamp, double value) {

        byte[] result = new byte[existingData.length + 16];
        System.arraycopy(existingData, 0, result, 0, existingData.length);

        longToByteArray(Double.doubleToLongBits(value), result, existingData.length);
        longToByteArray(timestamp, result, existingData.length + 8);

        return result;
    }

    public static byte[] mergeData(TimeSeries seriesBefore, TimeSeries seriesAfter) {

        byte[] result = new byte[seriesBefore.getData().length + seriesAfter.getData().length];
        System.arraycopy(seriesBefore.getData(), 0, result, 0, seriesBefore.getData().length);
        System.arraycopy(seriesAfter.getData(), 0, result, seriesBefore.getData().length, seriesAfter.getData().length);

        return result;
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

}

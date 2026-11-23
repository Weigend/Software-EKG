package io.softwareecg.awb.queryengine.pipeline.simplification.algorithms;

import io.softwareecg.awb.timeseries.TimeSeries;

import java.util.List;

/**
 * SimplificationService that will remove all points of a series that have an identical value
 * to the previous value. So only the changes of an series will remain.
 *
 * Operates directly on primitive arrays — no Value object allocation.
 *
 * @since 13.09.2019
 */
public class ValueChangeSimplificationService {

    public List<TimeSeries> simplify(List<TimeSeries> timeSeries) {

        for (TimeSeries series : timeSeries) {
            long[] timestamps = series.getTimestamps();
            double[] values = series.getValuesArray();
            int n = timestamps.length;

            if (n == 0) {
                continue;
            }

            // count how many points to keep
            int count = 1; // first point always kept
            double lastVal = values[0];
            for (int i = 1; i < n; i++) {
                if (values[i] != lastVal) {
                    count++;
                    lastVal = values[i];
                }
            }
            // always keep last point if different timestamp
            if (timestamps[n - 1] != timestamps[count > 0 ? findLastKeptIndex(values, n) : 0]) {
                count++;
            }

            if (count >= n) {
                continue; // nothing to simplify
            }

            // build result arrays
            long[] resultTs = new long[count];
            double[] resultVals = new double[count];
            int idx = 0;
            resultTs[0] = timestamps[0];
            resultVals[0] = values[0];
            idx = 1;
            lastVal = values[0];
            int lastKeptIdx = 0;

            for (int i = 1; i < n; i++) {
                if (values[i] != lastVal) {
                    resultTs[idx] = timestamps[i];
                    resultVals[idx] = values[i];
                    lastVal = values[i];
                    lastKeptIdx = i;
                    idx++;
                }
            }

            // ensure last point is included
            if (lastKeptIdx != n - 1 && idx < count) {
                resultTs[idx] = timestamps[n - 1];
                resultVals[idx] = values[n - 1];
                idx++;
            }

            series.setSortedArrays(java.util.Arrays.copyOf(resultTs, idx),
                    java.util.Arrays.copyOf(resultVals, idx));
        }

        return timeSeries;
    }

    private static int findLastKeptIndex(double[] values, int n) {
        double lastVal = values[0];
        int lastIdx = 0;
        for (int i = 1; i < n; i++) {
            if (values[i] != lastVal) {
                lastVal = values[i];
                lastIdx = i;
            }
        }
        return lastIdx;
    }
}

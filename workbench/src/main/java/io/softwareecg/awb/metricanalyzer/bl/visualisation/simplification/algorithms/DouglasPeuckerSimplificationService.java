package io.softwareecg.awb.metricanalyzer.bl.visualisation.simplification.algorithms;

import io.softwareecg.awb.metricanalyzer.bl.visualisation.simplification.SimplificationService;
import io.softwareecg.awb.sdk.core.log.EkgLogger;
import io.softwareecg.awb.sdk.datamodel.TimeSeries;
import jakarta.inject.Singleton;
import org.slf4j.Logger;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;

/**
 * Simplification service using a priority-queue-based Douglas-Peucker algorithm
 * that targets an exact point count per series.
 *
 * Uses a two-level approach for large series: fast pre-decimation via
 * Largest-Triangle-Three-Buckets (LTTB), then Douglas-Peucker for final refinement.
 *
 * Operates directly on primitive long[]/double[] arrays — no Value object allocation.
 *
 */
@Singleton
public class DouglasPeuckerSimplificationService implements SimplificationService {

    private static final Logger LOGGER = EkgLogger.get();

    /** Pre-decimation factor: LTTB reduces to this multiple of targetCount before DP runs */
    private static final int PRE_DECIMATION_FACTOR = 8;

    @Override
    public List<TimeSeries> simplify(List<TimeSeries> timeSeriesList, final int threshold) {

        if (threshold < 0) {
            return timeSeriesList;
        }

        timeSeriesList.parallelStream().forEach(timeSeries -> {

            if (Thread.currentThread().isInterrupted()) {
                throw new IllegalStateException("Thread interrupted.",
                        new InterruptedException());
            }

            int originalSize = timeSeries.getPointCount();

            if (originalSize <= threshold) {
                return;
            }

            long t0 = System.currentTimeMillis();
            simplifySeries(timeSeries, threshold);
            long elapsed = System.currentTimeMillis() - t0;

            LOGGER.info("Series simplified: {} -> {} points in {}ms (threshold={})",
                    originalSize, timeSeries.getPointCount(), elapsed, threshold);
        });

        return timeSeriesList;
    }

    /**
     * Simplifies a single series in-place to at most 'targetCount' points.
     * For large series, applies LTTB pre-decimation first.
     */
    private static void simplifySeries(TimeSeries timeSeries, int targetCount) {

        long[] timestamps = timeSeries.getTimestamps();
        double[] values = timeSeries.getValuesArray();

        // normalize duplicate timestamps first
        int normLen = normalizeTimestamps(timestamps, values);
        if (normLen < timestamps.length) {
            timestamps = Arrays.copyOf(timestamps, normLen);
            values = Arrays.copyOf(values, normLen);
        }

        int n = timestamps.length;

        if (n <= targetCount) {
            timeSeries.setSortedArrays(timestamps, values);
            return;
        }

        if (targetCount < 2) {
            targetCount = 2;
        }

        // Pre-decimation: if data is much larger than target, use LTTB to reduce first
        int preTarget = targetCount * PRE_DECIMATION_FACTOR;
        if (n > preTarget) {
            Object[] decimated = lttbDecimate(timestamps, values, n, preTarget);
            timestamps = (long[]) decimated[0];
            values = (double[]) decimated[1];
            n = timestamps.length;
        }

        // Douglas-Peucker on the (now manageable) data
        douglasPeucker(timeSeries, timestamps, values, n, targetCount);
    }

    /**
     * Largest-Triangle-Three-Buckets (LTTB) downsampling.
     * O(N) algorithm that preserves visual shape by selecting the point in each bucket
     * that forms the largest triangle with its neighbors.
     */
    private static Object[] lttbDecimate(long[] timestamps, double[] values, int n, int targetCount) {
        long[] resultTs = new long[targetCount];
        double[] resultVals = new double[targetCount];

        // first point always included
        resultTs[0] = timestamps[0];
        resultVals[0] = values[0];

        double bucketSize = (double) (n - 2) / (targetCount - 2);

        int prevSelected = 0;

        for (int bucket = 1; bucket < targetCount - 1; bucket++) {
            // boundaries of current bucket
            int bucketStart = (int) Math.floor((bucket - 1) * bucketSize) + 1;
            int bucketEnd = (int) Math.floor(bucket * bucketSize) + 1;
            if (bucketEnd > n - 1) bucketEnd = n - 1;

            // average of next bucket (used as the third triangle point)
            int nextBucketStart = bucketEnd;
            int nextBucketEnd = (int) Math.floor((bucket + 1) * bucketSize) + 1;
            if (nextBucketEnd > n - 1) nextBucketEnd = n - 1;

            double avgT = 0, avgV = 0;
            int nextCount = nextBucketEnd - nextBucketStart;
            if (nextCount <= 0) nextCount = 1;
            for (int i = nextBucketStart; i < nextBucketEnd; i++) {
                avgT += timestamps[i];
                avgV += values[i];
            }
            avgT /= nextCount;
            avgV /= nextCount;

            // find point in current bucket that maximizes triangle area
            double maxArea = -1;
            int maxIdx = bucketStart;
            double tA = timestamps[prevSelected];
            double vA = values[prevSelected];

            for (int i = bucketStart; i < bucketEnd; i++) {
                double area = Math.abs((tA - avgT) * (values[i] - vA)
                        - (tA - timestamps[i]) * (avgV - vA));
                if (area > maxArea) {
                    maxArea = area;
                    maxIdx = i;
                }
            }

            resultTs[bucket] = timestamps[maxIdx];
            resultVals[bucket] = values[maxIdx];
            prevSelected = maxIdx;
        }

        // last point always included
        resultTs[targetCount - 1] = timestamps[n - 1];
        resultVals[targetCount - 1] = values[n - 1];

        return new Object[]{resultTs, resultVals};
    }

    /**
     * Standard Douglas-Peucker with priority queue for exact target count.
     */
    private static void douglasPeucker(TimeSeries timeSeries, long[] timestamps, double[] values,
                                        int n, int targetCount) {
        boolean[] selected = new boolean[n];
        selected[0] = true;
        selected[n - 1] = true;
        int selectedCount = 2;

        PriorityQueue<Segment> heap = new PriorityQueue<>(Comparator.comparingDouble(s -> -s.maxDeviation));

        Segment initial = findMaxDeviation(timestamps, values, 0, n - 1);
        if (initial != null) {
            heap.add(initial);
        }

        while (selectedCount < targetCount && !heap.isEmpty()) {
            Segment seg = heap.poll();

            selected[seg.maxIndex] = true;
            selectedCount++;

            Segment left = findMaxDeviation(timestamps, values, seg.startIndex, seg.maxIndex);
            Segment right = findMaxDeviation(timestamps, values, seg.maxIndex, seg.endIndex);

            if (left != null) {
                heap.add(left);
            }
            if (right != null) {
                heap.add(right);
            }
        }

        // collect selected points into compact arrays
        long[] resultTs = new long[selectedCount];
        double[] resultVals = new double[selectedCount];
        int idx = 0;
        for (int i = 0; i < n; i++) {
            if (selected[i]) {
                resultTs[idx] = timestamps[i];
                resultVals[idx] = values[i];
                idx++;
            }
        }

        timeSeries.setSortedArrays(resultTs, resultVals);
    }

    private static Segment findMaxDeviation(long[] timestamps, double[] values, int startIndex, int endIndex) {
        if (endIndex - startIndex <= 1) {
            return null;
        }

        double tStart = timestamps[startIndex];
        double tEnd = timestamps[endIndex];
        double vStart = values[startIndex];
        double vEnd = values[endIndex];

        double maxDev = -1;
        int maxIdx = -1;
        double tRange = tEnd - tStart;

        if (tRange == 0) {
            // all timestamps identical — pick point with max deviation from midpoint
            double mid = (vStart + vEnd) / 2.0;
            for (int i = startIndex + 1; i < endIndex; i++) {
                double deviation = Math.abs(values[i] - mid);
                if (deviation > maxDev) {
                    maxDev = deviation;
                    maxIdx = i;
                }
            }
        } else {
            double invRange = 1.0 / tRange;
            for (int i = startIndex + 1; i < endIndex; i++) {
                double t = timestamps[i] - tStart;
                double interpolated = vStart + (t * invRange) * (vEnd - vStart);
                double deviation = Math.abs(values[i] - interpolated);
                if (deviation > maxDev) {
                    maxDev = deviation;
                    maxIdx = i;
                }
            }
        }

        return new Segment(startIndex, endIndex, maxIdx, maxDev);
    }

    private record Segment(int startIndex, int endIndex, int maxIndex, double maxDeviation) {
    }

    /**
     * Normalizes in-place: removes duplicate timestamps, keeping the value
     * with the highest deviation from the previous timestamp's value.
     * Returns the new length.
     */
    private static int normalizeTimestamps(long[] timestamps, double[] values) {
        int n = timestamps.length;
        if (n < 2) {
            return n;
        }

        int writeIdx = 1;
        double previousValue = values[0];
        long currentTimestamp = timestamps[0];
        int bestIdx = 0;
        double bestDiff = 0;

        for (int i = 1; i < n; i++) {
            if (timestamps[i] != currentTimestamp) {
                if (bestIdx != writeIdx - 1) {
                    timestamps[writeIdx] = timestamps[bestIdx];
                    values[writeIdx] = values[bestIdx];
                    writeIdx++;
                    previousValue = values[bestIdx];
                }
                currentTimestamp = timestamps[i];
                timestamps[writeIdx] = timestamps[i];
                values[writeIdx] = values[i];
                bestIdx = writeIdx;
                bestDiff = Math.abs(previousValue - values[i]);
                writeIdx++;
            } else {
                double diff = Math.abs(previousValue - values[i]);
                if (diff > bestDiff) {
                    bestDiff = diff;
                    bestIdx = i;
                    timestamps[writeIdx - 1] = timestamps[i];
                    values[writeIdx - 1] = values[i];
                }
            }
        }

        if (bestIdx >= writeIdx) {
            timestamps[writeIdx - 1] = timestamps[bestIdx];
            values[writeIdx - 1] = values[bestIdx];
        }

        return writeIdx;
    }
}

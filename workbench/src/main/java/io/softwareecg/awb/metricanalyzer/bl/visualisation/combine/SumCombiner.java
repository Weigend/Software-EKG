package io.softwareecg.awb.metricanalyzer.bl.visualisation.combine;

import io.softwareecg.awb.metricanalyzer.bl.api.SeriesCombineMode;
import io.softwareecg.awb.sdk.datamodel.TimeSeries;
import io.softwareecg.awb.sdk.datamodel.Value;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Combiner implementation that will sum the values of multiple
 * time series at each time they are overlapping.
 * <p>
 * For bucketed modes (per second/minute/hour/day/month), each series
 * is first averaged within each bucket, then the per-series averages
 * are summed across series. This prevents temporal accumulation from
 * inflating the result beyond the theoretical maximum.
 *
 * @since 21.07.2018
 */
public class SumCombiner extends Combiner {

    private final SeriesCombineMode combineMode;

    public SumCombiner(SeriesCombineMode combineMode) {
        this.combineMode = combineMode;
    }

    @Override
    public TimeSeries combine(String metricName, List<TimeSeries> timeSeriesList) {

        HashMap<Long, Value> resultIndex = new HashMap<>();

        // look for oldest timestamp, the granularity (as divisor parameter) and offset
        GranularityResult analyticsResult;

        if (combineMode == SeriesCombineMode.SUM_EXACT) {
            analyticsResult = analyzeSeries(timeSeriesList);
        } else {
            switch (combineMode) {
                case SUM_SEC:
                    analyticsResult = createGranularityForSec(timeSeriesList);
                    break;
                case SUM_MIN:
                    analyticsResult = createGranularityForMin(timeSeriesList);
                    break;
                case SUM_HOUR:
                    analyticsResult = createGranularityForHours(timeSeriesList);
                    break;
                case SUM_DAY:
                    analyticsResult = createGranularityForDays(timeSeriesList);
                    break;
                case SUM_MONTH:
                    analyticsResult = createGranularityForMonths(timeSeriesList);
                    break;
                default:
                    throw new IllegalArgumentException("Unexpected combine mode " + combineMode.getName());

            }
        }

        TimeSeries combinedSeries = new TimeSeries(metricName);

        // For each series: first average within buckets, then sum across series.
        // This prevents temporal accumulation when the bucket is larger than the sampling interval.
        for (TimeSeries timeSeries : timeSeriesList) {

            // merge meta data (differ filter dimensions will become a '*')
            combinedSeries.mergeMetaData(timeSeries);

            // Phase 1: Group this series' values by bucket and compute per-bucket average
            Map<Long, double[]> seriesBuckets = new HashMap<>(); // [sum, count]
            for (Value value : timeSeries.getValues()) {
                long key = computeBucketKey(value.getTimestamp(), analyticsResult);
                double[] acc = seriesBuckets.computeIfAbsent(key, k -> new double[2]);
                acc[0] += value.getValue();
                acc[1] += 1;
            }

            // Phase 2: Add per-series bucket average to result (sum across series)
            for (Map.Entry<Long, double[]> entry : seriesBuckets.entrySet()) {
                double avgInBucket = entry.getValue()[0] / entry.getValue()[1];
                long key = entry.getKey();
                if (!resultIndex.containsKey(key)) {
                    resultIndex.put(key, new Value(key, avgInBucket));
                } else {
                    resultIndex.get(key).addAndSum(avgInBucket);
                }
            }
        }

        // fill time series with sorted values and return it
        return prepareSeries(resultIndex, combinedSeries);
    }
}

package io.softwareecg.awb.query.pipeline.combine;

import io.softwareecg.awb.query.model.SeriesCombineMode;
import io.softwareecg.awb.timeseries.TimeSeries;
import io.softwareecg.awb.timeseries.Value;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Combiner implementation that will calculate the average of the values of multiple
 * time series at each timestamp they are overlapping.
 * The analysis of the overlapping will be done by creating time buckets.
 * <p>
 * For bucketed modes, each series is first averaged within each bucket,
 * then the per-series averages are averaged across series.
 *
 * @since 21.07.2018
 */
public class AvgCombiner extends Combiner {

    private final SeriesCombineMode combineMode;

    public AvgCombiner(SeriesCombineMode combineMode) {
        this.combineMode = combineMode;
    }

    @Override
    public TimeSeries combine(String newSeriesName, List<TimeSeries> timeSeriesList) {

        Map<Long, Value> resultIndex = new HashMap<>();

        // look for oldest timestamp, the granularity (as divisor parameter) and offset
        GranularityResult analyticsResult;

        if (combineMode == SeriesCombineMode.AVG_EXACT) {
            analyticsResult = analyzeSeries(timeSeriesList);
        } else {
            switch (combineMode) {
                case AVG_SEC:
                    analyticsResult = createGranularityForSec(timeSeriesList);
                    break;
                case AVG_MIN:
                    analyticsResult = createGranularityForMin(timeSeriesList);
                    break;
                case AVG_HOUR:
                    analyticsResult = createGranularityForHours(timeSeriesList);
                    break;
                case AVG_DAY:
                    analyticsResult = createGranularityForDays(timeSeriesList);
                    break;
                case AVG_MONTH:
                    analyticsResult = createGranularityForMonths(timeSeriesList);
                    break;
                default:
                    throw new IllegalArgumentException("Unexpected combiner mode " + combineMode.getName());
            }
        }

        TimeSeries combinedSeries = new TimeSeries(newSeriesName);

        // Accumulator per bucket: [sum_of_series_averages, series_count]
        Map<Long, double[]> resultAccumulator = new HashMap<>();

        // For each series: first average within buckets, then accumulate for cross-series averaging
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

            // Phase 2: Add per-series bucket average to accumulator
            for (Map.Entry<Long, double[]> entry : seriesBuckets.entrySet()) {
                double avgInBucket = entry.getValue()[0] / entry.getValue()[1];
                double[] acc = resultAccumulator.computeIfAbsent(entry.getKey(), k -> new double[2]);
                acc[0] += avgInBucket;
                acc[1] += 1;
            }
        }

        // Convert accumulator to final averaged values
        for (Map.Entry<Long, double[]> entry : resultAccumulator.entrySet()) {
            resultIndex.put(entry.getKey(),
                    new Value(entry.getKey(), entry.getValue()[0] / entry.getValue()[1]));
        }

        // fill time series with sorted values and return it
        return prepareSeries(resultIndex, combinedSeries);
    }
}

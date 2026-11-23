package io.softwareecg.awb.queryengine.pipeline.smoothing;

import com.carrotsearch.hppc.DoubleArrayList;
import io.softwareecg.awb.investigation.model.SeriesSmoothingGranularity;
import io.softwareecg.awb.timeseries.Value;

import java.util.*;

/**
 * Implements a series smoothing by use the median of all values inside
 * a bucket (time interval) of the given time series.
 *
 * @since 23.07.2018
 */
public class MedianTimeSeriesSmoother extends AbstractTimeSeriesSmoother {

    /**
     * Constructs a new instance of MedianTimeSeriesSmoother which will use
     * the granularity of timestamp buckets for computing the series smoothing
     *
     * @param smoothingGranularity an SeriesSmoothingGranularity enum that specifies the granularity
     */
    protected MedianTimeSeriesSmoother(SeriesSmoothingGranularity smoothingGranularity) {
        super(smoothingGranularity);
    }

    /* (non-Javadoc)
     * @see io.softwareecg.awb.queryengine.pipeline.smoothing.AbstractTimeSeriesSmoother#computeValues(...)
     */
    @Override
    protected List<Value> computeSeriesValues(Map<Long, List<Value>> bucketValueMap) {

        List<Value> resultList = new ArrayList<>(bucketValueMap.size());

        for (Map.Entry<Long, List<Value>> bucket : bucketValueMap.entrySet()) {

            DoubleArrayList doubleList = DoubleArrayList.from();

            for (Value bucketValue : bucket.getValue()) {
                doubleList.add(bucketValue.getValue());
            }

            double median = calcMedian(doubleList.toArray());
            resultList.add(new Value(bucket.getKey(), median));
        }

        Collections.sort(resultList);

        return resultList;
    }

    private static double calcMedian(double[] valuesOfBucket) {
        Arrays.sort(valuesOfBucket);

        double median;
        if (valuesOfBucket.length % 2 == 0) {
            median = (valuesOfBucket[valuesOfBucket.length / 2] + valuesOfBucket[valuesOfBucket.length / 2 - 1]) / 2;
        }else {
            median = valuesOfBucket[valuesOfBucket.length / 2];
        }

        return median;
    }
}

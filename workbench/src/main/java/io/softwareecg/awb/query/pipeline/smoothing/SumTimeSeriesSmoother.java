package io.softwareecg.awb.query.pipeline.smoothing;

import io.softwareecg.awb.query.model.SeriesSmoothingGranularity;
import io.softwareecg.awb.timeseries.Value;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Implements a series smoothing by accumulate all values inside
 * a bucket (time interval) of the given time series.
 *
 * @since 23.07.2018
 */
public class SumTimeSeriesSmoother extends AbstractTimeSeriesSmoother {

    /**
     * Constructs a new instance of SumTimeSeriesSmoother which will use
     * the granularity of timestamp buckets for computing the series smoothing
     *
     * @param smoothingGranularity an SeriesSmoothingGranularity enum that specifies the granularity
     */
    protected SumTimeSeriesSmoother(SeriesSmoothingGranularity smoothingGranularity) {
        super(smoothingGranularity);
    }

    /* (non-Javadoc)
     * @see io.softwareecg.awb.query.pipeline.smoothing.AbstractTimeSeriesSmoother#computeValues(...)
     */
    @Override
    protected List<Value> computeSeriesValues(Map<Long, List<Value>> bucketValueMap) {

        List<Value> resultList = new ArrayList<>(bucketValueMap.size());

        for (Map.Entry<Long, List<Value>> bucket : bucketValueMap.entrySet()) {

            Value value = new Value(bucket.getKey(), 0);

            for (Value bucketValue : bucket.getValue()) {
                value.addAndSum(bucketValue.getValue());
            }

            resultList.add(value);
        }

        Collections.sort(resultList);

        return resultList;
    }
}

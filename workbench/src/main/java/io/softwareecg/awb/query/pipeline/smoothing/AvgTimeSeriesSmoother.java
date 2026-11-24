package io.softwareecg.awb.query.pipeline.smoothing;

import com.carrotsearch.hppc.DoubleArrayList;
import io.softwareecg.awb.query.api.SeriesSmoothingGranularity;
import io.softwareecg.awb.timeseries.Value;
import org.apache.commons.math3.stat.StatUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Implements a series smoothing by use the average of all values inside
 * a bucket (time interval) of the given time series.
 *
 * @since 23.07.2018
 */
public class AvgTimeSeriesSmoother extends AbstractTimeSeriesSmoother {

    /**
     * Constructs a new instance of AvgTimeSeriesSmoother which will use
     * the granularity of timestamp buckets for computing the series smoothing
     *
     * @param smoothingGranularity an SeriesSmoothingGranularity enum that specifies the granularity
     */
    protected AvgTimeSeriesSmoother(SeriesSmoothingGranularity smoothingGranularity) {
        super(smoothingGranularity);
    }

    /* (non-Javadoc)
     * @see io.softwareecg.awb.query.pipeline.smoothing.AbstractTimeSeriesSmoother#computeValues(...)
     */
    @Override
    protected List<Value> computeSeriesValues(Map<Long, List<Value>> bucketValueMap) {

        List<Value> resultList = new ArrayList<>(bucketValueMap.size());

        for (Map.Entry<Long, List<Value>> bucket : bucketValueMap.entrySet()) {

            DoubleArrayList doubleList = DoubleArrayList.from();

            for (Value bucketValue : bucket.getValue()) {
                doubleList.add(bucketValue.getValue());
            }

            double average = StatUtils.mean(doubleList.buffer, 0, doubleList.elementsCount);
            resultList.add(new Value(bucket.getKey(), average));
        }

        Collections.sort(resultList);

        return resultList;
    }
}

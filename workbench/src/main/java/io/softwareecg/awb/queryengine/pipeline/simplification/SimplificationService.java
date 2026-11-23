//______________________________________________________________________________
//
//                  Project:    Software EKG
//______________________________________________________________________________
//
//                   Author:    Weigend AM 2009 - 2017
//______________________________________________________________________________
//
// Notice: This piece of software was created, designed and implemented by
// experienced craftsmen and innovators in Munich, Germany.
// Changes should be done with respect to the original design.
//______________________________________________________________________________
package io.softwareecg.awb.queryengine.pipeline.simplification;

import io.softwareecg.awb.timeseries.TimeSeries;

import java.util.List;

/**
 * Interface that represents service that simplify time series
 * to improve the rendering performance.
 * In the most common cases this will do by some type of vectorization.
 */
@FunctionalInterface
public interface SimplificationService {

    /**
     * Reduce the amount of data points in each individual time series to a maximum defined
     * by the threshold parameter. Each series is treated independently — if a single series
     * has fewer points than the threshold it will be kept untouched.
     *
     * @param timeSeries the time series to vectorize
     * @param threshold  maximum number of data points per individual series;
     *                   0 indicates that no simplification should be started
     * @return vectorized time series
     */
    List<TimeSeries> simplify(List<TimeSeries> timeSeries, int threshold);
}

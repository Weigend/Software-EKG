//______________________________________________________________________________
//
//          Project:    Software EKG
//______________________________________________________________________________
//
//         Author:      Weigend AM 2009 - 2014
//______________________________________________________________________________
//
// Notice: This piece of software was created, designed and implemented by
// experienced craftsmen and innovators in Munich, Germany.
// Changes should be done with respect to the original design.
//______________________________________________________________________________
package io.softwareecg.awb.query.model;

import io.softwareecg.awb.runtime.NamedEnum;

/**
 * ValueType defines how the series values are aggregated.
 *
 */
public enum SeriesSmoothingType implements NamedEnum {

    /**
     * The Smoother will returned the untouched value list
     */
    NONE ("Inactive", "the values as they were measured"),

    /**
     * smooth each series by calculating the average value
     * of each series value in a defined time slice
     */
    AVG ("Average", "the average of the values of every interval"),

    /**
     * The MIN.
     */
    MIN ("Min", "the smallest value of every interval"),

    /**
     * The MAX.
     */
    MAX ("Max", "the largest value of every interval"),

    /**
     * The accumulated value of all values in a bucket of time series
     */
    SUM ("Sum", "the sum of the values of every interval, for values that are counts"),

    /**
     * The median value of all values in the granularity range.
     */
    MEDIAN ("Median", "the median of the values of every interval"),

    /**
     * The amount of all values points (per time interval) independent of the individual values
     */
    VALUE_COUNT ("Value Count", "how many values every interval has, for series in which a value is an event"),

    /**
     * The difference of the sum of all values inside the defined time interval to the
     * previous time interval (or to zero if there is no previous interval)
     */
    DIFF ("Value Difference", "the change of the sum of the values against the interval before, for counters that only grow");


    private final String name;

    /** what the smoothing shows instead of the single values */
    private final String effect;

    SeriesSmoothingType(String name, String effect) {
        this.name = name;
        this.effect = effect;
    }

    @Override
    public String getDescription() {
        return name + ": " + effect;
    }

    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return super.toString();
    }
}

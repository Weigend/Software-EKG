package io.softwareecg.awb.metricanalyzer.bl.api;

import io.softwareecg.awb.sdk.core.NamedEnum;

/**
 * Enumeration that represents the different modes of
 * time series combining
 *
 * @since 21.07.2018
 */
public enum SeriesCombineMode implements NamedEnum {

    /**
     * inactive, no combine of any series
     */
    NONE ("Inactive"),

    /**
     * Similar series that are identically in each filter dimension except
     * the measurement will concat to a single long series.
     *
     * Result: a set of unique time series with no measurement value because it used as combined
     */
    CONCAT ("Concatenate"),

    /**
     * All series will merge together by adding values in the same slices.
     * If multiple series exists in the same time interval it will sum.
     * Series in different time intervals will just concat.
     *
     * Result: a single time series with the sum of all input series at each covered time slot
     */
    SUM_EXACT("Sum (Exact)"),

    SUM_SEC("Sum (per Second)"),

    SUM_MIN("Sum (per Minute)"),

    SUM_HOUR("Sum (per Hour)"),

    SUM_DAY("Sum (per Day)"),

    SUM_MONTH("Sum (per Month)"),

    /**
     * All series will merge together by calculate the average value of each series point in
     * the same time slices and concat values in different slices.
     *
     * Result: a single time series with the average of all input series at each covered time slot
     */
    AVG_EXACT("Average (Exact)"),

    AVG_SEC("Average (per Second)"),

    AVG_MIN("Average (per Minute)"),

    AVG_HOUR("Average (per Hour)"),

    AVG_DAY("Average (per Day)"),

    AVG_MONTH("Average (per Month)");

    /**
     * The readable name of the enumeration
     */
    private final String name;

    /**
     * Internal constructor that creates enum of this type
     * with the readable name that represents the enum value.
     *
     * @param name the readable name that represents the enum value
     */
    SeriesCombineMode(String name) {
        this.name = name;
    }

    /**
     * Returns the readable name that represents the enum value
     *
     * @return the alias name of the enumeration
     */
    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return super.toString();
    }
}

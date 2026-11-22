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
    NONE ("Inactive", "every part of a series is a line of its own, one for each measurement"),

    /**
     * Similar series that are identically in each filter dimension except
     * the measurement will concat to a single long series.
     *
     * Result: a set of unique time series with no measurement value because it used as combined
     */
    CONCAT ("Concatenate", "the parts a series has in several measurements are joined to one line"),

    /**
     * All series will merge together by adding values in the same slices.
     * If multiple series exists in the same time interval it will sum.
     * Series in different time intervals will just concat.
     *
     * Result: a single time series with the sum of all input series at each covered time slot
     */
    SUM_EXACT("Sum (Exact)", "one series instead of all: the sum of their values at the same instant"),

    SUM_SEC("Sum (per Second)", "one series instead of all: the sum of their values in every second"),

    SUM_MIN("Sum (per Minute)", "one series instead of all: the sum of their values in every minute"),

    SUM_HOUR("Sum (per Hour)", "one series instead of all: the sum of their values in every hour"),

    SUM_DAY("Sum (per Day)", "one series instead of all: the sum of their values on every day"),

    SUM_MONTH("Sum (per Month)", "one series instead of all: the sum of their values in every month"),

    /**
     * All series will merge together by calculate the average value of each series point in
     * the same time slices and concat values in different slices.
     *
     * Result: a single time series with the average of all input series at each covered time slot
     */
    AVG_EXACT("Average (Exact)", "one series instead of all: the average of their values at the same instant"),

    AVG_SEC("Average (per Second)", "one series instead of all: the average of their values in every second"),

    AVG_MIN("Average (per Minute)", "one series instead of all: the average of their values in every minute"),

    AVG_HOUR("Average (per Hour)", "one series instead of all: the average of their values in every hour"),

    AVG_DAY("Average (per Day)", "one series instead of all: the average of their values on every day"),

    AVG_MONTH("Average (per Month)", "one series instead of all: the average of their values in every month");

    /**
     * The readable name of the enumeration
     */
    private final String name;

    /**
     * What the mode does to the series of a chart
     */
    private final String effect;

    /**
     * Internal constructor that creates enum of this type
     * with the readable name that represents the enum value.
     *
     * @param name the readable name that represents the enum value
     * @param effect what the mode does to the series of a chart, for the tooltip
     */
    SeriesCombineMode(String name, String effect) {
        this.name = name;
        this.effect = effect;
    }

    @Override
    public String getDescription() {
        return name + ": " + effect;
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

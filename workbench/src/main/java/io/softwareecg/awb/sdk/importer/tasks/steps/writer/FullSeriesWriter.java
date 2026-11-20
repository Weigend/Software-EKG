package io.softwareecg.awb.sdk.importer.tasks.steps.writer;

import io.softwareecg.awb.sdk.datamodel.SeriesData;

/**
 * Interface that represents a specialized type of SeriesWriter implementation that
 * will always proceed a full load of time-series data to the EKG types.
 * Existing series will overwritten completely or skipped.
 *
 * @since 14.07.2018
 */
public interface FullSeriesWriter<TIN extends SeriesData, TOUT extends SeriesData> extends SeriesWriter<TIN, TOUT> {

    /**
     * Set a flag that indicates if existing time-series data
     * should overwritten or not.
     *
     * @param doOverwrite <code>true</code> if the write should overwrite existing time series data.
     */
    void overwriteExistingSeries(boolean doOverwrite);
}
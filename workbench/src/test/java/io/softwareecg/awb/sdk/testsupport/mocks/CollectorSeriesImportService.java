package io.softwareecg.awb.sdk.testsupport.mocks;

import io.softwareecg.awb.sdk.awbapi.repository.SeriesImportService;
import io.softwareecg.awb.sdk.datamodel.TimeSeries;

import java.util.List;

/**
 * An extended SeriesImportService interface that has one additional
 * method that serves in-memory collected series data that was written to the service.
 *
 * @since 24.03.2020
 */
public interface CollectorSeriesImportService extends SeriesImportService {

    /**
     * Return the in-memory collected series data that was
     * written to the services.
     *
     * @return the time series data as list
     */
    List<TimeSeries> getCollectedSeriesData();
}

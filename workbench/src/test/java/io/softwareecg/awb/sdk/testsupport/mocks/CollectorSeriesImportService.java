package io.softwareecg.awb.sdk.testsupport.mocks;

import io.softwareecg.awb.timeseries.port.TimeSeriesWriteService;
import io.softwareecg.awb.timeseries.TimeSeries;

import java.util.List;

/**
 * An extended TimeSeriesWriteService interface that has one additional
 * method that serves in-memory collected series data that was written to the service.
 *
 * @since 24.03.2020
 */
public interface CollectorSeriesImportService extends TimeSeriesWriteService {

    /**
     * Return the in-memory collected series data that was
     * written to the services.
     *
     * @return the time series data as list
     */
    List<TimeSeries> getCollectedSeriesData();
}

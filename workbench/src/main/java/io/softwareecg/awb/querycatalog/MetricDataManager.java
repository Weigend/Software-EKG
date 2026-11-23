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
package io.softwareecg.awb.querycatalog;

import io.softwareecg.awb.queryengine.MetricDataAccessService;
import io.softwareecg.awb.repository.api.EkgRepository;
import io.softwareecg.awb.repository.port.RepositoryException;
import io.softwareecg.awb.timeseries.TimeSeries;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.stream.Stream;


/**
 * Import the as time series stored metrics into solr.
 *
 */
public class MetricDataManager implements MetricsManagementService {

    private static final Logger LOGGER = LoggerFactory.getLogger(MetricDataManager.class);

    private MetricDataAccessService metricDataAccess;

    public void setRepository(EkgRepository repository) {
        this.metricDataAccess = repository.getBoundedService(MetricDataAccessService.class);
    }

    /**
     * todo - do something with the overrideExistingSeries that also didn't handled in the original code
     *
     * @param timeSeriesStream the TimeSeries as stream
     * @param overrideExistingSeries TRUE to override existing time series, otherwise false
     * @throws RepositoryException exception if the types isn't available or an error occurred during write process
     */
    @Override
    public void importTimeSeries(Stream<TimeSeries> timeSeriesStream, boolean overrideExistingSeries) throws RepositoryException {

        try {
            metricDataAccess.addEntities(timeSeriesStream, overrideExistingSeries);

        } catch (RepositoryException e) {
            LOGGER.error("error on import time series with exception " + e);
            throw new RepositoryException(e.getMessage());
        }
    }

    @Override
    public void commit() {
        metricDataAccess.commitOrRollback();
    }
}

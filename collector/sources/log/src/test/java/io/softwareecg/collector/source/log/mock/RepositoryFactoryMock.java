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
package io.softwareecg.collector.source.log.mock;

import io.softwareecg.collector.config.CounterDescriptor;
import io.softwareecg.collector.dt.TimeSeriesHeader;
import io.softwareecg.collector.repository.RepositoryFactory;
import io.softwareecg.collector.repository.StaticTimeSeriesRepository;

import java.util.Date;

/**
 * Created by martin.binder on 10.05.2014.
 */
public class RepositoryFactoryMock implements RepositoryFactory {


    @Override
    public StaticTimeSeriesRepository createStaticRepository(CounterDescriptor descriptor) {
        return new StaticTimeSeriesRepository() {

            @Override
            public void createMeasurement(TimeSeriesHeader timeSeriesHeader, String[] metricLabels) {

            }

            @Override
            public void addValueSetToMeasurement(Date captureTime, Double[] metricValues) {

            }

            @Override
            public void flush() {

            }
        };
    }
}

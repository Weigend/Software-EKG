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
package io.softwareecg.collector.repository.csv;

import io.softwareecg.collector.config.TestConfigFactory;
import io.softwareecg.collector.dt.CollectorJobConfiguration;
import io.softwareecg.collector.repository.StaticTimeSeriesRepository;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Created by f.lautenschlager on 09.04.14.
 */
public class CsvRepositoryFactoryTest {


    @Test
    public void testCreateStorage() {
        CollectorJobConfiguration config = TestConfigFactory.createTestConfig();
        StaticTimeSeriesRepository storage = new CsvRepositoryFactory().createStaticRepository(config);
        assertThat(storage instanceof Csv4jRepository, is(true));
    }

    @Test
    public void testCreateStorageAdditionalInfoIsNull() {
        CollectorJobConfiguration config = TestConfigFactory.createTestConfig();
        StaticTimeSeriesRepository storage = new CsvRepositoryFactory().createStaticRepository(config);
        assertThat(storage, notNullValue());
    }

    @Test
    public void testCreateStorageWithAdditionalInfo() {
        CollectorJobConfiguration config = TestConfigFactory.createTestConfig();
        StaticTimeSeriesRepository storage = new CsvRepositoryFactory().createStaticRepository(config);
        assertThat(storage, notNullValue());
    }


    @Test
    public void testCreateStorageEscapeName() {
        CollectorJobConfiguration config = TestConfigFactory.createTestConfig("=");
        StaticTimeSeriesRepository archiveStorage = new CsvRepositoryFactory().createStaticRepository(config);
        assertThat(archiveStorage, notNullValue());
    }
}

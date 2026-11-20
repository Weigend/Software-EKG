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
package io.softwareecg.collector.repository.json;

import io.softwareecg.collector.config.TestConfigFactory;
import io.softwareecg.collector.repository.StaticTimeSeriesRepository;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 */
public class JsonRepositoryFactoryTest {

    @Test
    public void testCreateStorage() throws Exception {
        StaticTimeSeriesRepository storage = new JsonRepositoryFactory().createStaticRepository(TestConfigFactory.createTestConfig());
        assertThat(storage instanceof Json4jRepository, is(true));
    }

    @Test
    public void testCreateStorageAdditionalInfoIsNull() throws Exception {
        StaticTimeSeriesRepository storage = new JsonRepositoryFactory().createStaticRepository(TestConfigFactory.createTestConfig());
        assertThat(storage, notNullValue());
    }

    @Test
    public void testCreateStorageWithAdditionalInfo() throws Exception {
        StaticTimeSeriesRepository storage = new JsonRepositoryFactory().createStaticRepository(TestConfigFactory.createTestConfig());
        assertThat(storage, notNullValue());
    }


    @Test
    public void testCreateStorageEscapeName() throws Exception {
        StaticTimeSeriesRepository archiveStorage = new JsonRepositoryFactory().createStaticRepository(TestConfigFactory.createTestConfig("="));
        assertThat(archiveStorage, notNullValue());
    }
}

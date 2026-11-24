package io.softwareecg.awb.repository.adapter;

import io.avaje.inject.BeanScope;
import io.softwareecg.awb.projectcontext.model.Project;
import io.softwareecg.awb.query.MetricDataAccessServiceImpl;
import io.softwareecg.awb.query.QueryRunner;
import io.softwareecg.awb.query.api.MetricDataAccessService;
import io.softwareecg.awb.query.api.MetricQueryService;
import io.softwareecg.awb.query.model.ComputationSpec;
import io.softwareecg.awb.query.model.QuerySpec;
import io.softwareecg.awb.query.pipeline.SeriesTransformPipeline;
import io.softwareecg.awb.repository.api.RepositoryException;
import io.softwareecg.awb.repository.persistence.TimeSeriesRepository;
import io.softwareecg.awb.repository.connection.TimeSeriesRepositoryProvider;
import io.softwareecg.wfx.lookup.api.Lookup;
import io.softwareecg.wfx.lookup.api.LookupStrategy;
import io.softwareecg.wfx.lookup.api.TypeRef;
import io.softwareecg.wfx.platform.api.EventBus;
import io.softwareecg.wfx.platform.core.eventbus.SimpleEventBus;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * A service of a repository works on that repository, whichever was opened after it. The
 * injection knows one bean of every service, so this does not come by itself.
 */
public class BoundServiceTest {

    private final Map<Class<?>, Object> beans = new HashMap<>();

    @Before
    @SuppressWarnings("unchecked")
    public void startLookup() {
        EventBus<java.util.EventObject> eventBus = new SimpleEventBus();
        SeriesTransformPipeline pipeline = mock(SeriesTransformPipeline.class);
        beans.put(EventBus.class, eventBus);
        beans.put(BeanScope.class, mock(BeanScope.class));
        beans.put(SeriesTransformPipeline.class, pipeline);
        beans.put(MetricDataAccessService.class, new MetricDataAccessServiceImpl());
        beans.put(MetricQueryService.class, new QueryRunner(eventBus, pipeline));

        Lookup.init(new LookupStrategy() {
            @Override
            public <T> T lookup(Class<T> clazz) {
                return clazz.cast(beans.get(clazz));
            }

            @Override
            public <T> T lookup(TypeRef<T> type) {
                return null;
            }

            @Override
            public <T> List<T> lookupAll(Class<T> clazz) {
                T bean = lookup(clazz);
                return bean == null ? List.of() : List.of(bean);
            }

            @Override
            public <T> List<T> lookupAll(TypeRef<T> type) {
                return List.of();
            }
        });
    }

    @After
    public void shutdownLookup() {
        Lookup.shutdown();
    }

    @Test
    public void readsTheRepositoryItBelongsToAfterAnotherWasOpened() throws Exception {
        TestRepository first = new TestRepository("first", holding(1));
        TestRepository second = new TestRepository("second", holding(2));

        assertEquals(1, first.getBoundedService(MetricDataAccessService.class).getAmountMeasuredPointsInProject("p"));
        assertEquals(2, second.getBoundedService(MetricDataAccessService.class).getAmountMeasuredPointsInProject("p"));
        assertEquals(1, first.getBoundedService(MetricDataAccessService.class).getAmountMeasuredPointsInProject("p"));
    }

    @Test
    public void queriesTheRepositoryItBelongsToAfterAnotherWasOpened() throws Exception {
        TimeSeriesRepository firstClient = failingToSearch();
        MetricQueryService first = new TestRepository("first", firstClient).getBoundedService(MetricQueryService.class);
        new TestRepository("second", failingToSearch()).getBoundedService(MetricQueryService.class);

        first.getComputedTimeSeries(new QuerySpec.Builder().withProject(new Project("p")).build(),
                new ComputationSpec(), 10);

        verify(firstClient).search(any(), any());
    }

    @Test
    public void isCreatedOnceForARepository() throws Exception {
        TestRepository repository = new TestRepository("first", holding(1));

        assertSame(repository.getBoundedService(MetricDataAccessService.class),
                repository.getBoundedService(MetricDataAccessService.class));
    }

    @Test
    public void isMissingWhileTheRepositoryCannotBeConnected() {
        assertNull(new TestRepository("offline", null).getBoundedService(MetricDataAccessService.class));
    }

    private static TimeSeriesRepository holding(long values) throws RepositoryException {
        TimeSeriesRepository client = mock(TimeSeriesRepository.class);
        when(client.sumFieldValue(any(), any())).thenReturn(values);
        return client;
    }

    private static TimeSeriesRepository failingToSearch() throws RepositoryException {
        TimeSeriesRepository client = mock(TimeSeriesRepository.class);
        when(client.search(any(), any())).thenThrow(new RepositoryException("no search in this test"));
        return client;
    }

    /** Without a client it is a repository whose server does not answer. */
    private static class TestRepository extends RepositoryAdapterBase {

        private final TimeSeriesRepository client;

        TestRepository(String id, TimeSeriesRepository client) {
            this.id = id;
            this.repositoryName = id;
            this.client = client;
        }

        @Override
        public TimeSeriesRepository getRepositoryClient() {
            if (client == null) {
                throw new IllegalStateException("connection refused");
            }
            return client;
        }

        @Override
        protected TimeSeriesRepositoryProvider getRepositoryClientFactory() {
            return null;
        }
    }
}

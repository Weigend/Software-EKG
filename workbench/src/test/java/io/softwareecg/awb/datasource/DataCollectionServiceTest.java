package io.softwareecg.awb.datasource;

import io.softwareecg.awb.metadata.port.CollectionSourceEvent;
import io.softwareecg.awb.metadata.model.CollectionSourceRow;
import io.softwareecg.awb.metadata.port.CollectionSourceStore;
import io.softwareecg.awb.secrets.port.SecretStore;
import io.softwareecg.awb.datasource.model.DataSourceInfo.State;
import io.softwareecg.awb.datasource.adapter.prometheus.MetricExpression;
import io.softwareecg.awb.projectcontext.api.ProjectDataAccessService;
import io.softwareecg.awb.projectcontext.api.ProjectDataCollection;
import io.softwareecg.awb.projectcontext.model.Project;
import io.softwareecg.awb.repository.api.EkgRepository;
import io.softwareecg.awb.projectcontext.model.CloudPlatformType;
import io.softwareecg.awb.projectcontext.model.ProjectFlavor;
import io.softwareecg.awb.timeseries.port.TimeSeriesWriteService;
import io.softwareecg.collector.config.CollectorConfiguration;
import io.softwareecg.collector.control.CollectorEngine;
import io.softwareecg.collector.repository.RepositoryFactory;
import org.junit.Before;
import org.junit.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.softwareecg.awb.datasource.NewAnalysisService;
import io.softwareecg.awb.datasource.model.AnalysisPlan;
import io.softwareecg.awb.datasource.model.DataSourceInfo;
import io.softwareecg.awb.datasource.model.SeriesEnds;
/**
 * Starting an analysis and managing its data sources. The collector engines are not really
 * started here; what the service hands to them is recorded.
 */
public class DataCollectionServiceTest {

    private final List<String> projectNames = new ArrayList<>();
    private final InMemorySources sources = new InMemorySources();
    private final Map<String, String> secrets = new HashMap<>();
    private final List<CollectorConfiguration> startedWith = new ArrayList<>();
    private boolean engineFails;

    private NewAnalysisService service;

    @Before
    public void setUp() {
        ProjectDataAccessService projects = mock(ProjectDataAccessService.class);
        when(projects.listProjects()).thenAnswer(call -> projectNames.stream().map(Project::new).collect(Collectors.toList()));
        when(projects.persistProject(any())).thenAnswer(call -> {
            Project project = call.getArgument(0);
            projectNames.add(project.getName());
            return project;
        });
        doAnswer(call -> projectNames.remove(call.<String>getArgument(0))).when(projects).deleteProjectByName(anyString());

        EkgRepository repository = mock(EkgRepository.class);
        when(repository.getId()).thenReturn("local");
        when(repository.getBoundedService(ProjectDataAccessService.class)).thenReturn(projects);

        SecretStore secretStore = new SecretStore() {
            @Override
            public void store(String reference, String secret) {
                secrets.put(reference, secret);
            }

            @Override
            public String read(String reference) {
                return secrets.get(reference);
            }

            @Override
            public void delete(String reference) {
                secrets.remove(reference);
            }
        };

        service = new NewAnalysisService(repository, sources, secretStore) {
            @Override
            protected CollectorEngine startEngine(CollectorConfiguration configuration, RepositoryFactory output) {
                if (engineFails) {
                    throw new IllegalStateException("no threads left");
                }
                startedWith.add(configuration);
                return new CollectorEngine(output, true);   // created, not running
            }

            @Override
            protected TimeSeriesWriteService importService() {
                return mock(TimeSeriesWriteService.class);
            }

            @Override
            protected SeriesEnds seriesEnds() {
                return (project, measurement, series) -> 0;
            }
        };
    }

    @Test
    public void startingAnAnalysisCreatesProjectAndSourceAndKeepsTheSecretApart() {
        Project project = service.startAnalysis(plan("Shop", "s3cret"));

        assertEquals("Shop", project.getName());
        assertEquals(List.of("Shop"), projectNames);
        assertEquals("the project is complete: views that show it rely on flavor and platform type",
                ProjectFlavor.CLASSIC, project.getProjectFlavor());
        assertEquals(CloudPlatformType.NONE, project.getCloudPlatformType());

        CollectionSourceRow source = sources.rows.get(0);
        assertEquals("Shop", source.projectName);
        assertEquals("prometheus", source.sourceType);
        assertEquals(NewAnalysisService.EXECUTION_EMBEDDED, source.executionMode);
        assertEquals("http://prometheus.example:9090", source.endpoint);
        assertTrue(source.enabled);
        assertFalse("the stored configuration holds no secret", source.config.contains("s3cret"));
        assertEquals("s3cret", secrets.get(source.secretRef));

        assertEquals("the engine gets the secret filled in",
                "s3cret", startedWith.get(0).getClassicCollectorJobConfiguration().get(0).getPassword());
        assertFalse("what can be exported holds no secret",
                service.exportCollectorConfig(source.id).contains("s3cret"));
    }

    @Test
    public void failedStartLeavesNothingBehindAndTheNameFree() {
        engineFails = true;
        try {
            service.startAnalysis(plan("Shop", "s3cret"));
            fail("the failure must be reported");
        } catch (IllegalStateException expected) {
            assertTrue(expected.getMessage(), expected.getMessage().contains("no threads left"));
        }
        assertTrue("no project is left behind", projectNames.isEmpty());
        assertTrue("no source is left behind", sources.rows.isEmpty());
        assertTrue("no secret is left behind", secrets.isEmpty());

        engineFails = false;
        service.startAnalysis(plan("Shop", "s3cret"));
        assertEquals(List.of("Shop"), projectNames);
    }

    @Test
    public void nameOfAnExistingAnalysisIsRejectedBeforeAnythingIsCreated() {
        service.startAnalysis(plan("Shop", null));
        try {
            service.startAnalysis(plan("shop", null));
            fail("duplicate name accepted");
        } catch (IllegalArgumentException expected) {
            // fine
        }
        assertEquals(1, sources.rows.size());
    }

    @Test
    public void anAnalysisCanHaveSeveralSources() {
        service.startAnalysis(plan("Shop", null));
        String second = service.addSource(new AnalysisPlan("Shop", "http://other.example:9090", null, "token",
                plan("Shop", null).groups(), Duration.ofHours(1), Duration.ofSeconds(60)));

        assertEquals(2, sources.listByProject("Shop").size());
        assertEquals(List.of("http://prometheus.example:9090", "http://other.example:9090"),
                service.sources().stream().map(DataSourceInfo::endpoint).collect(Collectors.toList()));
        assertEquals("token", secrets.get(sources.find(second).secretRef));

        try {
            service.addSource(plan("Unknown", null));
            fail("a source needs an existing analysis");
        } catch (IllegalArgumentException expected) {
            // fine
        }
    }

    @Test
    public void everySourceOfAnAnalysisStoresItsSeriesUnderANameOfItsOwn() {
        service.startAnalysis(plan("Shop", null));
        String second = service.addSource(new AnalysisPlan("Shop", "http://other.example:9090", null, null,
                plan("Shop", null).groups(), Duration.ofHours(1), Duration.ofSeconds(60)));
        String third = service.addSource(new AnalysisPlan("Shop", "https://other.example/prometheus", null, null,
                plan("Shop", null).groups(), Duration.ofHours(1), Duration.ofSeconds(60)));

        assertEquals("the first source keeps the plain name", "live", sources.listByProject("Shop").get(0).seriesNamespace);
        assertEquals("live-other.example", sources.find(second).seriesNamespace);
        assertEquals("live-other.example-2", sources.find(third).seriesNamespace);

        // the name is given once: a new address does not move the series of a source
        service.updateConnection(second, "http://moved.example:9090", null, null);
        assertEquals("live-other.example", sources.find(second).seriesNamespace);

        // another analysis starts with the plain name again
        service.startAnalysis(plan("Billing", null));
        assertEquals("live", sources.listByProject("Billing").get(0).seriesNamespace);
    }

    @Test
    public void pausedSourceStaysStoredAndResumes() {
        service.startAnalysis(plan("Shop", null));
        String id = sources.rows.get(0).id;

        service.pause(id);
        assertFalse(sources.find(id).enabled);
        assertEquals(State.PAUSED, service.sources().get(0).state());

        service.resume(id);
        assertTrue(sources.find(id).enabled);
        assertEquals("resuming starts the engine again", 2, startedWith.size());
    }

    @Test
    public void changingTheConnectionKeepsWhatIsCollectedAndReconnects() {
        service.startAnalysis(plan("Shop", "old"));
        CollectionSourceRow source = sources.rows.get(0);

        service.updateConnection(source.id, "https://new.example:9090", "analyst", "new");

        assertEquals("https://new.example:9090", sources.find(source.id).endpoint);
        assertEquals("new", secrets.get(source.secretRef));
        var job = startedWith.get(startedWith.size() - 1).getClassicCollectorJobConfiguration().get(0);
        assertEquals("https://new.example:9090", job.getNodeAddress());
        assertEquals("analyst", job.getUsername());
        assertEquals("new", job.getPassword());
        assertEquals("up{job=\"node\"}", job.getMetricDefinitions()[0].getDefinition());

        // null keeps the stored secret, blank removes it
        service.updateConnection(source.id, "https://new.example:9090", "analyst", null);
        assertEquals("new", secrets.get(source.secretRef));
        service.updateConnection(source.id, "https://new.example:9090", null, "");
        assertTrue(secrets.isEmpty());
        assertNull(sources.find(source.id).secretRef);
    }

    @Test
    public void removingASourceForgetsItsSecretButKeepsTheAnalysis() {
        service.startAnalysis(plan("Shop", "s3cret"));
        String id = sources.rows.get(0).id;

        service.remove(id);

        assertTrue(sources.rows.isEmpty());
        assertTrue(secrets.isEmpty());
        assertEquals(List.of("Shop"), projectNames);
    }

    @Test
    public void sourceThatCannotStartShowsWhyAndDoesNotBlockTheOthers() {
        service.startAnalysis(plan("Shop", "s3cret"));
        service.startAnalysis(plan("Billing", null));
        service.stopAll();
        secrets.clear();   // e.g. the secret store was emptied

        service.startAll();

        Map<String, DataSourceInfo> byProject = service.sources().stream()
                .collect(Collectors.toMap(DataSourceInfo::projectName, info -> info));
        assertEquals(State.ERROR, byProject.get("Shop").state());
        assertTrue(byProject.get("Shop").detail(), byProject.get("Shop").detail().contains("credentials"));
        assertEquals("the other source was started", 3, startedWith.size());

        String shopId = byProject.get("Shop").id();
        assertEquals("the problem is in the history of the source",
                State.ERROR.name(), service.history(shopId, 10).get(0).state());
    }

    @Test
    public void aProjectTellsWhetherDataIsCollectedForIt() {
        assertEquals("a project without sources collects nothing",
                ProjectDataCollection.State.NONE, service.statusOf("Shop").state());

        service.startAnalysis(plan("Shop", "s3cret"));
        service.addSource(new AnalysisPlan("Shop", "http://other.example:9090", null, null,
                plan("Shop", null).groups(), Duration.ofHours(1), Duration.ofSeconds(60)));
        assertEquals(2, service.statusOf("Shop").sourceCount());
        assertEquals("other projects are not affected", ProjectDataCollection.State.NONE, service.statusOf("Billing").state());

        // one of the two sources cannot start: the project shows the error, with its reason
        service.stopAll();
        secrets.clear();
        service.startAll();
        ProjectDataCollection.Status broken = service.statusOf("Shop");
        assertEquals(ProjectDataCollection.State.ERROR, broken.state());
        assertTrue(broken.detail(), broken.detail().contains("credentials"));

        // all sources switched off: paused
        sources.rows.forEach(row -> service.pause(row.id));
        assertEquals(ProjectDataCollection.State.PAUSED, service.statusOf("Shop").state());

        // all sources removed: nothing is collected anymore
        new ArrayList<>(sources.rows).forEach(row -> service.remove(row.id));
        assertEquals(ProjectDataCollection.Status.NONE, service.statusOf("Shop"));
    }

    private static AnalysisPlan plan(String name, String password) {
        return new AnalysisPlan(name, "http://prometheus.example:9090", password == null ? null : "analyst", password,
                List.of(new AnalysisPlan.GroupSelection("node", List.of(new MetricExpression(null, "up{job=\"node\"}")))),
                Duration.ofHours(24), Duration.ofSeconds(60));
    }

    /** The source store without a database. */
    private static final class InMemorySources implements CollectionSourceStore {

        final List<CollectionSourceRow> rows = new ArrayList<>();
        final Map<String, List<CollectionSourceEvent>> events = new HashMap<>();

        @Override
        public List<CollectionSourceRow> listAll() {
            return new ArrayList<>(rows);
        }

        @Override
        public List<CollectionSourceRow> listByProject(String projectName) {
            return rows.stream().filter(row -> row.projectName.equals(projectName)).collect(Collectors.toList());
        }

        @Override
        public CollectionSourceRow find(String id) {
            return rows.stream().filter(row -> row.id.equals(id)).findFirst().orElse(null);
        }

        @Override
        public CollectionSourceRow insert(CollectionSourceRow row) {
            rows.add(row);
            return row;
        }

        @Override
        public boolean update(CollectionSourceRow row) {
            return find(row.id) != null;
        }

        @Override
        public void delete(String id) {
            rows.removeIf(row -> row.id.equals(id));
        }

        @Override
        public void addEvent(String sourceId, String state, String message) {
            events.computeIfAbsent(sourceId, id -> new ArrayList<>()).add(0, new CollectionSourceEvent(Instant.now(), state, message));
        }

        @Override
        public List<CollectionSourceEvent> listEvents(String sourceId, int limit) {
            return events.getOrDefault(sourceId, List.of()).stream().limit(limit).collect(Collectors.toList());
        }
    }
}

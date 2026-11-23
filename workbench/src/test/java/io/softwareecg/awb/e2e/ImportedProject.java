package io.softwareecg.awb.e2e;

import io.softwareecg.awb.query.MetricDataAccessService;
import io.softwareecg.awb.timeseries.dimension.NamedValueEntity;
import io.softwareecg.awb.query.model.MetricQuery;
import io.softwareecg.awb.query.model.QuerySpec;
import io.softwareecg.awb.projectcontext.api.ProjectDataAccessService;

import io.softwareecg.awb.projectcontext.events.ProjectSelectedEvent;
import io.softwareecg.awb.projectcontext.model.Project;
import io.softwareecg.awb.projectcontext.model.ProjectViewFlavor;
import io.softwareecg.wfx.lookup.api.Lookup;
import io.softwareecg.wfx.platform.api.EventBus;
import javafx.scene.control.Label;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Function;

/**
 * What the repository of the Workbench holds of a project: asked the way the explorer and the
 * charts ask it, so that it is what a user would see.
 */
public final class ImportedProject {

    private final Workbench workbench;
    private final String name;

    ImportedProject(Workbench workbench, String name) {
        this.workbench = workbench;
        this.name = name;
    }

    /** @return whether the Workbench knows the project */
    public boolean exists() {
        return workbench.embeddedRepository().getBoundedService(ProjectDataAccessService.class)
                .listProjects().stream().anyMatch(project -> name.equals(project.getName()));
    }

    /** Selects the project, as a click on it in the explorer does, and waits for the project bar. */
    @SuppressWarnings("unchecked")
    public void select() {
        Project project = workbench.embeddedRepository().getBoundedService(ProjectDataAccessService.class)
                .listProjects().stream().filter(candidate -> name.equals(candidate.getName())).findFirst().orElseThrow();
        workbench.onFx(() -> {
            Lookup.lookup(EventBus.class).publish(new ProjectSelectedEvent(this, null, project,
                    ProjectViewFlavor.PHYSICAL_VIEW, workbench.embeddedRepository()));
            return null;
        });
        workbench.awaitTrue("the project bar to show '" + name + "'", Duration.ofSeconds(20), () -> workbench.onFx(() ->
                workbench.find("lbProjectId").map(Label.class::cast).map(Label::getText).filter(name::equals).isPresent()));
    }

    /** @return the number of values of all series of the project */
    public long points() {
        return metrics().getAmountMeasuredPointsInProject(name);
    }

    /**
     * Waits until the repository holds this many values of the project. What an import wrote
     * can be searched a moment after it ended, not at once.
     */
    public void awaitPoints(long expected, Duration timeout) {
        long[] seen = new long[1];
        try {
            Workbench.await(expected + " values in the project '" + name + "'", timeout, () -> {
                seen[0] = points();
                return seen[0] == expected ? Optional.of(true) : Optional.empty();
            });
        } catch (AssertionError e) {
            throw new AssertionError(e.getMessage() + "; it has " + seen[0], e);
        }
    }

    public Set<String> hostGroups() {
        return names(service -> service.getHostGroups(query()));
    }

    public Set<String> hosts() {
        return names(service -> service.getHosts(query()));
    }

    public Set<String> measurements() {
        return names(service -> service.getMeasurements(query()));
    }

    public Set<String> processes() {
        return names(service -> service.getProcesses(query()));
    }

    public Set<String> metricGroups() {
        return names(service -> service.getMetricGroups(query()));
    }

    public Set<String> metricNames() {
        return names(service -> service.getMetricsNames(query()));
    }

    public Set<String> namespaces() {
        return names(service -> service.getNamespaces(query()));
    }

    public Set<String> services() {
        return names(service -> service.getServices(query()));
    }

    private Set<String> names(Function<MetricDataAccessService, List<? extends NamedValueEntity>> facet) {
        Set<String> names = new TreeSet<>();
        for (NamedValueEntity entity : facet.apply(metrics())) {
            // the entry for "all of them", which the service puts first for the drop-down lists
            if (!"*".equals(entity.getValueName())) {
                names.add(entity.getValueName());
            }
        }
        return names;
    }

    private MetricQuery query() {
        QuerySpec.Builder<QuerySpec> filter = new QuerySpec.Builder<>();
        filter.withProject(name);
        return new MetricQuery(filter.build());
    }

    private MetricDataAccessService metrics() {
        return workbench.embeddedRepository().getBoundedService(MetricDataAccessService.class);
    }
}

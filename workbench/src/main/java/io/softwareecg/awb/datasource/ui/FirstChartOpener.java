package io.softwareecg.awb.datasource.ui;

import io.softwareecg.awb.uitoolkit.events.OpeningMode;
import io.softwareecg.awb.datasource.model.CollectionPlan;
import io.softwareecg.awb.datasource.model.SeriesDimensions;
import io.softwareecg.awb.query.api.OpenInvestigationCommand;
import io.softwareecg.awb.query.model.QuerySpec;
import io.softwareecg.awb.projectcontext.model.Project;
import io.softwareecg.awb.repository.api.EkgRepository;
import io.softwareecg.awb.repository.events.ProjectDataChangedEvent;
import io.softwareecg.wfx.platform.api.EventBus;
import io.softwareecg.wfx.platform.api.EventBusListener;
import javafx.application.Platform;

import java.util.EventObject;
import java.util.List;

/**
 * Opens the first chart of a new analysis as soon as its first values are there, so that
 * starting an analysis ends in a picture and not in an empty project.
 * <p>
 * The chart shows one metric for all systems of the first selected group - the comparison
 * across similar systems is what the analysis is for. The chart follows the data from then
 * on like any other open chart.
 */
final class FirstChartOpener {

    /**
     * The metrics that make the best first picture, most telling first.
     */
    private static final List<String> HEADLINES = List.of(
            "cpu_usage_percent", "process_cpu_usage_percent", "jvm_heap_used_bytes", "load_1m");

    private final EventBus<EventObject> eventBus;

    private final EkgRepository repository;

    private final String projectName;

    private final String group;

    private final String metric;

    private final EventBusListener<ProjectDataChangedEvent> listener = this::dataChanged;

    private Project project;

    private boolean dataArrived;

    private boolean done;

    private FirstChartOpener(EventBus<EventObject> eventBus, EkgRepository repository, CollectionPlan plan) {
        this.eventBus = eventBus;
        this.repository = repository;
        this.projectName = plan.projectName();
        this.group = plan.firstHostGroup();
        this.metric = headline(plan.firstMetrics());
    }

    /**
     * Starts waiting for the first values of the analysis. Call before the analysis is started,
     * so that fast first values are not missed.
     */
    static FirstChartOpener waitFor(EventBus<EventObject> eventBus, EkgRepository repository, CollectionPlan plan) {
        FirstChartOpener opener = new FirstChartOpener(eventBus, repository, plan);
        eventBus.subscribe(ProjectDataChangedEvent.class, opener.listener);
        return opener;
    }

    /**
     * The analysis was started; its project is now known.
     */
    synchronized void started(Project createdProject) {
        project = createdProject;
        openIfReady();
    }

    /**
     * The analysis could not be started: there will be no chart.
     */
    synchronized void cancel() {
        done = true;
        eventBus.unsubscribe(ProjectDataChangedEvent.class, listener);
    }

    private synchronized boolean dataChanged(ProjectDataChangedEvent event) {
        if (projectName.equals(event.getProjectName())) {
            dataArrived = true;
            openIfReady();
        }
        return true;
    }

    private void openIfReady() {
        if (done || project == null || !dataArrived) {
            return;
        }
        done = true;
        eventBus.unsubscribe(ProjectDataChangedEvent.class, listener);

        @SuppressWarnings({"unchecked", "rawtypes"})
        QuerySpec query = (QuerySpec) new QuerySpec.Builder()
                .withProject(project)
                .withHostGroup(group)
                .withMetricGroup(SeriesDimensions.metricGroupOf(metric))
                .withMetric(metric)
                .withMultiMetricMode(false)
                .build();
        // CLEAR_VIEW: shown in the active chart panel, replacing what is there. NEW_VIEW would
        // stack the new chart on top of an empty one.
        Platform.runLater(() -> eventBus.publish(new OpenInvestigationCommand(this, query, repository, OpeningMode.CLEAR_VIEW)));
    }

    /**
     * Picks the metric of the first chart from the names of what is collected for the group.
     */
    static String headline(List<String> metrics) {
        for (String headline : HEADLINES) {
            if (metrics.contains(headline)) {
                return headline;
            }
        }
        // "up" tells little; anything else collected is a better first picture
        return metrics.stream().filter(name -> !name.isEmpty() && !"up".equals(name)).findFirst().orElse("up");
    }
}

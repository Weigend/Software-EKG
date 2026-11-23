package io.softwareecg.awb.metricview.adapter;

import io.softwareecg.awb.uitoolkit.events.OpeningMode;
import io.softwareecg.awb.uitoolkit.view.EkgView;
import io.softwareecg.awb.uitoolkit.view.ViewMapper;
import io.softwareecg.awb.bookmark.model.MetricQueryBookmark;
import io.softwareecg.awb.investigation.api.OpenInvestigationCommand;
import io.softwareecg.awb.bookmark.ui.BookmarkEvent;
import io.softwareecg.awb.repository.api.EkgRepository;
import io.softwareecg.awb.metricview.ChartAnalyticsPanel;
import io.softwareecg.awb.metricview.MetricViewController;
import io.softwareecg.awb.runtime.logging.EkgLogger;
import io.softwareecg.wfx.lookup.api.Lookup;
import io.softwareecg.wfx.platform.api.EventBus;
import io.softwareecg.wfx.platform.api.EventSubscriber;
import io.softwareecg.wfx.platform.core.eventbus.AnnotationProcessor;
import io.softwareecg.wfx.windowmanager.api.FXMLView;
import io.softwareecg.wfx.windowmanager.api.Position;
import io.softwareecg.wfx.windowmanager.api.View;
import io.softwareecg.wfx.windowmanager.api.ViewKind;
import io.softwareecg.wfx.windowmanager.api.WindowManager;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.slf4j.Logger;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

/**
 * Adapter between navigation events and chart panel controllers.
 */
public class ChartPanelAdapter extends MetricViewController {

    private static final Logger LOGGER = EkgLogger.get();

    @Inject
    private ChartPanelManager chartPanelManager;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        chartPanelManager.init();
        super.initialize(url, rb);
    }

    /**
     * The ChartPanelManager controls the set of {@link MetricViewController}.
     * <p>
     * A chart view is a document of the window manager and belongs to one project: the project
     * it was opened for stands in the title of its tab, and it shows nothing else for as long as
     * it is open. A request for a chart - from the explorer, a bookmark, a new analysis - goes
     * to a view of its project. If there is none, a new tab is opened for it; the views of
     * other projects stay as they are, with their filters and their charts.
     */
    @Singleton
    public static class ChartPanelManager {

        private final WindowManager windowManager;

        @Inject
        public ChartPanelManager(@io.avaje.inject.External WindowManager windowManager) {
            this.windowManager = windowManager;
        }

        private FXMLView<MetricViewController> lastActiveView;

        /** The open chart views in the order they were last used, the most recent one last. */
        private final List<FXMLView<MetricViewController>> chartViews = new ArrayList<>();

        private boolean initialized = false;

        /**
         * Initialize the panels controller.
         */
        public void init() {
            if (initialized) {
                return;
            }
            initialized = true;
            windowManager.focusedViewProperty().addListener((o, ov, activeView) -> updateLastActiveController(activeView));
            AnnotationProcessor.process(this);
        }

        /**
         * Update listener for detecting the last active {@link MetricViewController}.
         *
         * @param activeView The current active view.
         */
        @SuppressWarnings("unchecked")
        private void updateLastActiveController(View activeView) {
            if ((activeView instanceof FXMLView) && (((FXMLView) activeView).getController()) instanceof MetricViewController) {
                used((FXMLView<MetricViewController>) activeView);
            }
        }

        /** Notes that a view was used: it is the last active one and the first to be found for its project. */
        private void used(FXMLView<MetricViewController> view) {
            lastActiveView = view;
            chartViews.remove(view);
            chartViews.add(view);
        }

        /** Forgets the views whose tabs were closed: a document is gone for good then. */
        private void forgetClosedViews() {
            for (FXMLView<MetricViewController> view : new ArrayList<>(chartViews)) {
                if (!windowManager.hasRegisteredView(view)) {
                    chartViews.remove(view);
                    view.getController().dispose();
                }
            }
            if (lastActiveView != null && !chartViews.contains(lastActiveView)) {
                lastActiveView = chartViews.isEmpty() ? null : chartViews.get(chartViews.size() - 1);
            }
        }

        /**
         * Get the last active {@link MetricViewController}.
         *
         * @return The last active {@link MetricViewController}.
         */
        public MetricViewController getLastActiveController() {
            forgetClosedViews();
            return lastActiveView != null ? lastActiveView.getController() : null;
        }

        /**
         * Creates a new, empty chart panel and opens it. It belongs to no project until the
         * first chart is asked for.
         */
        public void createNewChartPanel() {
            createChartView(null);
        }

        private FXMLView<MetricViewController> createChartView(String title) {
            try {
                EkgView<MetricViewController> chartView = ChartAnalyticsPanel.createInstance(title);
                FXMLView<MetricViewController> view = new ViewMapper<MetricViewController>()
                        .convert(chartView, Position.CENTER, ViewKind.DOCUMENT);

                windowManager.register(view);
                used(view);
                return view;
            } catch (IOException e) {
                throw new IllegalStateException("Error while opening new chart window.", e);
            }
        }

        /**
         * Finds the view a request for a project goes to, or opens one.
         *
         * @param repository  the repository of the project
         * @param projectName the project; {@code null} if the request names none
         * @param newView     whether the request asks for a view of its own
         * @return the view, which is the last active one from now on
         */
        private FXMLView<MetricViewController> viewFor(EkgRepository repository, String projectName, boolean newView) {
            forgetClosedViews();
            String project = MetricViewController.projectKey(repository, projectName);

            if (project == null) {
                // nothing says whose data it is: the view in use, whatever it shows
                return lastActiveView != null && !newView ? lastActiveView : createChartView(null);
            }
            if (!newView) {
                // the view of this project that was used last - which is the active one if it fits
                for (int i = chartViews.size() - 1; i >= 0; i--) {
                    if (project.equals(chartViews.get(i).getController().getBoundProjectKey())) {
                        used(chartViews.get(i));
                        return lastActiveView;
                    }
                }
            }

            // An empty view that shows no project yet makes room: the new one carries the
            // project in its title, which a view cannot change once it is open.
            FXMLView<MetricViewController> empty = null;
            for (FXMLView<MetricViewController> view : chartViews) {
                if (!newView && view.getController().getBoundProjectKey() == null) {
                    empty = view;
                }
            }
            FXMLView<MetricViewController> view = createChartView(titleFor(project, projectName));
            if (empty != null) {
                windowManager.unregister(empty);
                chartViews.remove(empty);
                empty.getController().dispose();
            }
            return view;
        }

        /** The name of the project, numbered if the project has a view already. */
        private String titleFor(String project, String projectName) {
            long views = chartViews.stream()
                    .filter(view -> project.equals(view.getController().getBoundProjectKey())).count();
            return views == 0 ? projectName : projectName + " (" + (views + 1) + ")";
        }

        /**
         * Closes all chart panels.
         */
        public void removeAllDynamicChartPanels() {
            for (FXMLView<MetricViewController> view : new ArrayList<>(chartViews)) {
                windowManager.unregister(view);
                view.getController().dispose();
            }
            chartViews.clear();
            lastActiveView = null;
        }

        /**
         * Check if the given view contains a chart panel.
         *
         * @param view The view to check for a chart panel view.
         * @return true if the given view is a chart panel. otherwise false.
         */
        public boolean isChartPanel(View view) {
            return view.getViewId().startsWith(ChartAnalyticsPanel.VIEW_PREFIX);
        }

        /**
         * Returns true if at least one chart panel is visible.
         *
         * @return True if at least one chart panel is visible, false otherwise.
         */
        public boolean hasChartPanel() {
            for (View view : windowManager.getVisibleViews()) {
                if (isChartPanel(view)) {
                    return true;
                }
            }
            return false;
        }

        /**
         * Shows the chart a {@link OpenInvestigationCommand} asks for, in a view of its project.
         *
         * @param event The information about the new search.
         * @return indicates whether new Panel was opened
         */
        @EventSubscriber(eventClass = OpenInvestigationCommand.class)
        public boolean openChartPanel(OpenInvestigationCommand event) {
            int viewsBefore = chartViews.size();
            FXMLView<MetricViewController> view = viewFor(event.getRepository(),
                    event.getFilterParams() == null ? null : event.getFilterParams().getProjectName(),
                    event.getOpeningMode() == OpeningMode.NEW_VIEW);
            updateControllerByEvent(view.getController(), event);
            windowManager.showView(view);
            return chartViews.size() > viewsBefore;
        }

        /**
         * Reloads the graph with the bookmark. Wired via the WFX EventBus.
         *
         * @param event the bookmark event
         * @return true to indicate the event was handled
         */
        @EventSubscriber(eventClass = BookmarkEvent.class)
        public boolean openChartPanelWithBookmark(BookmarkEvent event) {
            // A bookmark of a project opens in a view of that project. A global bookmark is a
            // template for whatever project is looked at, so it goes to the view in use.
            String projectName = event.getBookmark() instanceof MetricQueryBookmark bookmark ? bookmark.getProject() : null;
            FXMLView<MetricViewController> view = viewFor(event.getSourceRepository(), projectName,
                    event.getOpeningMode() == OpeningMode.NEW_VIEW);

            view.getController().updateByBookmarkEvent(event);

            windowManager.showView(view);
            return true;
        }

        /**
         * Updates the target chart controller from a query context event.
         *
         * @param controller the controller the event was sent to
         * @param event event
         */
        public void updateControllerByEvent(MetricViewController controller, OpenInvestigationCommand event) {
            controller.updateByContextEvent(event);
        }
    }
}

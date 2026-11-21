package io.softwareecg.collector.ui;

import io.softwareecg.collector.ui.DisplayModel.Condition;
import io.softwareecg.collector.ui.DisplayModel.Job;
import io.softwareecg.collector.ui.DisplayModel.Metric;
import io.softwareecg.collector.ui.DisplayModel.Summary;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.OverrunStyle;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Supplier;

/**
 * What is on the screen. Two pages: the collector with its jobs, and one job with its metrics.
 * A tap on a job opens it, a tap on "Jobs" leads back.
 * <p>
 * Made for a small touch screen: everything that can be tapped is as high as a finger is wide,
 * the lists are moved by dragging, and nothing needs a keyboard or a pointer that hovers.
 */
final class DisplayView {

    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm:ss", Locale.ROOT);

    private static final double SPARKLINE_WIDTH = 190;

    private static final double SPARKLINE_HEIGHT = 44;

    private final DisplayModel model;

    private final Supplier<Instant> clock;

    private final ZoneId zone;

    private final BorderPane root = new BorderPane();

    // --- the header ---------------------------------------------------------------------------

    private final Label state = new Label();

    private final Label time = new Label();

    // --- the page of the collector ----------------------------------------------------------------

    private final Label jobsTile = new Label();

    private final Label okTile = new Label();

    private final Label problemsTile = new Label();

    private final Label metricsTile = new Label();

    private final Label running = new Label();

    private final ListView<Job> jobList = new ListView<>();

    private final Node overviewPage;

    // --- the page of a job --------------------------------------------------------------------------

    /** The job that is open, {@code null} while the page of the collector is shown. */
    private String openJob;

    private final Label jobTitle = new Label();

    private final Label jobSubtitle = new Label();

    private final Label jobState = new Label();

    private final Label jobError = new Label();

    private final ListView<String> metricList = new ListView<>();

    private final Node jobPage;

    /**
     * @param title what the collector collects for: the project of its configuration
     * @param clock the current time
     */
    DisplayView(DisplayModel model, String title, Supplier<Instant> clock, ZoneId zone) {
        this.model = model;
        this.clock = clock;
        this.zone = zone;

        root.getStyleClass().add("display");
        root.getStylesheets().add(DisplayView.class.getResource("display.css").toExternalForm());
        root.setTop(header(title));

        overviewPage = overviewPage();
        jobPage = jobPage();
        jobPage.setVisible(false);
        root.setCenter(new StackPane(overviewPage, jobPage));

        refresh();
    }

    Parent root() {
        return root;
    }

    /**
     * Shows the model as it is now.
     */
    void refresh() {
        Instant now = clock.get();
        Summary summary = model.summary();

        time.setText(TIME.format(now.atZone(zone)));
        state.setText(summary.text());
        mark(state, summary.condition());

        jobsTile.setText(String.valueOf(summary.jobs()));
        okTile.setText(String.valueOf(summary.ok()));
        problemsTile.setText(String.valueOf(summary.problems()));
        mark(problemsTile, summary.problems() > 0 ? Condition.PROBLEM : null);
        metricsTile.setText(String.valueOf(summary.metrics()));
        running.setText(runningText(summary, now));

        List<Job> jobs = model.jobs();
        if (!sameObjects(jobList.getItems(), jobs)) {
            jobList.getItems().setAll(jobs);
        } else {
            jobList.refresh();
        }

        Job job = openJob == null ? null : model.job(openJob);
        if (openJob != null && job == null) {
            showOverview();   // the collector no longer has the job
        } else if (job != null) {
            showJob(job);
        }
    }

    // ---------------------------------------------------------------------------------------------
    // the header
    // ---------------------------------------------------------------------------------------------

    private Node header(String title) {
        Label name = new Label(title);
        name.getStyleClass().add("title");
        name.setTextOverrun(OverrunStyle.ELLIPSIS);
        name.setMinWidth(0);

        state.getStyleClass().add("pill");
        state.setMinWidth(Region.USE_PREF_SIZE);
        time.getStyleClass().add("clock");
        time.setMinWidth(Region.USE_PREF_SIZE);

        HBox header = new HBox(14, trace(), name, spacer(), state, time);
        header.getStyleClass().add("header");
        header.setAlignment(Pos.CENTER_LEFT);
        return header;
    }

    /** The trace of an ECG, as the sign of the product. */
    private static Node trace() {
        Canvas canvas = new Canvas(54, 30);
        GraphicsContext graphics = canvas.getGraphicsContext2D();
        graphics.setStroke(Color.web("#35c8f5"));
        graphics.setLineWidth(2.5);
        double[] x = {1, 14, 18, 22, 26, 31, 36, 40, 53};
        double[] y = {17, 17, 11, 17, 3, 27, 13, 17, 17};
        graphics.strokePolyline(x, y, x.length);
        return canvas;
    }

    // ---------------------------------------------------------------------------------------------
    // the page of the collector
    // ---------------------------------------------------------------------------------------------

    private Node overviewPage() {
        HBox tiles = new HBox(12, tile(jobsTile, "Jobs"), tile(okTile, "OK"), tile(problemsTile, "Problems"),
                tile(metricsTile, "Metrics"));
        tiles.getStyleClass().add("tiles");

        running.getStyleClass().add("running");

        jobList.getStyleClass().add("rows");
        jobList.setCellFactory(list -> new JobCell());
        jobList.setFocusTraversable(false);
        Label none = new Label("The collector has no jobs.");
        none.getStyleClass().add("placeholder");
        jobList.setPlaceholder(none);
        VBox.setVgrow(jobList, Priority.ALWAYS);

        VBox page = new VBox(10, tiles, running, jobList);
        page.getStyleClass().add("page");
        return page;
    }

    private static Node tile(Label value, String caption) {
        value.getStyleClass().add("tile-value");
        Label name = new Label(caption);
        name.getStyleClass().add("tile-caption");
        VBox tile = new VBox(value, name);
        tile.getStyleClass().add("tile");
        tile.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(tile, Priority.ALWAYS);
        tile.setMaxWidth(Double.MAX_VALUE);
        return tile;
    }

    private static String runningText(Summary summary, Instant now) {
        if (summary.startedAt() == null) {
            return "Not started";
        }
        String text = (summary.running() ? "Running for " : "Stopped, was started ")
                + (summary.running() ? DisplayText.span(Duration.between(summary.startedAt(), now))
                : DisplayText.ago(summary.startedAt(), now));
        return summary.newestValues() == null ? text + "  ·  no values yet"
                : text + "  ·  last values " + DisplayText.ago(summary.newestValues(), now);
    }

    private final class JobCell extends ListCell<Job> {

        private final Region mark = new Region();
        private final Label title = new Label();
        private final Label subtitle = new Label();
        private final Label stateText = new Label();
        private final Label age = new Label();
        private final Label error = new Label();
        private final VBox content;

        JobCell() {
            getStyleClass().add("row");
            mark.getStyleClass().add("mark");
            title.getStyleClass().add("row-title");
            title.setMinWidth(0);
            subtitle.getStyleClass().add("row-subtitle");
            subtitle.setMinWidth(0);
            stateText.getStyleClass().add("state-text");
            stateText.setMinWidth(Region.USE_PREF_SIZE);
            age.getStyleClass().add("row-subtitle");
            age.setMinWidth(Region.USE_PREF_SIZE);
            error.getStyleClass().add("error-text");
            error.setWrapText(true);

            VBox names = new VBox(2, title, subtitle);
            names.setMinWidth(0);
            HBox.setHgrow(names, Priority.ALWAYS);
            VBox right = new VBox(2, stateText, age);
            right.setAlignment(Pos.CENTER_RIGHT);
            Label open = new Label("›");
            open.getStyleClass().add("chevron");
            HBox line = new HBox(12, mark, names, right, open);
            line.setAlignment(Pos.CENTER_LEFT);
            content = new VBox(6, line, error);

            setOnMouseClicked(event -> {
                if (getItem() != null) {
                    open(getItem());
                }
            });
        }

        @Override
        protected void updateItem(Job job, boolean empty) {
            super.updateItem(job, empty);
            if (empty || job == null) {
                setGraphic(null);
                return;
            }
            title.setText(job.title());
            int metrics = job.status().metricCount();
            subtitle.setText(job.subtitle() + "  ·  " + metrics + (metrics == 1 ? " metric" : " metrics"));
            stateText.setText(job.stateText());
            mark(stateText, job.condition());
            mark(mark, job.condition());
            age.setText(job.status().lastSampleTime() == null ? "no values yet"
                    : DisplayText.ago(job.status().lastSampleTime(), clock.get()));
            String problem = job.condition() == Condition.PROBLEM ? job.status().lastError() : null;
            error.setText(problem);
            error.setVisible(problem != null);
            error.setManaged(problem != null);
            // the text of an error is wrapped to the width of the list
            content.setPrefWidth(Math.max(0, getListView().getWidth() - 44));
            setGraphic(content);
        }
    }

    // ---------------------------------------------------------------------------------------------
    // the page of a job
    // ---------------------------------------------------------------------------------------------

    private Node jobPage() {
        Button back = new Button("‹  Jobs");
        back.getStyleClass().add("back");
        back.setFocusTraversable(false);
        back.setMinWidth(Region.USE_PREF_SIZE);
        back.setOnAction(event -> showOverview());

        jobTitle.getStyleClass().add("row-title");
        jobTitle.setMinWidth(0);
        jobSubtitle.getStyleClass().add("row-subtitle");
        jobSubtitle.setMinWidth(0);
        VBox names = new VBox(2, jobTitle, jobSubtitle);
        names.setMinWidth(0);
        HBox.setHgrow(names, Priority.ALWAYS);

        jobState.getStyleClass().add("pill");
        jobState.setMinWidth(Region.USE_PREF_SIZE);

        HBox head = new HBox(14, back, names, jobState);
        head.setAlignment(Pos.CENTER_LEFT);

        jobError.getStyleClass().add("error-text");
        jobError.setWrapText(true);

        metricList.getStyleClass().add("rows");
        metricList.setCellFactory(list -> new MetricCell());
        metricList.setFocusTraversable(false);
        Label none = new Label("No values yet.");
        none.getStyleClass().add("placeholder");
        metricList.setPlaceholder(none);
        VBox.setVgrow(metricList, Priority.ALWAYS);

        VBox page = new VBox(10, head, jobError, metricList);
        page.getStyleClass().add("page");
        return page;
    }

    /**
     * Opens the page of a job, as a tap on the job does.
     */
    void open(Job job) {
        openJob = DisplayModel.keyOf(job);
        showJob(job);
    }

    /**
     * Returns to the page of the collector, as a tap on "Jobs" does.
     */
    void showOverview() {
        openJob = null;
        jobPage.setVisible(false);
        overviewPage.setVisible(true);
    }

    private void showJob(Job job) {
        jobTitle.setText(job.title());
        jobSubtitle.setText(job.subtitle() + "  ·  " + (job.status().lastSampleTime() == null ? "no values yet"
                : "values " + DisplayText.ago(job.status().lastSampleTime(), clock.get())));
        jobState.setText(job.stateText());
        mark(jobState, job.condition());
        String problem = job.condition() == Condition.PROBLEM ? job.status().lastError() : null;
        jobError.setText(problem);
        jobError.setVisible(problem != null);
        jobError.setManaged(problem != null);

        List<String> labels = new ArrayList<>();
        for (Metric metric : job.metrics()) {
            labels.add(metric.label());
        }
        if (!labels.equals(metricList.getItems())) {
            metricList.getItems().setAll(labels);
        } else {
            metricList.refresh();
        }
        overviewPage.setVisible(false);
        jobPage.setVisible(true);
    }

    private final class MetricCell extends ListCell<String> {

        private final Label name = new Label();
        private final Label range = new Label();
        private final Label value = new Label();
        private final Sparkline curve = new Sparkline(SPARKLINE_WIDTH, SPARKLINE_HEIGHT);
        private final HBox content;

        MetricCell() {
            getStyleClass().add("row");
            name.getStyleClass().add("row-title");
            name.setMinWidth(0);
            // the end of the name of a metric says more than its beginning
            name.setTextOverrun(OverrunStyle.LEADING_ELLIPSIS);
            range.getStyleClass().add("row-subtitle");
            range.setMinWidth(0);
            value.getStyleClass().add("metric-value");
            value.setMinWidth(Region.USE_PREF_SIZE);

            VBox names = new VBox(2, name, range);
            names.setMinWidth(0);
            HBox.setHgrow(names, Priority.ALWAYS);
            content = new HBox(14, names, value, curve);
            content.setAlignment(Pos.CENTER_LEFT);
        }

        @Override
        protected void updateItem(String label, boolean empty) {
            super.updateItem(label, empty);
            Job job = openJob == null ? null : model.job(openJob);
            Metric metric = empty || label == null || job == null ? null : job.metric(label);
            if (metric == null) {
                setGraphic(null);
                return;
            }
            name.setText(metric.label());
            double min = metric.history().min();
            range.setText(Double.isNaN(min) ? "no value yet"
                    : DisplayText.value(min) + "  to  " + DisplayText.value(metric.history().max()));
            value.setText(DisplayText.value(metric.history().last()));
            curve.draw(metric.history().values());
            content.setPrefWidth(Math.max(0, getListView().getWidth() - 44));
            setGraphic(content);
        }
    }

    // ---------------------------------------------------------------------------------------------
    // helpers
    // ---------------------------------------------------------------------------------------------

    private static Region spacer() {
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        return spacer;
    }

    /** Gives a node the colour of a condition; none for {@code null}. */
    private static void mark(Node node, Condition condition) {
        node.getStyleClass().removeAll("ok", "problem", "starting", "stopped");
        if (condition != null) {
            node.getStyleClass().add(condition.name().toLowerCase(Locale.ROOT));
        }
    }

    private static boolean sameObjects(List<Job> shown, List<Job> current) {
        if (shown.size() != current.size()) {
            return false;
        }
        for (int i = 0; i < shown.size(); i++) {
            if (shown.get(i) != current.get(i)) {
                return false;
            }
        }
        return true;
    }
}

package io.softwareecg.awb.datasource.ui;

import io.softwareecg.awb.datasource.NewAnalysisService;
import io.softwareecg.awb.datasource.adapter.ssh.SshDiscovery.HostCheck;
import io.softwareecg.awb.datasource.adapter.ssh.SshDiscovery.ProcessCandidate;
import io.softwareecg.awb.datasource.adapter.ssh.SshPlan;
import io.softwareecg.awb.datasource.adapter.ssh.TopMetric;
import javafx.application.Platform;
import javafx.beans.property.StringProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.VPos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;

import java.time.Duration;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * The step of a new analysis in which Unix hosts are named that are read directly, by SSH and
 * {@code top}. It follows the way the collector thinks: <b>hosts</b> (with one login for all of
 * them), the <b>processes</b> to watch on each, and the <b>metrics</b> to read - and it shows
 * what that multiplies out to.
 * <p>
 * The hosts have to be named, since nothing knows them: typed, as a range ({@code db-[01-12]}),
 * or {@code localhost} for this machine. "Check hosts" then does what a discovery does for a
 * source with an inventory: it logs in to each host, says whether that worked, and offers the
 * programs running there as processes to watch. Whoever changes nothing else gets the
 * essential metrics of each host every 15 seconds.
 */
final class SshPage {

    private final NewAnalysisService service;

    private final Runnable onChange;

    private final TextArea hostsField = new TextArea();
    private final Label hostsFound = new Label();
    private final TextField userField = new TextField();
    private final PasswordField passwordField = new PasswordField();
    private final Button checkButton = new Button("Check hosts");
    private final VBox checkResults = new VBox(2);
    private final FlowPane processPane = new FlowPane(14, 4);
    private final Label processHint = new Label("Check the hosts to see what is running there, or name the programs:");
    private final TextField moreProcesses = new TextField();
    private final Map<String, CheckBox> processChoices = new LinkedHashMap<>();
    private final Map<TopMetric, CheckBox> metricChoices = new EnumMap<>(TopMetric.class);
    private final FlowPane processMetricsPane = new FlowPane(14, 4);
    private final Label summary = new Label();
    private final TextField nameField = new TextField();
    private final TextField groupField = new TextField("linux");
    private final ComboBox<Duration> intervalBox = new ComboBox<>();

    private final VBox node;

    /** The width of one of several choices standing side by side, so that they form columns. */
    private static final double CHOICE_WIDTH = 166;

    /** The name of the analysis this page proposed, to tell it from one the user typed. */
    private String proposedName;

    /** Counts the checks started, so that the answer of an overtaken one is ignored. */
    private int checkGeneration;

    /**
     * @param service          looks at the hosts
     * @param existingAnalysis the analysis a source is added to, or {@code null} for a new one
     * @param onChange         called whenever what the page holds has changed
     */
    SshPage(NewAnalysisService service, String existingAnalysis, Runnable onChange) {
        this.service = service;
        this.onChange = onChange;

        hostsField.setId("sshHosts");
        hostsField.setPromptText("db-01, db-02   or a range: db-[01-12]   –   localhost reads this machine");
        hostsField.setPrefRowCount(2);
        hostsField.setWrapText(true);
        hostsField.setTooltip(new Tooltip("Host names or addresses, separated by commas or line breaks.\n"
                + "db-[01-12] stands for db-01 to db-12. A port can follow (db-01:2222),\n"
                + "and a host can get a name of its own (db=10.0.0.5)."));
        hostsField.textProperty().addListener((o, ov, nv) -> {
            // what a check said belongs to the hosts it looked at
            checkGeneration++;
            checkResults.getChildren().clear();
            changed();
        });
        hostsFound.setId("sshHostsFound");
        hostsFound.getStyleClass().add("found-detail");

        userField.setId("sshUser");
        userField.setPromptText("as on this machine");
        userField.setPrefColumnCount(12);
        passwordField.setId("sshPassword");
        passwordField.setPromptText("empty: your SSH key");
        passwordField.setPrefColumnCount(12);
        checkButton.setId("checkHosts");
        checkButton.setOnAction(e -> checkHosts());
        checkButton.setMinWidth(Region.USE_PREF_SIZE);
        HBox loginRow = new HBox(8, userField, new Label("Password"), passwordField, checkButton);
        loginRow.setAlignment(Pos.CENTER_LEFT);

        checkResults.setId("hostResults");
        ScrollPane resultsScroll = new ScrollPane(checkResults);
        resultsScroll.setFitToWidth(true);
        resultsScroll.setMaxHeight(84);
        resultsScroll.setStyle("-fx-background-color: transparent;");
        resultsScroll.visibleProperty().bind(javafx.beans.binding.Bindings.isNotEmpty(checkResults.getChildren()));
        resultsScroll.managedProperty().bind(resultsScroll.visibleProperty());

        processHint.getStyleClass().add("found-detail");
        processHint.setWrapText(true);
        moreProcesses.setId("moreProcesses");
        moreProcesses.setPromptText("further programs, e.g. java, postgres");
        moreProcesses.setTooltip(new Tooltip("Programs to watch on every host, by a part of their command line,\n"
                + "separated by commas. The host as a whole is always read."));
        moreProcesses.textProperty().addListener((o, ov, nv) -> changed());
        moreProcesses.setPrefWidth(250);
        moreProcesses.setMaxWidth(250);
        VBox processes = new VBox(4, processHint, processPane, moreProcesses);

        FlowPane systemMetricsPane = new FlowPane(8, 4);
        systemMetricsPane.setPrefWrapLength(4 * CHOICE_WIDTH + 24);
        processMetricsPane.setHgap(8);
        processPane.setHgap(8);
        processPane.setPrefWrapLength(4 * CHOICE_WIDTH + 24);
        for (TopMetric metric : TopMetric.values()) {
            CheckBox choice = new CheckBox(displayName(metric));
            choice.setId("topMetric-" + metric.name());
            choice.setPrefWidth(CHOICE_WIDTH);
            choice.setSelected(metric.isEssential());
            choice.selectedProperty().addListener((o, ov, nv) -> changed());
            metricChoices.put(metric, choice);
            (metric.scope() == TopMetric.Scope.SYSTEM ? systemMetricsPane : processMetricsPane).getChildren().add(choice);
        }
        Label perHost = new Label("Of each host");
        perHost.getStyleClass().add("found-detail");
        Label perProcess = new Label("Of each process");
        perProcess.getStyleClass().add("found-detail");
        VBox metrics = new VBox(4, perHost, systemMetricsPane, perProcess, processMetricsPane);

        GridPane hostsForm = new GridPane();
        hostsForm.setHgap(10);
        hostsForm.setVgap(8);
        addRow(hostsForm, 0, "Names", new VBox(3, hostsField, hostsFound));
        addRow(hostsForm, 1, "User", new VBox(4, loginRow, resultsScroll));

        VBox form = new VBox(8, band("Hosts"), hostsForm, band("Processes to watch on every host"), processes,
                band("Metrics"), metrics);
        form.setPadding(new Insets(0, 8, 0, 0));

        summary.setId("sshSummary");
        summary.setWrapText(true);
        summary.setStyle("-fx-font-weight: bold;");

        nameField.setId("sshAnalysisName");
        nameField.setPromptText("name of the analysis");
        nameField.textProperty().addListener((o, ov, nv) -> changed());
        if (existingAnalysis != null) {
            nameField.setText(existingAnalysis);
            nameField.setDisable(true);
        }
        groupField.setId("sshGroup");
        groupField.setPrefColumnCount(9);
        groupField.setTooltip(new Tooltip("The name these hosts have as a group in the analysis."));
        groupField.textProperty().addListener((o, ov, nv) -> changed());
        intervalBox.setId("sshInterval");
        intervalBox.getItems().addAll(Duration.ofSeconds(5), Duration.ofSeconds(15), Duration.ofSeconds(60));
        intervalBox.setValue(Duration.ofSeconds(15));
        intervalBox.setConverter(seconds());
        HBox.setHgrow(nameField, Priority.ALWAYS);
        HBox settings = new HBox(8, new Label("Name"), nameField, new Label("Group"), groupField,
                new Label("Update"), intervalBox);
        settings.setAlignment(Pos.CENTER_LEFT);

        ScrollPane formScroll = new ScrollPane(form);
        formScroll.setFitToWidth(true);
        formScroll.setPrefViewportHeight(470);
        formScroll.setStyle("-fx-background-color: transparent;");
        VBox.setVgrow(formScroll, Priority.ALWAYS);

        node = new VBox(8, formScroll, band("Analysis"), summary, settings);
        node.setPadding(new Insets(12));
        changed();
    }

    private static Label band(String text) {
        Label band = new Label(text);
        band.getStyleClass().add("section-band");
        band.setMaxWidth(Double.MAX_VALUE);
        return band;
    }

    private static void addRow(GridPane form, int row, String label, Node content) {
        Label caption = new Label(label);
        caption.setMinWidth(Region.USE_PREF_SIZE);
        GridPane.setValignment(caption, VPos.TOP);
        caption.setPadding(new Insets(4, 0, 0, 0));
        GridPane.setHgrow(content, Priority.ALWAYS);
        form.add(caption, 0, row);
        form.add(content, 1, row);
    }

    Node node() {
        return node;
    }

    /** The name of the analysis, shared with the other steps that ask for it. */
    StringProperty nameProperty() {
        return nameField.textProperty();
    }

    void focus() {
        Platform.runLater(hostsField::requestFocus);
    }

    /** Says what the start of the analysis is doing, or what went wrong, in place of the summary. */
    void showMessage(String text, boolean problem) {
        summary.setStyle(problem ? "-fx-font-weight: bold; -fx-text-fill: #b42318;" : "-fx-font-weight: bold;");
        summary.setText(text);
    }

    //=================================================================================================================
    // what the page holds
    //=================================================================================================================

    private List<SshPlan.Host> hosts() {
        return SshPlan.parseHosts(hostsField.getText());
    }

    /** The processes to watch: the ticked ones and the ones named by hand. */
    private List<String> processes() {
        List<String> processes = new ArrayList<>();
        processChoices.forEach((name, choice) -> {
            if (choice.isSelected()) {
                processes.add(name);
            }
        });
        if (moreProcesses.getText() != null) {
            for (String name : moreProcesses.getText().split("[,;]")) {
                if (!name.isBlank() && !processes.contains(name.trim())) {
                    processes.add(name.trim());
                }
            }
        }
        return processes;
    }

    private List<TopMetric> metrics(TopMetric.Scope scope) {
        List<TopMetric> metrics = new ArrayList<>();
        metricChoices.forEach((metric, choice) -> {
            if (metric.scope() == scope && choice.isSelected()) {
                metrics.add(metric);
            }
        });
        return metrics;
    }

    /**
     * @param projectName the name of the analysis
     * @return what the page holds as a plan; it may be incomplete ({@link #isComplete()})
     */
    SshPlan plan(String projectName) {
        return new SshPlan(projectName, groupField.getText() == null ? "" : groupField.getText().trim(), hosts(),
                blankToNull(userField.getText()), blankToNull(passwordField.getText()), processes(),
                metrics(TopMetric.Scope.SYSTEM), metrics(TopMetric.Scope.PROCESS), intervalBox.getValue());
    }

    boolean isComplete() {
        return nameField.getText() != null && !nameField.getText().isBlank() && plan(nameField.getText()).isComplete();
    }

    /** Shows what the choices multiply out to. */
    private void changed() {
        SshPlan plan = plan("");
        int hosts = plan.hosts().size();
        hostsFound.setText(hosts == 0 ? "" : hosts + (hosts == 1 ? " host:  " : " hosts:  ") + shorten(plan.hosts()));
        checkButton.setDisable(hosts == 0);

        // the name is proposed from the first host until the user gives one
        String name = nameField.getText() == null ? "" : nameField.getText();
        if (!nameField.isDisabled() && hosts > 0 && (name.isBlank() || name.equals(proposedName))) {
            proposedName = plan.hosts().get(0).name();
            nameField.setText(proposedName);
        }

        boolean anyProcess = !plan.processes().isEmpty();
        processMetricsPane.setDisable(!anyProcess);

        summary.setStyle("-fx-font-weight: bold;");
        if (hosts == 0) {
            summary.setText("Name the hosts to read.");
        } else if (plan.jobCount() == 0) {
            summary.setText("Nothing is selected to read.");
        } else {
            int processes = plan.processMetrics().isEmpty() ? 0 : plan.processes().size();
            boolean system = !plan.systemMetrics().isEmpty();
            String each = (system ? "the host itself" : "")
                    + (system && processes > 0 ? " + " : "")
                    + (processes > 0 ? processes + (processes == 1 ? " process" : " processes") : "");
            summary.setText(hosts + (hosts == 1 ? " host" : " hosts") + " × (" + each + ")  =  "
                    + plan.jobCount() + (plan.jobCount() == 1 ? " collector job, " : " collector jobs, ")
                    + plan.seriesCount() + " series");
        }
        onChange.run();
    }

    //=================================================================================================================
    // looking at the hosts
    //=================================================================================================================

    private void checkHosts() {
        List<SshPlan.Host> hosts = hosts();
        if (hosts.isEmpty()) {
            return;
        }
        String user = blankToNull(userField.getText());
        String password = blankToNull(passwordField.getText());
        int generation = ++checkGeneration;

        checkButton.setDisable(true);
        checkResults.getChildren().setAll(detail("Checking " + hosts.size() + (hosts.size() == 1 ? " host" : " hosts") + " ...", null));

        Thread thread = new Thread(() -> {
            List<HostCheck> checks;
            try {
                checks = service.inspectHosts(hosts, user, password);
            } catch (RuntimeException e) {
                checks = hosts.stream().map(host -> new HostCheck(host, false, String.valueOf(e.getMessage()), List.of())).toList();
            }
            List<HostCheck> result = checks;
            Platform.runLater(() -> {
                if (generation == checkGeneration) {
                    showChecks(result);
                }
            });
        }, "check-hosts");
        thread.setDaemon(true);
        thread.start();
    }

    private void showChecks(List<HostCheck> checks) {
        checkButton.setDisable(false);
        checkResults.getChildren().clear();
        long reachable = checks.stream().filter(HostCheck::reachable).count();
        Label head = detail(reachable == checks.size()
                ? (checks.size() == 1 ? "The host can be read." : "All " + checks.size() + " hosts can be read.")
                : reachable + " of " + checks.size() + " hosts can be read:", reachable == checks.size() ? "#1a7f37" : "#8a6100");
        head.setId("hostCheckSummary");
        checkResults.getChildren().add(head);
        for (HostCheck check : checks) {
            if (!check.reachable()) {
                checkResults.getChildren().add(detail("✗  " + check.host().name() + "    " + check.problem(), "#b42318"));
            }
        }

        // what runs on the hosts, the busiest first, summed up over all of them
        Map<String, double[]> candidates = new LinkedHashMap<>();
        for (HostCheck check : checks) {
            for (ProcessCandidate candidate : check.processes()) {
                double[] sums = candidates.computeIfAbsent(candidate.name(), name -> new double[2]);
                sums[0] += candidate.instances();
                sums[1] += candidate.cpuPercent() + candidate.memoryPercent();
            }
        }
        List<String> ticked = processChoices.entrySet().stream()
                .filter(entry -> entry.getValue().isSelected()).map(Map.Entry::getKey).toList();
        processChoices.clear();
        processPane.getChildren().clear();
        candidates.entrySet().stream()
                .sorted((a, b) -> Double.compare(b.getValue()[1], a.getValue()[1]))
                .limit(12)
                .forEach(entry -> {
                    int instances = (int) entry.getValue()[0];
                    CheckBox choice = new CheckBox(entry.getKey() + (instances > 1 ? "  ×" + instances : ""));
                    choice.setId("process-" + entry.getKey());
                    choice.setPrefWidth(CHOICE_WIDTH);
                    choice.setSelected(ticked.contains(entry.getKey()));
                    choice.selectedProperty().addListener((o, ov, nv) -> changed());
                    processChoices.put(entry.getKey(), choice);
                    processPane.getChildren().add(choice);
                });
        processHint.setText(candidates.isEmpty()
                ? "Name the programs to watch on every host:"
                : "Running on the hosts, the busiest first – tick what you want to watch on every host:");
        changed();
    }

    private static Label detail(String text, String color) {
        Label label = new Label(text);
        label.setWrapText(true);
        if (color != null) {
            label.setStyle("-fx-text-fill: " + color + ";");
        }
        return label;
    }

    //=================================================================================================================
    // helpers
    //=================================================================================================================

    /** "cpu usage %", "load 1m" - a metric as the user reads it, the same wording as for every source. */
    private static String displayName(TopMetric metric) {
        return metric.seriesName().replace("_percent", " %").replace('_', ' ');
    }

    private static String shorten(List<SshPlan.Host> hosts) {
        if (hosts.size() <= 4) {
            return String.join(", ", hosts.stream().map(SshPlan.Host::name).toList());
        }
        return hosts.get(0).name() + ", " + hosts.get(1).name() + " ... " + hosts.get(hosts.size() - 1).name();
    }

    private static String blankToNull(String text) {
        return text == null || text.isBlank() ? null : text.trim();
    }

    private static StringConverter<Duration> seconds() {
        return new StringConverter<>() {
            @Override
            public String toString(Duration duration) {
                return duration == null ? "" : String.format(Locale.ROOT, "every %d s", duration.toSeconds());
            }

            @Override
            public Duration fromString(String text) {
                return null;
            }
        };
    }
}

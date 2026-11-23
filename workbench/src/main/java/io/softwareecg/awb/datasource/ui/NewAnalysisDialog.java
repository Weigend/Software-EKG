package io.softwareecg.awb.datasource.ui;

import io.softwareecg.awb.datasource.model.AnalysisPlan;
import io.softwareecg.awb.datasource.spi.AnalysisSourceDefinition;
import io.softwareecg.awb.datasource.spi.AnalysisTopic;
import io.softwareecg.awb.datasource.model.CollectionPlan;
import io.softwareecg.awb.datasource.NewAnalysisService;
import io.softwareecg.awb.datasource.adapter.prometheus.MetricExpression;
import io.softwareecg.awb.datasource.adapter.prometheus.MetricProfile;
import io.softwareecg.awb.datasource.adapter.prometheus.PrometheusEnvironment;
import io.softwareecg.awb.datasource.adapter.prometheus.PrometheusEnvironment.Group;
import io.softwareecg.awb.datasource.adapter.prometheus.WildcardPattern;
import io.softwareecg.wfx.lookup.api.Lookup;
import io.softwareecg.wfx.platform.api.EventBus;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.RadioButton;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.control.TitledPane;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;

import java.net.URI;
import java.text.NumberFormat;
import java.time.Duration;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.EventObject;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * The way to an analysis with current data, in one dialog. It asks in the order a person
 * thinks: <b>what</b> the analysis is about, <b>how</b> the data comes, and then - depending on
 * the answer - <b>where</b> it is.
 * <ol>
 *   <li>What: the topics as tiles ({@link AnalysisTopic}) - Linux servers, Java processes and
 *       so on. Several can be chosen; topics that are planned but not built are shown greyed
 *       out. Whoever only has an address takes the tile "show me what is there" instead.</li>
 *   <li>How: for every topic the source ({@link AnalysisSourceDefinition}). Linux hosts can come through a
 *       Prometheus server that already gathered their values, or be read directly by SSH and
 *       top - the choice says what each way needs and gives.</li>
 *   <li>Where: one step per source in use. A Prometheus server is one address; the systems it
 *       monitors are found, assigned to the topics and selected. Hosts read directly are named
 *       ({@link SshPage} for Unix, {@link WindowsPage} for Windows).</li>
 * </ol>
 * Every source becomes a data source of its own in the same analysis. Everything the user
 * does not have to decide has a default: the usual source, the essential metrics, the update
 * interval.
 */
public class NewAnalysisDialog extends Dialog<Void> {

    private static final NumberFormat NUMBERS = NumberFormat.getIntegerInstance(Locale.getDefault());

    /** The size of every tile: three of them fill a row of the dialog. */
    private static final double TILE_WIDTH = 228;
    private static final double TILE_HEIGHT = 150;

    /** The width of one of several choices standing side by side, so that they form columns. */
    private static final double CHOICE_WIDTH = 240;

    private final NewAnalysisService service;

    private final TextField urlField = new TextField("http://localhost:9090");
    private final Button connectButton = new Button("Connect");
    private final TextField usernameField = new TextField();
    private final PasswordField passwordField = new PasswordField();
    private final TitledPane authenticationPane = new TitledPane();
    /** The question of the step shown, and what there is to say about it, in the header of the dialog. */
    private final Label stepTitle = new Label();
    private final Label stepHint = new Label();
    private final HBox stepTrail = new HBox(6);
    private final Label statusLabel = new Label("Enter the address of your Prometheus server.");
    private final VBox groupsBox = new VBox(12);
    private final TextField nameField = new TextField();
    private final ComboBox<Duration> lookbackBox = new ComboBox<>();
    private final ComboBox<Duration> intervalBox = new ComboBox<>();

    private final List<GroupRow> groupRows = new ArrayList<>();

    private final Map<AnalysisTopic, ToggleButton> tiles = new EnumMap<>(AnalysisTopic.class);

    /**
     * The topics that come from the Prometheus server. Empty means "show me what is there":
     * nothing is assumed about the environment.
     */
    private final Set<AnalysisTopic> chosenTopics = EnumSet.noneOf(AnalysisTopic.class);

    /** How each topic is collected, as chosen in the second step. */
    private final Map<AnalysisTopic, AnalysisSourceDefinition> chosenSources = new EnumMap<>(AnalysisTopic.class);

    private final VBox sourceRows = new VBox(16);

    /** The steps of the dialog. Which of the last three are taken depends on the sources chosen. */
    private enum Step { TOPICS, SOURCES, PROMETHEUS, SSH, WINDOWS }

    private List<Step> steps = List.of(Step.TOPICS);

    private int stepIndex;

    private Node topicPage;
    private Node sourcePage;
    private Node connectionPage;
    private SshPage sshPage;
    private WindowsPage windowsPage;

    /**
     * Why this AWB cannot read Windows performance counters - it does not run on Windows -
     * or {@code null} if it can.
     */
    private final String windowsProblem;

    /** How many of the plans of this dialog are started already; a retry goes on after them. */
    private int plansStarted;

    /** The analysis this dialog has created, once it has. */
    private String createdAnalysis;

    private PrometheusEnvironment environment;

    /** "Continue" in the first step, "Start Analysis" in the second. */
    private Button startButton;
    private Button backButton;

    private final String startText;

    /**
     * The analysis a further source is added to; {@code null} when a new analysis is started.
     */
    private final String existingAnalysis;

    /**
     * Starts a new analysis.
     */
    public NewAnalysisDialog(NewAnalysisService service) {
        this(service, null);
    }

    /**
     * Adds a further data source to an existing analysis.
     *
     * @param existingAnalysis the name of the analysis, or {@code null} to start a new one
     */
    public NewAnalysisDialog(NewAnalysisService service, String existingAnalysis) {
        this.service = service;
        this.existingAnalysis = existingAnalysis;
        this.windowsProblem = service.windowsCountersProblem();

        this.startText = existingAnalysis == null ? "Start Analysis" : "Add Source";

        setTitle(existingAnalysis == null ? "New Analysis" : "Add Data Source to " + existingAnalysis);
        setResizable(true);

        ButtonType backType = new ButtonType("Back", ButtonBar.ButtonData.BACK_PREVIOUS);
        ButtonType startType = new ButtonType("Continue", ButtonBar.ButtonData.OK_DONE);
        getDialogPane().getButtonTypes().addAll(backType, startType, ButtonType.CANCEL);
        getDialogPane().getStylesheets().add(
                NewAnalysisDialog.class.getResource("NewAnalysisDialog.css").toExternalForm());

        // all steps share the space, so that the dialog keeps its size from one to the other
        topicPage = buildTopicPage();
        sourcePage = buildSourcePage();
        connectionPage = buildContent();
        sshPage = new SshPage(service, existingAnalysis, this::updateStartButton);
        windowsPage = new WindowsPage(service, existingAnalysis, this::updateStartButton);
        // the name is asked wherever the last step is; it is one name
        if (existingAnalysis == null) {
            sshPage.nameProperty().bindBidirectional(nameField.textProperty());
            windowsPage.nameProperty().bindBidirectional(nameField.textProperty());
        }
        getDialogPane().setHeader(buildHeader());
        StackPane pages = new StackPane(topicPage, sourcePage, connectionPage, sshPage.node(), windowsPage.node());
        pages.getStyleClass().add("ekg-pages");
        getDialogPane().setContent(pages);
        getDialogPane().getStyleClass().add("ekg-wizard");
        getDialogPane().setPrefWidth(740);

        startButton = (Button) getDialogPane().lookupButton(startType);
        // the dialog stays open until the analysis really started
        startButton.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
            event.consume();
            if (isLastStep()) {
                startAnalysis();
            } else {
                next();
            }
        });
        backButton = (Button) getDialogPane().lookupButton(backType);
        backButton.setId("back");
        backButton.managedProperty().bind(backButton.visibleProperty());
        // the text of the start button changes with the step; it must not keep the width of the first one
        ButtonBar.setButtonUniformSize(startButton, false);
        startButton.setMinWidth(Region.USE_PREF_SIZE);
        backButton.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
            event.consume();
            stepIndex--;
            showStep();
        });

        showStep();
    }

    //=================================================================================================================
    // step 1: what the analysis is about
    //=================================================================================================================

    private Node buildTopicPage() {
        VBox page = new VBox(8);
        for (AnalysisTopic.Category category : AnalysisTopic.Category.values()) {
            Label title = band(category.title());
            HBox row = new HBox(12);
            for (AnalysisTopic topic : AnalysisTopic.values()) {
                if (topic.category() == category) {
                    ToggleButton tile = topicTile(topic);
                    tiles.put(topic, tile);
                    row.getChildren().add(tile);
                }
            }
            page.getChildren().addAll(title, row);
        }

        Button explore = new Button();
        explore.setId("tile-explore");
        explore.getStyleClass().setAll("topic-tile", "explore-tile");
        Label exploreTitle = new Label("I have a Prometheus \u2013 show me what is there");
        exploreTitle.getStyleClass().add("tile-title");
        Label exploreDescription = new Label("Connect the server and choose from everything it monitors");
        exploreDescription.getStyleClass().add("tile-description");
        HBox exploreContent = new HBox(14, TopicIcons.explore(), new VBox(2, exploreTitle, exploreDescription));
        exploreContent.setAlignment(Pos.CENTER_LEFT);
        exploreContent.setPadding(new Insets(4, 8, 4, 8));
        explore.setGraphic(exploreContent);
        explore.setMaxWidth(Double.MAX_VALUE);
        explore.setAlignment(Pos.CENTER_LEFT);
        explore.setOnAction(e -> {
            tiles.values().forEach(tile -> tile.setSelected(false));
            next();
        });

        page.getChildren().addAll(band("Or start from what is there"), explore);
        page.setPadding(new Insets(12));
        return page;
    }

    /**
     * One topic as a tile: icon, name and a line on what it covers. A topic that is not
     * available yet is shown all the same, greyed out and marked.
     */
    private ToggleButton topicTile(AnalysisTopic topic) {
        Label title = new Label(topic.title());
        title.getStyleClass().add("tile-title");
        Label description = new Label(topic.description());
        description.getStyleClass().add("tile-description");
        description.setWrapText(true);
        description.setAlignment(Pos.TOP_CENTER);
        description.setPrefWidth(180);
        description.setMinHeight(34);
        VBox content = new VBox(4, TopicIcons.of(topic), title, description);
        content.setAlignment(Pos.CENTER);
        content.setPadding(new Insets(12, 4, 4, 4));

        ToggleButton tile = new ToggleButton();
        tile.setId("tile-" + topic.name());
        tile.getStyleClass().setAll("topic-tile");

        // top right: the mark of a chosen topic, or the note why the topic cannot be chosen
        boolean usable = usable(topic);
        Label corner = new Label(usable ? "\u2713" : topic.isAvailable() ? unusableNote() : "coming soon");
        corner.getStyleClass().add(usable ? "tile-check" : "tile-badge");
        if (usable) {
            corner.visibleProperty().bind(tile.selectedProperty());
        }
        StackPane.setAlignment(corner, Pos.TOP_RIGHT);

        tile.setGraphic(new StackPane(content, corner));
        fixSize(tile);
        tile.setDisable(!usable);
        tile.selectedProperty().addListener((o, ov, nv) -> updateStartButton());
        return tile;
    }

    /** A topic can be chosen if it is built and one of the ways to collect it can be gone here. */
    private boolean usable(AnalysisTopic topic) {
        return topic.isAvailable() && topic.sources().stream().anyMatch(this::usable);
    }

    /** A way can be gone if it is built and - for Windows performance counters - this is a Windows. */
    private boolean usable(AnalysisTopic.SourceOption option) {
        return option.available() && (option.source() != AnalysisSourceDefinition.WINDOWS_COUNTERS || windowsProblem == null);
    }

    /** The few words on a tile that is built but cannot be chosen on this machine. */
    private static String unusableNote() {
        return System.getProperty("os.name", "").startsWith("Win") ? "not installed" : "on Windows only";
    }

    //=================================================================================================================
    // the steps
    //=================================================================================================================

    /**
     * The header of the dialog, as the other dialogs of the EKG have it: a blue band that says
     * what the step is about. It keeps its height from step to step, and on its right it shows
     * where in the sequence of "what, how, where" the dialog is.
     */
    private Node buildHeader() {
        stepTitle.setId("stepTitle");
        stepTitle.getStyleClass().add("header-title");
        stepHint.getStyleClass().add("header-hint");
        stepHint.setWrapText(true);
        // a width of its own, or the text is cut instead of broken into a second line
        stepHint.setPrefWidth(550);
        stepHint.setMinHeight(40);
        stepHint.setAlignment(Pos.TOP_LEFT);
        VBox texts = new VBox(4, stepTitle, stepHint);
        HBox.setHgrow(texts, Priority.ALWAYS);

        stepTrail.setAlignment(Pos.TOP_RIGHT);
        stepTrail.setMinWidth(Region.USE_PREF_SIZE);

        HBox header = new HBox(16, texts, stepTrail);
        header.getStyleClass().add("ekg-header");
        return header;
    }

    /** Shows "What > How > Where" with the part the step belongs to set off. */
    private void showTrail(Step step) {
        int current = step == Step.TOPICS ? 0 : step == Step.SOURCES ? 1 : 2;
        String[] parts = {"What", "How", "Where"};
        stepTrail.getChildren().clear();
        for (int i = 0; i < parts.length; i++) {
            if (i > 0) {
                Label arrow = new Label("\u203a");
                arrow.getStyleClass().add("header-trail");
                stepTrail.getChildren().add(arrow);
            }
            Label part = new Label(parts[i]);
            part.getStyleClass().add(i == current ? "header-trail-current" : "header-trail");
            stepTrail.getChildren().add(part);
        }
    }

    private void describeStep(String title, String hint) {
        stepTitle.setText(title);
        stepHint.setText(hint);
    }

    private static Label band(String text) {
        Label band = new Label(text);
        band.getStyleClass().add("section-band");
        band.setMaxWidth(Double.MAX_VALUE);
        return band;
    }

    private Step currentStep() {
        return steps.get(stepIndex);
    }

    /** The topics and the sources are never the end: what follows them is only known afterwards. */
    private boolean isLastStep() {
        return currentStep() != Step.TOPICS && currentStep() != Step.SOURCES && stepIndex == steps.size() - 1;
    }

    private void next() {
        if (currentStep() == Step.TOPICS) {
            Set<AnalysisTopic> selected = EnumSet.noneOf(AnalysisTopic.class);
            tiles.forEach((topic, tile) -> {
                if (tile.isSelected()) {
                    selected.add(topic);
                }
            });
            if (selected.isEmpty()) {
                // "show me what is there": a Prometheus server, nothing assumed about it
                chosenTopics.clear();
                steps = List.of(Step.TOPICS, Step.PROMETHEUS);
            } else {
                showSourceChoices(selected);
                steps = List.of(Step.TOPICS, Step.SOURCES);
            }
        } else if (currentStep() == Step.SOURCES) {
            chosenTopics.clear();
            chosenSources.forEach((topic, source) -> {
                if (source == AnalysisSourceDefinition.PROMETHEUS) {
                    chosenTopics.add(topic);
                }
            });
            List<Step> next = new ArrayList<>(List.of(Step.TOPICS, Step.SOURCES));
            if (!chosenTopics.isEmpty()) {
                next.add(Step.PROMETHEUS);
            }
            if (chosenSources.containsValue(AnalysisSourceDefinition.SSH_TOP)) {
                next.add(Step.SSH);
            }
            if (chosenSources.containsValue(AnalysisSourceDefinition.WINDOWS_COUNTERS)) {
                next.add(Step.WINDOWS);
            }
            steps = next;
        }
        stepIndex++;
        showStep();
    }

    private void showStep() {
        Step step = currentStep();
        topicPage.setVisible(step == Step.TOPICS);
        sourcePage.setVisible(step == Step.SOURCES);
        connectionPage.setVisible(step == Step.PROMETHEUS);
        sshPage.node().setVisible(step == Step.SSH);
        windowsPage.node().setVisible(step == Step.WINDOWS);

        showTrail(step);
        String target = existingAnalysis == null ? "" : " The data is added to the analysis '" + existingAnalysis + "'.";
        switch (step) {
            case TOPICS -> describeStep(existingAnalysis == null ? "What do you want to analyse?" : "What do you want to add?",
                    "Choose one or more topics. Each brings its essential metrics; how the data comes and where it is "
                            + "follow in the next steps.");
            case SOURCES -> describeStep("How does the data come?",
                    "Most things can be collected in more than one way. The ways differ in what they need and what "
                            + "they give; choose one for each topic.");
            case PROMETHEUS -> describeStep(chosenTopics.isEmpty()
                            ? "Connect your Prometheus server"
                            : "Where is the Prometheus server for " + titles(chosenTopics) + "?",
                    (chosenTopics.isEmpty()
                            ? "You then choose from everything the server monitors."
                            : "It monitors these systems; they are found and selected for you.") + target);
            case SSH -> describeStep("Which Linux / Unix hosts do you want to read?",
                    "They are read directly, by SSH and top: nothing to install, no Prometheus needed. "
                            + "Values are collected from now on, for as long as the analysis is running." + target);
            case WINDOWS -> describeStep("Which Windows hosts do you want to read?",
                    "They are read directly, through their performance counters: nothing to install. "
                            + "Values are collected from now on, for as long as the analysis is running." + target);
        }

        backButton.setVisible(stepIndex > 0);
        startButton.setText(isLastStep() ? startText : "Continue");
        startButton.setId(isLastStep() ? "startAnalysis" : "continue");

        if (step == Step.PROMETHEUS) {
            showConnectionStep();
        } else if (step == Step.SSH) {
            sshPage.focus();
        } else if (step == Step.WINDOWS) {
            windowsPage.focus();
        }
        updateStartButton();
    }

    //=================================================================================================================
    // step 2: how the data comes
    //=================================================================================================================

    private Node buildSourcePage() {
        VBox page = new VBox(8, sourceRows);
        page.setPadding(new Insets(12));
        return page;
    }

    /**
     * Offers the sources of every chosen topic. A choice made before stays; otherwise the
     * usual source of the topic is selected.
     */
    private void showSourceChoices(Set<AnalysisTopic> topics) {
        chosenSources.keySet().retainAll(topics);
        sourceRows.getChildren().clear();

        for (AnalysisTopic topic : topics) {
            Label title = band(topic.title());

            ToggleGroup choice = new ToggleGroup();
            HBox options = new HBox(12);
            for (AnalysisTopic.SourceOption option : topic.sources()) {
                ToggleButton tile = sourceTile(topic, option);
                tile.setToggleGroup(choice);
                options.getChildren().add(tile);
                if (usable(option) && (chosenSources.get(topic) == option.source()
                        || (!chosenSources.containsKey(topic) && choice.getSelectedToggle() == null))) {
                    tile.setSelected(true);
                    chosenSources.put(topic, option.source());
                }
            }
            // one way is always chosen: a click on the chosen one does not take it away
            choice.selectedToggleProperty().addListener((o, previous, selected) -> {
                if (selected == null) {
                    previous.setSelected(true);
                } else {
                    chosenSources.put(topic, (AnalysisSourceDefinition) selected.getUserData());
                }
            });
            sourceRows.getChildren().add(new VBox(8, title, options));
        }
    }

    /**
     * One way to collect a topic as a tile of the size the topics have: what it is called,
     * whether it is one server for all or each system by itself, and what it needs and gives.
     */
    private ToggleButton sourceTile(AnalysisTopic topic, AnalysisTopic.SourceOption option) {
        Label title = new Label(option.source().title());
        title.getStyleClass().add("tile-title");
        Label kind = new Label(option.source().isAggregating() ? "one server for all" : "each system directly");
        kind.getStyleClass().add("tile-kind");
        boolean usable = usable(option);
        // a way that is built but cannot be gone on this machine says why
        Label description = new Label(option.available() && !usable ? windowsProblem : option.description());
        description.getStyleClass().add("tile-description");
        description.setStyle("-fx-text-alignment: left;");
        description.setWrapText(true);
        description.setPrefWidth(TILE_WIDTH - 28);
        description.setAlignment(Pos.TOP_LEFT);
        VBox.setVgrow(description, Priority.ALWAYS);
        VBox content = new VBox(4, title, kind, description);
        content.setPadding(new Insets(6, 4, 4, 6));
        content.setAlignment(Pos.TOP_LEFT);

        ToggleButton tile = new ToggleButton();
        tile.setId("source-" + topic.name() + "-" + option.source().name());
        tile.getStyleClass().setAll("topic-tile");
        tile.setUserData(option.source());

        Label corner = new Label(usable ? "\u2713" : option.available() ? unusableNote() : "coming soon");
        corner.getStyleClass().add(usable ? "tile-check" : "tile-badge");
        if (usable) {
            corner.visibleProperty().bind(tile.selectedProperty());
        }
        StackPane.setAlignment(corner, Pos.TOP_RIGHT);

        tile.setGraphic(new StackPane(content, corner));
        tile.setAlignment(Pos.TOP_LEFT);
        fixSize(tile);
        tile.setDisable(!usable);
        return tile;
    }

    /** Every tile of the dialog has the same size, whatever stands on it. */
    private static void fixSize(ToggleButton tile) {
        tile.setMinSize(TILE_WIDTH, TILE_HEIGHT);
        tile.setPrefSize(TILE_WIDTH, TILE_HEIGHT);
        tile.setMaxSize(TILE_WIDTH, TILE_HEIGHT);
    }

    //=================================================================================================================
    // step 3: where the data is
    //=================================================================================================================

    private void showConnectionStep() {
        if (environment != null) {
            // the server is known already: show it again under the topics chosen now
            showEnvironment(environment);
        }
        Platform.runLater(urlField::requestFocus);
    }

    private static String titles(Set<AnalysisTopic> topics) {
        List<String> titles = topics.stream().map(AnalysisTopic::title).collect(Collectors.toList());
        if (titles.size() == 1) {
            return titles.get(0);
        }
        return String.join(", ", titles.subList(0, titles.size() - 1)) + " and " + titles.get(titles.size() - 1);
    }

    //=================================================================================================================
    // the Prometheus server
    //=================================================================================================================

    private Node buildContent() {
        urlField.setId("prometheusUrl");
        urlField.setPromptText("http://prometheus.example:9090");
        urlField.setOnAction(e -> connect());
        connectButton.setId("connect");
        connectButton.setOnAction(e -> connect());
        HBox.setHgrow(urlField, Priority.ALWAYS);
        HBox urlRow = new HBox(8, new Label("Address"), urlField, connectButton);
        urlRow.setAlignment(Pos.CENTER_LEFT);

        GridPane credentials = new GridPane();
        credentials.setHgap(8);
        credentials.setVgap(6);
        credentials.addRow(0, new Label("User"), usernameField);
        credentials.addRow(1, new Label("Password or token"), passwordField);
        usernameField.setPromptText("leave empty for a bearer token");
        usernameField.setPrefColumnCount(24);
        authenticationPane.setText("Authentication (only if the server asks for it)");
        authenticationPane.setContent(credentials);
        authenticationPane.setExpanded(false);
        authenticationPane.setAnimated(false);

        statusLabel.setId("status");
        statusLabel.setWrapText(true);

        ScrollPane groupsScroll = new ScrollPane(groupsBox);
        groupsScroll.setFitToWidth(true);
        groupsScroll.setPrefViewportHeight(280);
        groupsScroll.setStyle("-fx-background-color: transparent;");
        groupsBox.setPadding(new Insets(4, 8, 4, 4));
        VBox.setVgrow(groupsScroll, Priority.ALWAYS);

        nameField.setId("analysisName");
        nameField.setPromptText("name of the analysis");
        nameField.textProperty().addListener((o, ov, nv) -> updateStartButton());
        if (existingAnalysis != null) {
            nameField.setText(existingAnalysis);
            nameField.setDisable(true);
        }

        lookbackBox.getItems().addAll(Duration.ofHours(1), Duration.ofHours(6), Duration.ofHours(24), Duration.ofDays(7));
        lookbackBox.setValue(Duration.ofHours(24));
        lookbackBox.setConverter(durations("last "));
        intervalBox.getItems().addAll(Duration.ofSeconds(15), Duration.ofSeconds(60), Duration.ofMinutes(5));
        intervalBox.setValue(Duration.ofSeconds(60));
        intervalBox.setConverter(durations("every "));

        HBox.setHgrow(nameField, Priority.ALWAYS);
        HBox settingsRow = new HBox(8, new Label("Name"), nameField,
                new Label("Time range"), lookbackBox, new Label("Update"), intervalBox);
        settingsRow.setAlignment(Pos.CENTER_LEFT);

        VBox content = new VBox(8, band("Server"), urlRow, authenticationPane, statusLabel,
                band("What it monitors"), groupsScroll, band("Analysis"), settingsRow);
        content.setPadding(new Insets(12));
        return content;
    }

    /**
     * One discovered group of systems with the choice of what to collect for it: which of its
     * systems (a pattern such as {@code db*}, resolved as it is typed) and which metrics (the
     * essentials one by one, further ones by pattern, or everything). Only a selected group
     * shows these choices; whoever changes none of them gets all systems with the essentials.
     */
    private final class GroupRow {

        private final Group group;
        /** "host" for servers, "system" for anything else a group may consist of. */
        private final String systemWord;
        private final CheckBox selected;
        private final TextField hostsField = new TextField();
        private final Label hostsFound = new Label();
        private final RadioButton essentials = new RadioButton();
        private final RadioButton everything = new RadioButton();
        /** The essentials on offer, by what identifies them whatever the interval or the systems. */
        private final Map<String, CheckBox> essentialChoices = new java.util.LinkedHashMap<>();
        private final TextField moreField = new TextField();
        private final Label moreFound = new Label();
        private final VBox node;

        GroupRow(Group group, boolean preselected) {
            this.group = group;
            boolean hosts = MetricProfile.topicsOf(group).contains(AnalysisTopic.LINUX);
            this.systemWord = hosts ? "host" : "system";

            int systems = group.systems().size();
            selected = new CheckBox(group.name() + "    " + systems + (systems == 1 ? " system" : " systems")
                    + "    " + abbreviate(group.systems()));
            selected.setId("group-" + group.name());
            selected.setStyle("-fx-font-weight: bold;");
            selected.setSelected(preselected);

            hostsField.setId("hosts-" + group.name());
            hostsField.setPromptText("all " + systemWord + "s \u2013 or e.g. db*, web-0?");
            fixWidth(hostsField, 250);
            hostsField.setTooltip(new Tooltip("Names or patterns, separated by commas. * stands for any text, ? for one character.\n"
                    + "The pattern is kept: " + systemWord + "s that match it later are collected as well."));
            hostsFound.setId("hostsFound-" + group.name());
            hostsFound.getStyleClass().add("found-detail");
            hostsFound.setMinWidth(0);
            HBox hostsRow = new HBox(8, hostsField, hostsFound);
            hostsRow.setAlignment(Pos.CENTER_LEFT);

            ToggleGroup choice = new ToggleGroup();
            essentials.setToggleGroup(choice);
            everything.setToggleGroup(choice);
            essentials.setSelected(true);

            FlowPane essentialsPane = new FlowPane(8, 4);
            essentialsPane.setPrefWrapLength(2 * CHOICE_WIDTH + 8);
            for (MetricExpression expression : MetricProfile.essentials(group, intervalBox.getValue())) {
                CheckBox metric = new CheckBox(displayName(expression));
                metric.getStyleClass().add("essential-metric");
                // one width for all, so that they stand in columns
                metric.setPrefWidth(CHOICE_WIDTH);
                metric.setSelected(true);
                metric.selectedProperty().addListener((o, ov, nv) -> update());
                essentialChoices.put(keyOf(expression), metric);
                essentialsPane.getChildren().add(metric);
            }
            moreField.setId("more-" + group.name());
            moreField.setPromptText("further metrics, e.g. " + exampleMetricPattern(group));
            fixWidth(moreField, 250);
            moreField.setTooltip(new Tooltip("Further metrics of this group by name or pattern, separated by commas,\n"
                    + "collected as they are in Prometheus."));
            moreFound.setId("moreFound-" + group.name());
            moreFound.getStyleClass().add("found-detail");
            moreFound.setMinWidth(0);
            HBox moreRow = new HBox(8, moreField, moreFound);
            moreRow.setAlignment(Pos.CENTER_LEFT);
            VBox essentialDetails = new VBox(6, essentialsPane, moreRow);
            essentialDetails.setPadding(new Insets(0, 0, 2, 26));
            essentialDetails.disableProperty().bind(essentials.selectedProperty().not());

            if (essentialChoices.isEmpty()) {
                essentials.setDisable(true);
                everything.setSelected(true);
            }

            GridPane details = new GridPane();
            details.setHgap(10);
            details.setVgap(6);
            details.setPadding(new Insets(2, 0, 4, 26));
            Label hostsLabel = new Label(hosts ? "Hosts" : "Systems");
            Label metricsLabel = new Label("Metrics");
            hostsLabel.setMinWidth(Region.USE_PREF_SIZE);
            metricsLabel.setMinWidth(Region.USE_PREF_SIZE);
            GridPane.setValignment(metricsLabel, javafx.geometry.VPos.TOP);
            details.add(hostsLabel, 0, 0);
            details.add(hostsRow, 1, 0);
            details.add(metricsLabel, 0, 1);
            details.add(new VBox(4, essentials, essentialDetails, everything), 1, 1);
            GridPane.setHgrow(hostsRow, Priority.ALWAYS);
            // only a selected group shows its choices, so that the list of groups stays short
            details.visibleProperty().bind(selected.selectedProperty());
            details.managedProperty().bind(selected.selectedProperty());

            node = new VBox(4, selected, details);
            selected.selectedProperty().addListener((o, ov, nv) -> updateStartButton());
            hostsField.textProperty().addListener((o, ov, nv) -> update());
            moreField.textProperty().addListener((o, ov, nv) -> update());
            choice.selectedToggleProperty().addListener((o, ov, nv) -> update());
            update();
        }

        /** Shows what the patterns select and how much that is. */
        private void update() {
            List<String> matching = MetricProfile.systemsMatching(group, hostPattern());
            int all = group.systems().size();
            String word = systemWord;
            if (matching.isEmpty()) {
                hostsFound.setText("no " + word + " matches");
                hostsFound.setStyle("-fx-text-fill: #b42318;");
            } else {
                hostsFound.setStyle("");
                hostsFound.setText(matching.size() == all
                        ? "all " + all + " " + word + (all == 1 ? "" : "s")
                        : matching.size() + " of " + all + ":  " + shorten(matching));
            }
            hostsFound.setTooltip(matching.isEmpty() || matching.size() <= 3 ? null : new Tooltip(String.join("\n", matching)));

            Duration interval = intervalBox.getValue();
            List<String> more = furtherMetrics(chosenEssentials(interval));
            boolean typed = moreField.getText() != null && !moreField.getText().isBlank();
            moreFound.setStyle(typed && more.isEmpty() ? "-fx-text-fill: #b42318;" : "");
            moreFound.setText(!typed ? "" : more.isEmpty() ? "no further metric matches"
                    : more.size() + (more.size() == 1 ? " metric:  " : " metrics:  ") + shorten(more));
            moreFound.setTooltip(more.size() <= 3 ? null : new Tooltip(String.join("\n", more)));

            int series = MetricProfile.estimatedSeries(group, chosenEssentials(interval), matching.size())
                    + MetricProfile.estimatedSeriesOf(group, more, matching.size());
            essentials.setText("Essentials    ~" + NUMBERS.format(series) + " series");
            everything.setText("Everything    " + NUMBERS.format(group.metricCount()) + " metrics    "
                    + (matching.size() == all ? NUMBERS.format(group.seriesCount())
                    : "~" + NUMBERS.format((long) Math.ceil((double) group.seriesCount() * matching.size() / Math.max(1, all))))
                    + " series");
            updateStartButton();
        }

        private WildcardPattern hostPattern() {
            return WildcardPattern.parse(hostsField.getText());
        }

        /** The essentials that are ticked. */
        private List<MetricExpression> chosenEssentials(Duration interval) {
            return MetricProfile.essentials(group, interval, hostPattern()).stream()
                    .filter(expression -> {
                        CheckBox choice = essentialChoices.get(keyOf(expression));
                        return choice != null && choice.isSelected();
                    })
                    .collect(Collectors.toList());
        }

        /**
         * The metrics the pattern for further metrics selects - without those the essentials
         * collect unchanged already, which would otherwise be written twice.
         */
        private List<String> furtherMetrics(List<MetricExpression> chosen) {
            if (moreField.getText() == null || moreField.getText().isBlank()) {
                return List.of();
            }
            Set<String> plain = chosen.stream().filter(expression -> expression.name() == null)
                    .map(NewAnalysisDialog::keyOf).collect(Collectors.toSet());
            return MetricProfile.metricsMatching(group, WildcardPattern.parse(moreField.getText())).stream()
                    .filter(name -> !plain.contains(name))
                    .collect(Collectors.toList());
        }

        /** Whether the choices of the group yield anything to collect. */
        boolean isComplete() {
            if (MetricProfile.systemsMatching(group, hostPattern()).isEmpty()) {
                return false;
            }
            if (everything.isSelected()) {
                return true;
            }
            List<MetricExpression> chosen = chosenEssentials(intervalBox.getValue());
            return !chosen.isEmpty() || !furtherMetrics(chosen).isEmpty();
        }

        AnalysisPlan.GroupSelection selection(Duration interval) {
            if (everything.isSelected()) {
                return new AnalysisPlan.GroupSelection(group.name(), MetricProfile.everything(group, hostPattern()));
            }
            List<MetricExpression> expressions = new ArrayList<>(chosenEssentials(interval));
            List<String> more = furtherMetrics(expressions);
            if (!more.isEmpty()) {
                expressions.add(MetricProfile.metrics(group, hostPattern(), more));
            }
            return new AnalysisPlan.GroupSelection(group.name(), expressions);
        }
    }

    //=================================================================================================================
    // behaviour
    //=================================================================================================================

    private void connect() {
        String url = urlField.getText() == null ? "" : urlField.getText().trim();
        if (url.isEmpty()) {
            return;
        }
        String username = usernameField.getText();
        String password = passwordField.getText();

        connectButton.setDisable(true);
        statusLabel.setStyle("");
        statusLabel.setText("Connecting to " + url + " ...");

        runInBackground("discover-prometheus", () -> {
            try {
                PrometheusEnvironment found = service.discover(url, username, password);
                Platform.runLater(() -> showEnvironment(found));
            } catch (RuntimeException e) {
                Platform.runLater(() -> showConnectionProblem(e));
            }
        });
    }

    private void showEnvironment(PrometheusEnvironment found) {
        environment = found;
        connectButton.setDisable(false);
        groupRows.clear();
        groupsBox.getChildren().clear();

        if (found.groups().isEmpty()) {
            statusLabel.setText("Connected, but this Prometheus has no data yet.");
            updateStartButton();
            return;
        }

        if (chosenTopics.isEmpty()) {
            showEverythingFound(found);
        } else {
            showFoundByTopic(found);
        }

        if (nameField.getText() == null || nameField.getText().isBlank()) {
            nameField.setText(defaultName(found.url()));
        }
        updateStartButton();
    }

    /**
     * "Show me what is there": every group the server has, the largest one selected.
     */
    private void showEverythingFound(PrometheusEnvironment found) {
        int systems = found.systemCount();
        statusLabel.setStyle("-fx-text-fill: #1a7f37;");
        statusLabel.setText("Connected. Found " + found.groups().size() + (found.groups().size() == 1 ? " group" : " groups")
                + " with " + systems + (systems == 1 ? " system." : " systems.")
                + " Choose what you want to analyse:");

        // the largest group is what an analysis usually is about; the others are one click away
        for (int i = 0; i < found.groups().size(); i++) {
            addGroupRow(found.groups().get(i), i == 0, false);
        }
    }

    /**
     * The groups of the server under the topics chosen in the first step. What belongs to a
     * chosen topic is selected; what the server has besides is listed below, not selected.
     */
    private void showFoundByTopic(PrometheusEnvironment found) {
        List<Group> remaining = new ArrayList<>(found.groups());
        List<String> summary = new ArrayList<>();
        boolean anythingFound = false;

        for (AnalysisTopic topic : chosenTopics) {
            List<Group> matching = remaining.stream()
                    .filter(group -> MetricProfile.topicsOf(group).contains(topic))
                    .collect(Collectors.toList());
            remaining.removeAll(matching);

            int systems = matching.stream().mapToInt(group -> group.systems().size()).sum();
            Label title = new Label(topic.title());
            title.getStyleClass().add("found-topic");
            Label detail = new Label(matching.isEmpty()
                    ? "nothing found on this server"
                    : systems + (systems == 1 ? " system" : " systems") + " found");
            detail.getStyleClass().add(matching.isEmpty() ? "found-nothing" : "found-detail");
            HBox header = new HBox(10, title, detail);
            header.setId("found-" + topic.name());
            header.setAlignment(Pos.BASELINE_LEFT);
            groupsBox.getChildren().add(header);

            matching.forEach(group -> addGroupRow(group, true, true));
            summary.add(topic.title() + ": " + (matching.isEmpty() ? "nothing found"
                    : systems + (systems == 1 ? " system" : " systems")));
            anythingFound |= !matching.isEmpty();
        }

        if (!remaining.isEmpty()) {
            Label title = new Label("Also found");
            title.getStyleClass().add("found-topic");
            Label detail = new Label("not part of what you chose \u2013 tick to include");
            detail.getStyleClass().add("found-detail");
            HBox header = new HBox(10, title, detail);
            header.setId("found-other");
            header.setAlignment(Pos.BASELINE_LEFT);
            header.setPadding(new Insets(6, 0, 0, 0));
            groupsBox.getChildren().add(header);
            remaining.forEach(group -> addGroupRow(group, false, true));
        }

        if (anythingFound) {
            statusLabel.setStyle("-fx-text-fill: #1a7f37;");
            statusLabel.setText("Connected. " + String.join(". ", summary) + ".");
        } else {
            statusLabel.setStyle("-fx-text-fill: #8a6100;");
            statusLabel.setText("Connected, but this server monitors nothing for " + titles(chosenTopics) + "."
                    + (remaining.isEmpty() ? "" : " You can choose from what it has:"));
        }
    }

    private void addGroupRow(Group group, boolean preselected, boolean indented) {
        GroupRow row = new GroupRow(group, preselected);
        if (indented) {
            row.node.setPadding(new Insets(0, 0, 0, 12));
        }
        groupRows.add(row);
        groupsBox.getChildren().add(row.node);
    }

    private void showConnectionProblem(RuntimeException e) {
        environment = null;
        connectButton.setDisable(false);
        groupRows.clear();
        groupsBox.getChildren().clear();
        statusLabel.setStyle("-fx-text-fill: #b42318;");
        statusLabel.setText(e.getMessage() != null ? e.getMessage() : e.toString());
        if (e.getMessage() != null && e.getMessage().contains("credentials")) {
            authenticationPane.setExpanded(true);
            Platform.runLater(usernameField::requestFocus);
        }
        updateStartButton();
    }

    /**
     * The plan for the Prometheus server, from what is selected on its page.
     */
    private AnalysisPlan prometheusPlan(String projectName) {
        Duration interval = intervalBox.getValue();
        List<AnalysisPlan.GroupSelection> selections = groupRows.stream()
                .filter(row -> row.selected.isSelected())
                .map(row -> row.selection(interval))
                .collect(Collectors.toList());
        return new AnalysisPlan(projectName, environment.url(),
                blankToNull(usernameField.getText()), blankToNull(passwordField.getText()),
                selections, lookbackBox.getValue(), interval);
    }

    /**
     * Starts the analysis with one data source per source in use. If a later source cannot be
     * started, the analysis stays with the ones before it, the dialog says so and stays open;
     * another try goes on with the source that failed.
     */
    private void startAnalysis() {
        String name = nameField.getText() == null ? "" : nameField.getText().trim();
        List<CollectionPlan> plans = new ArrayList<>();
        if (steps.contains(Step.PROMETHEUS)) {
            if (environment == null) {
                return;
            }
            plans.add(prometheusPlan(name));
        }
        if (steps.contains(Step.SSH)) {
            plans.add(sshPage.plan(name));
        }
        if (steps.contains(Step.WINDOWS)) {
            plans.add(windowsPage.plan(name));
        }
        if (plans.isEmpty()) {
            return;
        }

        startButton.setDisable(true);
        showProgress("Starting the analysis ...", false);

        // subscribed before the start, so that fast first values are not missed
        FirstChartOpener firstChart = existingAnalysis == null && createdAnalysis == null
                ? FirstChartOpener.waitFor(eventBus(), service.getRepository(), plans.get(0)) : null;

        runInBackground("start-analysis", () -> {
            try {
                for (int i = plansStarted; i < plans.size(); i++) {
                    if (existingAnalysis == null && createdAnalysis == null) {
                        firstChart.started(service.startAnalysis(plans.get(i)));
                        createdAnalysis = name;
                    } else {
                        service.addSource(plans.get(i));
                    }
                    plansStarted = i + 1;
                }
                Platform.runLater(() -> {
                    setResult(null);
                    close();
                });
            } catch (RuntimeException e) {
                if (firstChart != null && createdAnalysis == null) {
                    firstChart.cancel();
                }
                String problem = e.getMessage() != null ? e.getMessage() : e.toString();
                Platform.runLater(() -> {
                    showProgress(plansStarted == 0 ? problem
                            : "The analysis is running, but " + plans.get(plansStarted).endpoint()
                            + " could not be added: " + problem, true);
                    updateStartButton();
                });
            }
        });
    }

    /** Says on the step shown what the start is doing, or what went wrong. */
    private void showProgress(String text, boolean problem) {
        if (currentStep() == Step.SSH) {
            sshPage.showMessage(text, problem);
        } else if (currentStep() == Step.WINDOWS) {
            windowsPage.showMessage(text, problem);
        } else {
            statusLabel.setStyle(problem ? "-fx-text-fill: #b42318;" : "");
            statusLabel.setText(text);
        }
    }

    private void updateStartButton() {
        if (startButton == null) {
            return;
        }
        if (steps == null || sshPage == null || windowsPage == null) {
            return;
        }
        switch (currentStep()) {
            case TOPICS -> {
                startButton.setDisable(tiles.values().stream().noneMatch(ToggleButton::isSelected));
                return;
            }
            case SOURCES -> {
                startButton.setDisable(false);
                return;
            }
            case SSH -> {
                startButton.setDisable(!sshPage.isComplete());
                return;
            }
            case WINDOWS -> {
                startButton.setDisable(!windowsPage.isComplete());
                return;
            }
            default -> {
                // the Prometheus server, below
            }
        }
        boolean ready = environment != null
                && nameField.getText() != null && !nameField.getText().isBlank()
                && groupRows.stream().anyMatch(row -> row.selected.isSelected())
                && groupRows.stream().filter(row -> row.selected.isSelected()).allMatch(GroupRow::isComplete);
        startButton.setDisable(!ready);
    }

    //=================================================================================================================
    // helpers
    //=================================================================================================================

    @SuppressWarnings("unchecked")
    private static EventBus<EventObject> eventBus() {
        return Lookup.lookup(EventBus.class);
    }

    private static void runInBackground(String name, Runnable work) {
        Thread thread = new Thread(work, name);
        thread.setDaemon(true);
        thread.start();
    }

    /** A field keeps its width; what stands next to it gets shortened instead. */
    private static void fixWidth(TextField field, double width) {
        field.setMinWidth(width);
        field.setPrefWidth(width);
        field.setMaxWidth(width);
    }

    /** "cpu usage %", "node load1" - an essential metric as the user reads it. */
    private static String displayName(MetricExpression expression) {
        return keyOf(expression).replace("_per_second", "/s").replace("_percent", " %").replace('_', ' ');
    }

    /** What identifies a metric of a profile: its name, or the plain metric it stands for. */
    private static String keyOf(MetricExpression expression) {
        if (expression.name() != null) {
            return expression.name();
        }
        int brace = expression.expression().indexOf('{');
        return brace < 0 ? expression.expression() : expression.expression().substring(0, brace);
    }

    /** The first names of a list, and how many more there are. */
    private static String shorten(List<String> names) {
        int shown = Math.min(3, names.size());
        String text = String.join(", ", names.subList(0, shown));
        return names.size() > shown ? text + ", +" + (names.size() - shown) + " more" : text;
    }

    /** A pattern that fits metrics the group really has, as the example in the empty field. */
    private static String exampleMetricPattern(Group group) {
        return group.seriesByMetric().keySet().stream()
                .filter(name -> name.indexOf('_') > 0 && name.indexOf('_') < name.lastIndexOf('_'))
                .map(name -> name.substring(0, name.indexOf('_', name.indexOf('_') + 1)) + "_*")
                .collect(Collectors.groupingBy(prefix -> prefix, java.util.TreeMap::new, Collectors.counting()))
                .entrySet().stream()
                .filter(entry -> entry.getValue() > 1)
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("*_total");
    }

    private static String abbreviate(List<String> systems) {
        if (systems.size() <= 3) {
            return String.join(", ", systems);
        }
        return systems.get(0) + " ... " + systems.get(systems.size() - 1);
    }

    private static String defaultName(String url) {
        try {
            String host = URI.create(url).getHost();
            return host == null || host.isBlank() ? "Prometheus" : host;
        } catch (IllegalArgumentException e) {
            return "Prometheus";
        }
    }

    private static String blankToNull(String text) {
        return text == null || text.isBlank() ? null : text;
    }

    private static StringConverter<Duration> durations(String prefix) {
        return new StringConverter<>() {
            @Override
            public String toString(Duration duration) {
                if (duration == null) {
                    return "";
                }
                long seconds = duration.toSeconds();
                if (seconds % 86_400 == 0 && seconds > 86_400) {
                    return prefix + plural(seconds / 86_400, "day");
                }
                if (seconds % 3_600 == 0) {
                    return prefix + plural(seconds / 3_600, "hour");
                }
                if (seconds % 60 == 0 && seconds > 60) {
                    return prefix + plural(seconds / 60, "minute");
                }
                return prefix + seconds + " s";
            }

            @Override
            public Duration fromString(String text) {
                return null;
            }
        };
    }

    private static String plural(long amount, String unit) {
        return amount == 1 ? unit : amount + " " + unit + "s";
    }
}

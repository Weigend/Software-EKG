package io.softwareecg.awb.datasource.ui;

import io.softwareecg.awb.da.api.collection.CollectionSourceEvent;
import io.softwareecg.awb.datasource.bl.CollectorConfigCompiler;
import io.softwareecg.awb.datasource.bl.DataCollectionService;
import io.softwareecg.awb.datasource.bl.DataSourceInfo;
import io.softwareecg.awb.datasource.bl.DataSourceInfo.State;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.util.Duration;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Function;

/**
 * Shows the data sources of all analyses and how their collection is doing, and lets the
 * user act on them: pause and resume, reconnect, change the connection, add a further source
 * to an analysis, export the configuration for a stand-alone collector, remove.
 * <p>
 * The dialog does not block the application and keeps itself up to date, so it can stay open
 * next to the charts as the answer to "is my data still coming in?".
 */
public class DataSourcesDialog extends Dialog<Void> {

    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("dd.MM. HH:mm:ss").withZone(ZoneId.systemDefault());

    private static final Duration REFRESH_INTERVAL = Duration.seconds(3);

    private final DataCollectionService service;

    private final TableView<DataSourceInfo> table = new TableView<>();
    private final ListView<String> history = new ListView<>();
    private final Label message = new Label();
    private final Button pauseButton = new Button("Pause");
    private final Button reconnectButton = new Button("Reconnect");
    private final Button editButton = new Button("Edit Connection...");
    private final Button addButton = new Button("Add Source...");
    private final Button exportButton = new Button("Export Collector Config...");
    private final Button removeButton = new Button("Remove");

    /** One thread for everything that touches the service: reads never overlap, actions keep their order. */
    private final ExecutorService worker = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "data-sources-dialog");
        thread.setDaemon(true);
        return thread;
    });

    private final Timeline refresher = new Timeline(new KeyFrame(REFRESH_INTERVAL, event -> refresh()));

    private boolean refreshing;

    /**
     * The project whose sources are shown; {@code null} shows the sources of all projects.
     */
    private final String projectName;

    /**
     * Shows the data sources of all analyses.
     */
    public DataSourcesDialog(DataCollectionService service) {
        this(service, null);
    }

    /**
     * Shows the data sources of one project.
     *
     * @param projectName the project, or {@code null} for all projects
     */
    public DataSourcesDialog(DataCollectionService service, String projectName) {
        this.service = service;
        this.projectName = projectName;

        setTitle(projectName == null ? "Data Sources" : "Data Sources of " + projectName);
        setHeaderText(projectName == null
                ? "Where the data of your analyses comes from, and whether it is still coming in."
                : "Where the data of '" + projectName + "' comes from, and whether it is still coming in.");
        initModality(Modality.NONE);
        setResizable(true);
        getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        // the look of the other dialogs of the EKG: a blue header over a light content area
        getDialogPane().getStylesheets().add(
                DataSourcesDialog.class.getResource("NewAnalysisDialog.css").toExternalForm());
        getDialogPane().getStyleClass().add("ekg-dialog");
        getDialogPane().setContent(buildContent());
        getDialogPane().setPrefSize(980, 520);

        table.getSelectionModel().selectedItemProperty().addListener((o, ov, selected) -> selectionChanged(selected));
        selectionChanged(null);

        refresher.setCycleCount(Animation.INDEFINITE);
        setOnShown(event -> {
            refresh();
            refresher.play();
        });
        setOnHidden(event -> {
            refresher.stop();
            worker.shutdownNow();
        });
    }

    //=================================================================================================================
    // layout
    //=================================================================================================================

    private VBox buildContent() {
        table.setId("dataSources");
        table.setPlaceholder(new Label(projectName == null
                ? "No data sources yet. Use \"Analysis > New Analysis...\" to connect one."
                : "This project has no data source. Use \"Add Source...\" to connect one."));
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        if (projectName == null) {
            table.getColumns().add(column("Analysis", 140, DataSourceInfo::projectName));
        }
        table.getColumns().add(column("Source", 250, info -> info.sourceType() + "  " + info.endpoint()));
        TableColumn<DataSourceInfo, String> state = column("State", 120, info -> info.state().label());
        state.setCellFactory(column -> new StateCell());
        table.getColumns().add(state);
        table.getColumns().add(column("Last value", 120, info -> info.lastValue() == null ? "" : TIME.format(info.lastValue())));
        table.getColumns().add(column("Series", 60, info -> info.seriesCount() == 0 ? "" : String.valueOf(info.seriesCount())));
        table.getColumns().add(column("Details", 240, info -> info.detail() == null ? "" : info.detail()));
        VBox.setVgrow(table, Priority.ALWAYS);

        pauseButton.setId("pauseSource");
        pauseButton.setOnAction(e -> selected().ifPresent(source -> {
            if (source.state() == State.PAUSED) {
                act("Resuming ...", () -> service.resume(source.id()));
            } else {
                act("Pausing ...", () -> service.pause(source.id()));
            }
        }));
        reconnectButton.setOnAction(e -> selected().ifPresent(source ->
                act("Reconnecting ...", () -> service.reconnect(source.id()))));
        editButton.setOnAction(e -> selected().ifPresent(this::editConnection));
        addButton.setOnAction(e -> {
            // in the view of one project the source goes to that project, selected source or not
            String target = projectName != null ? projectName : selected().map(DataSourceInfo::projectName).orElse(null);
            if (target != null) {
                NewAnalysisDialog dialog = new NewAnalysisDialog(service, target);
                dialog.initOwner(getDialogPane().getScene().getWindow());
                dialog.showAndWait();
                refresh();
            }
        });
        exportButton.setOnAction(e -> selected().ifPresent(this::exportConfig));
        removeButton.setId("removeSource");
        removeButton.setOnAction(e -> selected().ifPresent(this::remove));

        HBox actions = new HBox(8, pauseButton, reconnectButton, editButton, addButton, exportButton, removeButton);
        actions.setAlignment(Pos.CENTER_LEFT);

        history.setId("sourceHistory");
        history.setPrefHeight(120);
        history.setMinHeight(110);
        table.setMinHeight(140);
        history.setPlaceholder(new Label("Select a source to see what happened to it."));

        message.setId("sourcesMessage");
        message.setWrapText(true);

        VBox content = new VBox(10, table, actions, new Label("History of the selected source"), history, message);
        content.setPadding(new Insets(12));
        return content;
    }

    private static TableColumn<DataSourceInfo, String> column(String title, double width, Function<DataSourceInfo, String> value) {
        TableColumn<DataSourceInfo, String> column = new TableColumn<>(title);
        column.setPrefWidth(width);
        column.setSortable(false);
        column.setCellValueFactory(cell -> new ReadOnlyStringWrapper(value.apply(cell.getValue())));
        return column;
    }

    /** Shows the state in the colour of its meaning: fine, attention, problem. */
    private static final class StateCell extends TableCell<DataSourceInfo, String> {
        @Override
        protected void updateItem(String label, boolean empty) {
            super.updateItem(label, empty);
            if (empty || label == null || getTableRow() == null || getTableRow().getItem() == null) {
                setText(null);
                setStyle("");
                return;
            }
            setText(label);
            State state = getTableRow().getItem().state();
            setStyle(switch (state) {
                case RUNNING -> "-fx-text-fill: #1a7f37; -fx-font-weight: bold;";
                case ERROR -> "-fx-text-fill: #b42318; -fx-font-weight: bold;";
                case PAUSED, STOPPED -> "-fx-text-fill: #6e7781;";
                default -> "-fx-text-fill: #9a6700;";
            });
        }
    }

    //=================================================================================================================
    // behaviour
    //=================================================================================================================

    /** Reads the sources and the history of the selected one off the JavaFX thread and shows them. */
    private void refresh() {
        if (refreshing || worker.isShutdown()) {
            return;
        }
        refreshing = true;
        String selectedId = selected().map(DataSourceInfo::id).orElse(null);
        worker.execute(() -> {
            try {
                List<DataSourceInfo> sources = projectName == null ? service.sources() : service.sourcesOf(projectName);
                List<String> events = new ArrayList<>();
                if (selectedId != null) {
                    for (CollectionSourceEvent event : service.history(selectedId, 30)) {
                        events.add(describe(event));
                    }
                }
                Platform.runLater(() -> show(sources, selectedId, events));
            } catch (RuntimeException e) {
                Platform.runLater(() -> {
                    refreshing = false;
                    problem(e);
                });
            }
        });
    }

    private void show(List<DataSourceInfo> sources, String selectedId, List<String> events) {
        refreshing = false;
        table.getItems().setAll(sources);
        if (selectedId != null) {
            sources.stream().filter(source -> source.id().equals(selectedId)).findFirst()
                    .ifPresent(source -> table.getSelectionModel().select(source));
            history.getItems().setAll(events);
        } else if (!sources.isEmpty()) {
            table.getSelectionModel().selectFirst();
        }
    }

    private void selectionChanged(DataSourceInfo selected) {
        boolean none = selected == null;
        for (Button button : List.of(pauseButton, reconnectButton, editButton, addButton, exportButton, removeButton)) {
            button.setDisable(none);
        }
        if (projectName != null) {
            addButton.setDisable(false);
        }
        if (none) {
            history.getItems().clear();
            return;
        }
        pauseButton.setText(selected.state() == State.PAUSED ? "Resume" : "Pause");
        reconnectButton.setDisable(selected.state() == State.PAUSED);
        // only a source that is one address has a connection to edit; a list of hosts is added anew
        editButton.setDisable(!"prometheus".equals(selected.sourceType()));
    }

    /** Runs an action on a source in the background and shows what came of it. */
    private void act(String progress, Runnable action) {
        message.setStyle("");
        message.setText(progress);
        worker.execute(() -> {
            try {
                action.run();
                Platform.runLater(() -> {
                    message.setText("");
                    refresh();
                });
            } catch (RuntimeException e) {
                Platform.runLater(() -> {
                    problem(e);
                    refresh();
                });
            }
        });
    }

    private void problem(RuntimeException e) {
        message.setStyle("-fx-text-fill: #b42318;");
        message.setText(e.getMessage() != null ? e.getMessage() : e.toString());
    }

    private void editConnection(DataSourceInfo source) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.initOwner(getDialogPane().getScene().getWindow());
        dialog.setTitle("Edit Connection");
        dialog.setHeaderText("How " + source.projectName() + " reaches its " + source.sourceType() + " source.\n"
                + "What is collected stays as it is.");
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        TextField url = new TextField(source.endpoint());
        url.setPrefColumnCount(36);
        TextField username = new TextField();
        username.setPromptText("leave empty for a bearer token");
        PasswordField password = new PasswordField();
        password.setPromptText("leave empty to keep the stored one");
        CheckBox noAuthentication = new CheckBox("This server needs no authentication");
        username.disableProperty().bind(noAuthentication.selectedProperty());
        password.disableProperty().bind(noAuthentication.selectedProperty());

        GridPane grid = new GridPane();
        grid.setHgap(8);
        grid.setVgap(8);
        grid.setPadding(new Insets(12));
        grid.addRow(0, new Label("Address"), url);
        grid.addRow(1, new Label("User"), username);
        grid.addRow(2, new Label("Password or token"), password);
        grid.add(noAuthentication, 1, 3);
        dialog.getDialogPane().setContent(grid);

        if (dialog.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) {
            return;
        }
        String newUsername = noAuthentication.isSelected() ? null : username.getText();
        // null keeps the stored secret, blank removes it
        String newPassword = noAuthentication.isSelected() ? ""
                : password.getText() == null || password.getText().isEmpty() ? null : password.getText();
        String newUrl = url.getText();
        act("Reconnecting with the new connection ...",
                () -> service.updateConnection(source.id(), newUrl, newUsername, newPassword));
    }

    private void exportConfig(DataSourceInfo source) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Export Collector Configuration");
        chooser.setInitialFileName("collector-" + source.projectName().replaceAll("[^A-Za-z0-9._-]+", "_") + ".yaml");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Collector configuration", "*.yaml", "*.yml"));
        File file = chooser.showSaveDialog(getDialogPane().getScene().getWindow());
        if (file == null) {
            return;
        }
        worker.execute(() -> {
            try {
                Files.writeString(file.toPath(), service.exportCollectorConfig(source.id()), StandardCharsets.UTF_8);
                Platform.runLater(() -> {
                    message.setStyle("");
                    message.setText("Exported to " + file + ". It contains no credentials: a collector reads the "
                            + "password or token from the environment variable "
                            + CollectorConfigCompiler.SECRET_ENVIRONMENT_VARIABLE + ".");
                });
            } catch (IOException | RuntimeException e) {
                Platform.runLater(() -> problem(new IllegalStateException("Could not export: " + e.getMessage(), e)));
            }
        });
    }

    private void remove(DataSourceInfo source) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Stop collecting from " + source.endpoint() + " for '" + source.projectName() + "'?\n\n"
                        + "The source and its stored credentials are removed. The analysis and the data "
                        + "collected so far stay.",
                ButtonType.OK, ButtonType.CANCEL);
        confirm.initOwner(getDialogPane().getScene().getWindow());
        confirm.setHeaderText("Remove data source");
        Optional<ButtonType> answer = confirm.showAndWait();
        if (answer.isPresent() && answer.get() == ButtonType.OK) {
            act("Removing ...", () -> service.remove(source.id()));
        }
    }

    private Optional<DataSourceInfo> selected() {
        return Optional.ofNullable(table.getSelectionModel().getSelectedItem());
    }

    private static String describe(CollectionSourceEvent event) {
        Instant time = event.time();
        String state;
        try {
            state = State.valueOf(event.state()).label();
        } catch (IllegalArgumentException e) {
            state = event.state();
        }
        return (time == null ? "" : TIME.format(time) + "   ") + state
                + (event.message() == null || event.message().isBlank() ? "" : "   " + event.message());
    }
}

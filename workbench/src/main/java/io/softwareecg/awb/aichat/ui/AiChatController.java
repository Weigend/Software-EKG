package io.softwareecg.awb.aichat.ui;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.softwareecg.awb.aichat.agent.AgentCallback;
import io.softwareecg.awb.aichat.agent.AgentLoop;
import io.softwareecg.awb.aichat.ui.agent.JavaFxAgentTask;
import io.softwareecg.awb.aichat.agent.SystemPrompts;
import io.softwareecg.awb.aichat.adapter.llm.AiBackendConfig;
import io.softwareecg.awb.aichat.adapter.llm.OpenAiCompatibleClient;
import io.softwareecg.awb.aichat.tools.AiTool;
import io.softwareecg.awb.aichat.tools.ChartContextProvider;
import io.softwareecg.awb.aichat.tools.GetCurrentContextTool;
import io.softwareecg.awb.aichat.tools.ListMetricsTool;
import io.softwareecg.awb.aichat.tools.QueryTimeSeriesTool;
import io.softwareecg.awb.aichat.tools.SshCommandTool;
import io.softwareecg.awb.investigation.model.SeriesCombineMode;
import io.softwareecg.awb.investigation.model.SeriesSmoothingType;
import io.softwareecg.awb.queryengine.MetricDataAccessService;
import io.softwareecg.awb.investigation.model.ComputationSpec;
import io.softwareecg.awb.investigation.model.QuerySpec;
import io.softwareecg.awb.metricview.MetricViewController;
import io.softwareecg.wfx.lookup.api.Lookup;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.concurrent.CompletableFuture;

/**
 * Controller for the AI Chat sidebar panel.
 * Displays user/assistant messages with TextFlow-based Markdown rendering.
 * Tool calls and debug output go to a dedicated log file only.
 */
public class AiChatController implements Initializable {

    private static final Logger LOG = LoggerFactory.getLogger(AiChatController.class);
    private static final Logger CHAT_DEBUG = LoggerFactory.getLogger("aichat.debug");

    @FXML private ScrollPane chatScrollPane;
    @FXML private VBox messageContainer;
    @FXML private TextArea inputArea;
    @FXML private Button sendButton;
    @FXML private Button resetButton;
    @FXML private Label modelLabel;
    @FXML private Label statusLabel;

    private AgentLoop agentLoop;
    private OpenAiCompatibleClient client;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Auto-scroll to bottom when new messages are added
        messageContainer.heightProperty().addListener((obs, oldVal, newVal) ->
                chatScrollPane.setVvalue(1.0));
    }

    public void initAgent(AiBackendConfig config) {
        this.client = new OpenAiCompatibleClient(config);
        modelLabel.setText(config.getModel());

        ChartContextProvider contextProvider = this::buildCurrentContextJson;
        List<AiTool> tools = List.of(
                new GetCurrentContextTool(contextProvider),
                new ListMetricsTool(this::resolveMetricDataAccessService),
                new QueryTimeSeriesTool(this::resolveMetricDataAccessService, contextProvider, this::chartTimeZone),
                new SshCommandTool(this::showSshApprovalDialog)
        );
        this.agentLoop = new AgentLoop(client, tools, SystemPrompts.DEFAULT);
    }

    // ---- Chart context (unchanged) ----

    private MetricViewController resolveActiveChartController() {
        try {
            Object manager = Lookup.lookup(
                    Class.forName("io.softwareecg.awb.metricview.adapter.ChartPanelAdapter$ChartPanelManager"));
            if (manager == null) return null;
            var getLastActive = manager.getClass().getMethod("getLastActiveController");
            return (MetricViewController) getLastActive.invoke(manager);
        } catch (Exception e) {
            LOG.warn("Could not resolve active chart controller: {}", e.getMessage());
            return null;
        }
    }

    private MetricDataAccessService resolveMetricDataAccessService() {
        MetricViewController controller = resolveActiveChartController();
        return controller != null ? controller.getMetricDataAccessService() : null;
    }

    private String buildCurrentContextJson() {
        try {
            MetricViewController controller = resolveActiveChartController();
            if (controller == null) return null;
            QuerySpec params = controller.getCurrentFilterParams();
            if (params == null) return null;

            ObjectMapper mapper = new ObjectMapper();
            ObjectNode ctx = mapper.createObjectNode();
            ZoneId zone = controller.getTimeZone();
            ctx.put("time_zone", zone.getId());
            ctx.put("project", safe(params.getProjectName()));
            ctx.put("host", safe(params.getHostName()));
            ctx.put("hostgroup", safe(params.getHostGroupName()));
            ctx.put("measurement", safe(params.getMeasurementName()));
            ctx.put("metric", safe(params.getMetricName()));
            ctx.put("process", safe(params.getProcessName()));

            String exclude = params.getExcludeMetricName();
            if (exclude != null && !exclude.isEmpty()) ctx.put("exclude_regex", exclude);

            if (params.getStart() > 0) {
                ctx.put("start", params.getStart());
                ctx.put("start_iso", formatEpoch(params.getStart(), zone));
            }
            if (params.getEnd() > 0) {
                ctx.put("end", params.getEnd());
                ctx.put("end_iso", formatEpoch(params.getEnd(), zone));
            }
            if (params.isExpertMode()) ctx.put("expert_mode", true);
            if (params.isMultiMetricMode()) ctx.put("multi_metric_mode", true);

            ComputationSpec compute = controller.getCurrentComputeParams();
            if (compute != null) {
                if (compute.getSeriesCombineMode() != SeriesCombineMode.NONE)
                    ctx.put("combine_mode", compute.getSeriesCombineMode().name());
                if (compute.getSeriesSmoothingType() != SeriesSmoothingType.NONE) {
                    ctx.put("smoothing_type", compute.getSeriesSmoothingType().name());
                    ctx.put("smoothing_granularity", compute.getSeriesSmoothingGranularity().name());
                }
            }
            return mapper.writeValueAsString(ctx);
        } catch (Exception e) {
            LOG.warn("Could not read chart context: {}", e.getMessage());
            return null;
        }
    }

    private static final DateTimeFormatter DATE_AND_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private static String formatEpoch(long epochMs, ZoneId zone) {
        return DATE_AND_TIME.withZone(zone).format(Instant.ofEpochMilli(epochMs));
    }

    /** The time zone of the chart the assistant looks at; that of the computer if there is no chart. */
    private ZoneId chartTimeZone() {
        MetricViewController controller = resolveActiveChartController();
        return controller != null ? controller.getTimeZone() : ZoneId.systemDefault();
    }

    private static String safe(String value) {
        return value == null || value.isEmpty() ? "*" : value;
    }

    // ---- User interaction ----

    @FXML
    private void onSend() {
        String text = inputArea.getText();
        if (text == null || text.isBlank()) return;
        inputArea.clear();
        addUserMessage(text);
        runAgent(text);
    }

    @FXML
    private void onInputKeyPressed(KeyEvent event) {
        if (event.getCode() == KeyCode.ENTER && !event.isShiftDown()) {
            event.consume();
            onSend();
        }
    }

    @FXML
    private void onReset() {
        messageContainer.getChildren().clear();
        if (agentLoop != null) agentLoop.reset();
    }

    // ---- Agent execution ----

    private void runAgent(String userMessage) {
        setLoading(true);

        CHAT_DEBUG.info("[USER] {}", userMessage);

        AgentCallback callback = new AgentCallback() {
            @Override public void onAssistantMessage(String text) {
                CHAT_DEBUG.info("[ASSISTANT] {}", text);
                Platform.runLater(() -> addAssistantMessage(text));
            }
            @Override public void onToolCall(String toolName, String arguments) {
                CHAT_DEBUG.info("[CALL] {}({})", toolName, arguments);
                Platform.runLater(() -> showStatus("Tool: " + toolName + "..."));
            }
            @Override public void onToolResult(String toolName, String result) {
                CHAT_DEBUG.info("[RESULT] {} -> {}", toolName, result);
            }
            @Override public void onComplete() {
                Platform.runLater(() -> setLoading(false));
            }
            @Override public void onError(String message, Throwable cause) {
                LOG.error("Agent error: {}", message, cause);
                CHAT_DEBUG.error("[ERROR] {}", message, cause);
                Platform.runLater(() -> { addErrorMessage(message); setLoading(false); });
            }
        };

        JavaFxAgentTask task = new JavaFxAgentTask(agentLoop, userMessage, callback);
        task.setOnFailed(e -> Platform.runLater(() -> {
            addErrorMessage("Task failed: " + task.getException().getMessage());
            setLoading(false);
        }));

        Thread thread = new Thread(task, "ai-chat-agent");
        thread.setDaemon(true);
        thread.start();
    }

    // ---- Message rendering ----

    private void addUserMessage(String text) {
        Label label = new Label(text);
        label.setWrapText(true);
        label.getStyleClass().add("ai-chat-user-msg");
        // Limit width to 85% so the bubble stays right-aligned with room to spare
        label.maxWidthProperty().bind(messageContainer.widthProperty().multiply(0.85));

        HBox wrapper = new HBox(label);
        wrapper.setAlignment(Pos.CENTER_RIGHT);

        messageContainer.getChildren().add(wrapper);
    }

    private void addAssistantMessage(String text) {
        VBox mdContent = SimpleMarkdownConverter.toNodes(text);
        mdContent.getStyleClass().add("ai-chat-assistant-msg");
        mdContent.setMaxWidth(Double.MAX_VALUE);

        HBox wrapper = new HBox(mdContent);
        wrapper.setAlignment(Pos.CENTER_LEFT);
        wrapper.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(mdContent, Priority.ALWAYS);

        messageContainer.getChildren().add(wrapper);
        hideStatus();
    }

    private void addErrorMessage(String text) {
        Label label = new Label(text);
        label.setWrapText(true);
        label.setMaxWidth(Double.MAX_VALUE);
        label.getStyleClass().add("ai-chat-error-msg");
        messageContainer.getChildren().add(label);
    }

    // ---- SSH approval dialog ----

    /**
     * Shows a blocking confirmation dialog on the JavaFX thread.
     * Called from the agent's background thread — uses CompletableFuture to bridge threads.
     */
    private boolean showSshApprovalDialog(String toolName, String description) {
        CompletableFuture<Boolean> result = new CompletableFuture<>();
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("Execute SSH Command?");
            alert.setHeaderText("The AI wants to execute a command on a server:");
            alert.setContentText(description);
            alert.getDialogPane().setMinWidth(500);
            alert.getDialogPane().setMinHeight(200);
            Optional<ButtonType> response = alert.showAndWait();
            result.complete(response.isPresent() && response.get() == ButtonType.OK);
        });
        try {
            return result.get();
        } catch (Exception e) {
            LOG.warn("SSH approval dialog interrupted", e);
            return false;
        }
    }

    // ---- Loading / status ----

    private void setLoading(boolean loading) {
        sendButton.setDisable(loading);
        inputArea.setDisable(loading);
        if (loading) showStatus("Thinking...");
        else hideStatus();
    }

    private void showStatus(String text) {
        statusLabel.setText(text);
        statusLabel.setManaged(true);
        statusLabel.setVisible(true);
    }

    private void hideStatus() {
        statusLabel.setManaged(false);
        statusLabel.setVisible(false);
    }
}

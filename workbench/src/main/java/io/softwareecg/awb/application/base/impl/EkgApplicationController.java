//______________________________________________________________________________
//
//          ProjectConfiguration:    Software EKG
//______________________________________________________________________________
//
//         Author:      Weigend AM 2009 - 2014
//______________________________________________________________________________
//
// Notice: This piece of software was created, designed and implemented by
// experienced craftsmen and innovators in Munich, Germany.
// Changes should be done with respect to the original design.
//______________________________________________________________________________
package io.softwareecg.awb.application.base.impl;

import io.softwareecg.awb.application.base.ApplicationBootstrap;
import io.softwareecg.awb.application.api.events.BeforeRestoreDefaultLayoutEvent;
import io.softwareecg.awb.application.base.about.AboutDialogController;
import io.softwareecg.awb.common.ui.events.FinishEvent;
import io.softwareecg.awb.common.ui.view.AppWindowProvider;
import io.softwareecg.awb.sdk.core.events.AwbErrorEvent;
import io.softwareecg.awb.sdk.core.events.CancellableTaskEvent;
import io.softwareecg.wfx.platform.api.EventBus;
import io.softwareecg.awb.sdk.core.log.EkgLogger;
import io.softwareecg.wfx.lookup.api.Lookup;
import io.softwareecg.wfx.windowmanager.api.WindowManager;
import io.softwareecg.wfx.windowmanager.windows.DefaultApplicationWindow;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.TextArea;
import javafx.scene.image.Image;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.EventObject;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * This controller is very important!
 * It controls the action created by the user via the menu,
 * e.g. importing types, closing the project or showing the "About"-Dialog.
 * <p/>
 * Suppress warnings "unused" cause sonar can not detect usages directly from fxml.
 *
 */
@ApplicationBootstrap
// WFX shows the first ApplicationWindow it finds and registers one of its own, the plain
// DefaultApplicationWindow. Without a priority the order of the two is the order of the jars
// on the classpath, which the file system decides: on Windows the plain window came first,
// and every module that asked this controller for a menu found it never initialised.
@jakarta.annotation.Priority(1)
public class EkgApplicationController extends DefaultApplicationWindow
        implements io.softwareecg.awb.application.api.EkgApplicationController {

    private static final Logger LOGGER = EkgLogger.get();

    @FXML
    private BorderPane borderPane;

    @FXML
    private VBox topContainer;

    @FXML
    private Menu analysisMenu;

    @FXML
    private Menu importMenu;

    @FXML
    private Menu repositoryMenu;

    /**
     * The separator above "Exit"; the entries of the modules go above it.
     */
    @FXML
    private SeparatorMenuItem analysisMenuEnd;

    @FXML
    private Menu windowMenu;

    /**
     * The separator above "Restore Default Layout"; the entries of the modules go above it.
     */
    @FXML
    private SeparatorMenuItem windowMenuEnd;

    @FXML
    private Button cancelTaskButton;

    /**
     * The background tasks that are running and can be cancelled, by task, in the
     * order they started. Only touched on the JavaFX Application Thread.
     */
    private final Map<Object, CancellableTaskEvent> cancellableTasks = new LinkedHashMap<>();

    /** Those of {@link #cancellableTasks} that were asked to stop and have not ended yet. */
    private final Set<Object> tasksBeingCancelled = new HashSet<>();

    private String title;

    @Override
    public void init() throws IOException {
        FXMLLoader loader = Lookup.lookup(FXMLLoader.class);
        loader.setLocation(getClass().getResource("/io/softwareecg/awb/application/base/impl/Window.fxml"));
        loader.setController(this);
        Parent root = loader.load();
        getStage().setScene(new Scene(root));

        getStage().setMaximized(true);
        borderPane.setCenter(getWindowManager().getRootPane());

        useSystemMenuBarIfPossible();

        registerErrorHandler();
        registerCancellableTaskHandler();
    }

    /**
     * Exits the SoftwareEKG.
     */
    @FXML
    public void restoreDefault() {
        Lookup.lookup(EventBus.class).publish(new BeforeRestoreDefaultLayoutEvent(this));
        Lookup.lookup(WindowManager.class).restoreDefaultLayout();
    }

    /**
     * Setter for property centerArea.
     *
     * @param node Value to set for property centerArea.
     */
    @FXML
    @SuppressWarnings("unused")
    private void setCenterArea(Node node) {
        //borderPane.setCenter(node);
    }

    /**
     * Shows the about dialog
     */
    @FXML
    private void handleAboutMenuItemAction() {
        try {
            FXMLLoader fxmlLoader = Lookup.lookup(FXMLLoader.class);
            fxmlLoader.setLocation(AboutDialogController.class.getResource("AboutDialog.fxml"));
            handleActionHelper(fxmlLoader.load(), "About Software EKG");
        } catch (IOException e) {
            LOGGER.warn("Exception raised and with this i could not handle the about-menu button action", e);
        }
    }

    /**
     * Exits the SoftwareEKG.
     */
    @FXML
    public void handleExitMenuItemAction() {
        //close program ordinary
        Lookup.lookup(EventBus.class).publish(new FinishEvent(0, this));

        // Platform.exit();
    }

    @Override
    public void restoreTitle() {
        getStage().setTitle(title);
    }

    @Override
    public String getTitle() {
        return title;
    }

    @Override
    public void setTitle(String title) {
        if (getStage() != null) {
            getStage().setTitle(title);
        }
        this.title = title;
    }

    /**
     * This method does all necessary things to show a dialog.
     *
     * @param root  the root node of the scene graph
     * @param title the title for the dialog
     */
    private void handleActionHelper(Parent root, String title) {
        Scene scene = new Scene(root);
        Stage stage = new Stage();
        stage.setTitle(title);
        stage.setScene(scene);
        stage.getIcons().add(new Image(getClass().getResourceAsStream("/ekg-app-icon2.png")));
        stage.initModality(Modality.WINDOW_MODAL);
        stage.initOwner(getStage());
        stage.setResizable(false);
        stage.show();
    }

    @Override
    public void setStage(Stage stage) {
        super.setStage(stage);
        getStage().setTitle(getTitle());
        getStage().getIcons().add(new Image(getClass().getResourceAsStream("/ekg-app-icon2.png")));
    }

    public AppWindowProvider getDefaultWindow() {
        return new AppWindowProvider(borderPane.getScene().getWindow());
    }

    public void addTopNode(Node node) {
        topContainer.getChildren().add(node);
    }

    public void addAnalysisMenuItem(MenuItem menuItem) {
        // above the separator that sets "Exit" apart
        int insertionIndex = Math.max(0, analysisMenu.getItems().indexOf(analysisMenuEnd));
        analysisMenu.getItems().add(insertionIndex, menuItem);
    }

    public void addRepositoryMenuItem(MenuItem menuItem) {
        repositoryMenu.getItems().add(menuItem);
    }

    public void addImportMenuItem(MenuItem menuItem) {
        // at the end: below the importers, which keep their own sorted block
        importMenu.getItems().add(menuItem);
    }

    public void addWindowMenuItem(MenuItem menuItem) {
        int insertionIndex = Math.max(0, windowMenu.getItems().indexOf(windowMenuEnd));
        windowMenu.getItems().add(insertionIndex, menuItem);
    }

    private void registerCancellableTaskHandler() {
        EventBus<EventObject> bus = Lookup.lookup(EventBus.class);
        bus.subscribe(CancellableTaskEvent.class, event -> {
            Platform.runLater(() -> handleCancellableTask((CancellableTaskEvent) event));
            return true;
        });
    }

    private void handleCancellableTask(CancellableTaskEvent event) {
        if (event.isStarted()) {
            cancellableTasks.put(event.getSource(), event);
        } else {
            cancellableTasks.remove(event.getSource());
            tasksBeingCancelled.remove(event.getSource());
        }
        updateCancelTaskButton();
    }

    /**
     * Asks every running task to stop. A task may need a while to wind down; the
     * button stays, disabled, until the last of them has reported its end.
     */
    @FXML
    private void handleCancelTaskAction() {
        for (CancellableTaskEvent task : cancellableTasks.values()) {
            if (tasksBeingCancelled.add(task.getSource())) {
                task.cancel();
            }
        }
        updateCancelTaskButton();
    }

    private void updateCancelTaskButton() {
        // the task started last that nobody has cancelled yet names the button
        String label = null;
        for (CancellableTaskEvent task : cancellableTasks.values()) {
            if (!tasksBeingCancelled.contains(task.getSource())) {
                label = task.getCancelLabel();
            }
        }

        boolean show = !cancellableTasks.isEmpty();
        cancelTaskButton.setVisible(show);
        cancelTaskButton.setManaged(show);
        cancelTaskButton.setDisable(label == null);
        cancelTaskButton.setText(label != null ? label : "Wird abgebrochen ...");
    }

    private void registerErrorHandler() {
        EventBus<EventObject> bus = Lookup.lookup(EventBus.class);
        bus.subscribe(AwbErrorEvent.class, event -> {
            Platform.runLater(() -> handleError((AwbErrorEvent) event));
            return true;
        });
    }

    private void handleError(AwbErrorEvent event) {

        Object source = event.getSource();
        Exception cause = event.getCause();


        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Fehler!");

        if (event.hasCustomErrorMessage()) {
            alert.setHeaderText(event.getCustomErrorMessage());
        } else {
            alert.setHeaderText("In folgender Klasse ist ein Fehler aufgetreten: " + source.getClass().getSimpleName());
        }

        if (cause != null) {
            alert.setContentText(cause.getMessage());
        }

        alert.getDialogPane().setMinWidth(800);
        Stage stage = (Stage) alert.getDialogPane().getScene().getWindow();
        stage.getIcons().add(new Image(this.getClass().getResource("/error-icon.png").toString()));

        // Create expandable Exception.
        String exceptionText;
        if (cause != null) {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            cause.printStackTrace(pw);
            exceptionText = sw.toString();
        } else {
            exceptionText = "- kein Stacktrace vorhanden -";
        }

        Label label = new Label("The exception stacktrace was:");

        TextArea textArea = new TextArea(exceptionText);
        textArea.setEditable(false);
        textArea.setWrapText(true);
        textArea.setMaxWidth(Double.MAX_VALUE);
        textArea.setMaxHeight(Double.MAX_VALUE);
        GridPane.setVgrow(textArea, Priority.ALWAYS);
        GridPane.setHgrow(textArea, Priority.ALWAYS);

        GridPane expContent = new GridPane();
        expContent.setMaxWidth(Double.MAX_VALUE);
        expContent.add(label, 0, 0);
        expContent.add(textArea, 0, 1);

        // Set expandable Exception into the dialog pane.
        alert.getDialogPane().setExpandableContent(expContent);

        alert.showAndWait();
    }

}

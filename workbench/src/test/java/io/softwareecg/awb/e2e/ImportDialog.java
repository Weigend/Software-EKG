package io.softwareecg.awb.e2e;

import io.softwareecg.awb.sdk.core.events.AwbErrorEvent;
import io.softwareecg.awb.sdk.core.events.CancellableTaskEvent;
import io.softwareecg.wfx.lookup.api.Lookup;
import io.softwareecg.wfx.platform.api.EventBus;
import io.softwareecg.wfx.platform.api.EventBusListener;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.nio.file.Path;
import java.time.Duration;
import java.util.EventObject;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * The dialog of an importer, filled in and confirmed the way a user does it.
 */
public final class ImportDialog {

    private static final Duration SHORT = Duration.ofSeconds(30);

    private final Workbench workbench;
    private final Stage stage;

    ImportDialog(Workbench workbench, Stage stage) {
        this.workbench = workbench;
        this.stage = stage;
    }

    public String title() {
        return workbench.onFx(stage::getTitle);
    }

    /** Names the new project the import goes into. */
    public ImportDialog projectName(String name) {
        type("projectName", name);
        return this;
    }

    /** Names the file or directory to import: the field beside <b>Browse...</b>. */
    public ImportDialog files(Path fileOrDirectory) {
        type("txtFilePath", fileOrDirectory.toAbsolutePath().toString());
        return this;
    }

    /** Names the server to import from. The field opens once the project is named. */
    public ImportDialog serverUrl(String url) {
        TextField field = Workbench.control(stage, "txtRepositoryURL", TextField.class);
        workbench.awaitTrue("the address of the server to be asked for", SHORT, () -> workbench.onFx(() -> !field.isDisabled()));
        type("txtRepositoryURL", url);
        return this;
    }

    /** Chooses the range of time to import, for example "1d". */
    public ImportDialog importRange(String range) {
        workbench.onFx(() -> {
            @SuppressWarnings("unchecked")
            ComboBox<String> ranges = Workbench.control(stage, "cmbImportRange", ComboBox.class);
            if (!ranges.getItems().contains(range)) {
                throw new AssertionError("The dialog offers no range '" + range + "', but " + ranges.getItems());
            }
            ranges.setValue(range);
            return null;
        });
        return this;
    }

    /** Sets or clears an option of the dialog, named by its text. */
    public ImportDialog option(String text, boolean selected) {
        workbench.onFx(() -> {
            CheckBox option = stage.getScene().getRoot().lookupAll(".check-box").stream()
                    .filter(CheckBox.class::isInstance).map(CheckBox.class::cast)
                    .filter(box -> text.equals(box.getText()))
                    .findFirst()
                    .orElseThrow(() -> new AssertionError("The dialog '" + stage.getTitle() + "' has no option '" + text + "'"));
            option.setSelected(selected);
            return null;
        });
        return this;
    }

    /** @return whether <b>Import Data</b> can be pressed */
    public boolean canImport() {
        return workbench.onFx(() -> !importButton().isDisabled());
    }

    /** Takes a picture of the dialog as it stands. */
    public Path screenshot(String name) {
        return workbench.screenshot(stage, name);
    }

    /** Closes the dialog without importing. */
    public void cancel() {
        workbench.onFx(() -> {
            Workbench.control(stage, "cancel", Button.class).fire();
            return null;
        });
        workbench.awaitTrue("the dialog to close", SHORT, () -> workbench.onFx(() -> !stage.isShowing()));
    }

    /**
     * Presses <b>Import Data</b> and waits until the import has ended.
     *
     * @param timeout how long the import may take
     * @throws AssertionError if the button cannot be pressed, or the import ends with an error:
     *                        the error the Workbench would show in its error dialog
     */
    public void importData(Duration timeout) {
        workbench.awaitTrue("'Import Data' to be enabled in '" + title() + "'", SHORT, this::canImport);

        AtomicBoolean finished = new AtomicBoolean();
        AtomicReference<AwbErrorEvent> failure = new AtomicReference<>();
        EventBus<EventObject> bus = eventBus();
        EventBusListener<CancellableTaskEvent> onTask = event -> {
            if (!event.isStarted()) {
                finished.set(true);
            }
            return true;
        };
        EventBusListener<AwbErrorEvent> onError = event -> {
            failure.compareAndSet(null, event);
            return true;
        };
        bus.subscribe(CancellableTaskEvent.class, onTask);
        bus.subscribe(AwbErrorEvent.class, onError);
        try {
            workbench.onFx(() -> {
                importButton().fire();
                return null;
            });
            workbench.awaitTrue("the import of '" + title() + "' to end", timeout,
                    () -> finished.get() || failure.get() != null);
        } finally {
            bus.unsubscribe(CancellableTaskEvent.class, onTask);
            bus.unsubscribe(AwbErrorEvent.class, onError);
        }
        // what the import announces at its end runs on the thread of the user interface
        workbench.onFx(() -> null);
        closeErrorDialogs();
        if (failure.get() != null) {
            AwbErrorEvent error = failure.get();
            String message = error.hasCustomErrorMessage() ? error.getCustomErrorMessage() : String.valueOf(error.getCause());
            throw new AssertionError("The import of '" + title() + "' failed: " + message, error.getCause());
        }
    }

    private void closeErrorDialogs() {
        Workbench.pause(Duration.ofMillis(300));
        workbench.onFx(() -> {
            javafx.stage.Window.getWindows().stream()
                    .filter(window -> window != workbench.mainStage() && window.isShowing())
                    .filter(Stage.class::isInstance).map(Stage.class::cast)
                    .toList()
                    .forEach(Stage::close);
            return null;
        });
    }

    private Button importButton() {
        return Workbench.control(stage, "ok", Button.class);
    }

    private void type(String id, String text) {
        workbench.onFx(() -> {
            TextField field = Workbench.control(stage, id, TextField.class);
            field.requestFocus();
            field.setText(text);
            return null;
        });
        // leaving the field is what makes some dialogs look at what was entered
        workbench.onFx(() -> {
            Optional.ofNullable(stage.getScene().getRoot().lookup("#cancel")).ifPresent(Node::requestFocus);
            return null;
        });
    }

    @SuppressWarnings("unchecked")
    private static EventBus<EventObject> eventBus() {
        return Lookup.lookup(EventBus.class);
    }
}

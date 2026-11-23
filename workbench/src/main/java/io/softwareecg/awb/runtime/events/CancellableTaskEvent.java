//______________________________________________________________________________
//
//                  Project:    Software EKG
//______________________________________________________________________________
package io.softwareecg.awb.runtime.events;

import java.util.EventObject;
import java.util.Objects;

/**
 * Announces a background task the user may cancel. It is published twice per
 * task: {@link #started} when the task begins and {@link #finished} when it
 * has ended, however it ended. In between, the application window offers a
 * button in its status bar that runs the task's cancel action.
 * <p>
 * The event's source is the task; the two events of one task carry the same
 * source object.
 */
public class CancellableTaskEvent extends EventObject {

    private final String cancelLabel;

    private final transient Runnable cancelAction;

    private CancellableTaskEvent(Object task, String cancelLabel, Runnable cancelAction) {
        super(task);
        this.cancelLabel = cancelLabel;
        this.cancelAction = cancelAction;
    }

    /**
     * The task has started and can be cancelled from now on.
     *
     * @param task         the task
     * @param cancelLabel  the text of the button that cancels it, for example "Import abbrechen"
     * @param cancelAction asks the task to stop; called on the JavaFX Application Thread.
     *                     It must return at once - the task may take a while to wind down
     *                     and says so by publishing {@link #finished} when it is done.
     */
    public static CancellableTaskEvent started(Object task, String cancelLabel, Runnable cancelAction) {
        return new CancellableTaskEvent(task, Objects.requireNonNull(cancelLabel), Objects.requireNonNull(cancelAction));
    }

    /**
     * The task has ended: completed, failed or cancelled.
     *
     * @param task the task that was announced with {@link #started}
     */
    public static CancellableTaskEvent finished(Object task) {
        return new CancellableTaskEvent(task, null, null);
    }

    /** {@code true} for the event that announces the start, {@code false} for the one that announces the end. */
    public boolean isStarted() {
        return cancelAction != null;
    }

    /** The text of the cancel button; {@code null} for a {@link #finished} event. */
    public String getCancelLabel() {
        return cancelLabel;
    }

    /** Ask the task to stop. Does nothing for a {@link #finished} event. */
    public void cancel() {
        if (cancelAction != null) {
            cancelAction.run();
        }
    }
}

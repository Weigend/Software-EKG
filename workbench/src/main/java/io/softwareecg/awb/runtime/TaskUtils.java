//______________________________________________________________________________
//
//                  Project:    Software EKG
//______________________________________________________________________________
//
//                   Author:    Weigend AM 2009 - 2017
//______________________________________________________________________________
//
// Notice: This piece of software was created, designed and implemented by
// experienced craftsmen and innovators in Munich, Germany.
// Changes should be done with respect to the original design.
//______________________________________________________________________________
package io.softwareecg.awb.runtime;

import javafx.concurrent.Task;
import javafx.concurrent.WorkerStateEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Some helper methods to easy start a task within background.
 *
 */
public final class TaskUtils {

    private static final Logger LOGGER = LoggerFactory.getLogger(TaskUtils.class);

    private TaskUtils() {
    }

    /**
     * Start a background task.
     *
     * @param taskName The task name.
     * @param task     the task to execute.
     * @return The javafx task.
     */
    public static Task<Void> runAsTask(String taskName, Runnable task) {
        return runAsTask(taskName, task, (s) -> {
            // no success handler
        }, e -> {
            // no failed handler
        });
    }

    /**
     * Start a background task.
     *
     * @param taskName The task name.
     * @param task     the task to execute.
     * @param success  the success consumer
     * @return The javafx task.
     */
    public static Task<Void> runAsTask(String taskName, Runnable task, Consumer<WorkerStateEvent> success) {
        return runAsTask(taskName, task, success, e -> {
            // no failed handler
        });
    }

    /**
     * Start a background task.
     *
     * @param taskName The task name.
     * @param task     the task to execute.
     * @param success  the success consumer
     * @return The javafx task.
     */
    public static <T> Task<T> runAsTask(String taskName, Supplier<T> task, BiConsumer<T, WorkerStateEvent> success) {
        return runAsTask(taskName, task, success, e -> {
            // no failed handler
        });
    }

    /**
     * Start a background task.
     *
     * @param taskName         The task name.
     * @param task             the task to execute.
     * @param success          the success consumer
     * @param failedOrCanceled the consumer for failed or canceled the task.
     * @return The javafx task.
     */
    public static Task<Void> runAsTask(String taskName, Runnable task, Consumer<WorkerStateEvent> success, Consumer<WorkerStateEvent> failedOrCanceled) {
        return runAsTask(taskName, () -> {
                    task.run();
                    return null;
                }, (e, t) -> success.accept(t), failedOrCanceled
        );
    }

    /**
     * Start a background task.
     *
     * @param taskName         The task name.
     * @param task             the task to execute.
     * @param success          the success consumer
     * @param failedOrCanceled the consumer for failed or canceled the task.
     * @return The javafx task.
     */
    public static <T> Task<T> runAsTask(String taskName, Supplier<T> task, BiConsumer<T, WorkerStateEvent> success, Consumer<WorkerStateEvent> failedOrCanceled) {
        Task<T> executor = new Task<T>() {
            @Override
            protected T call() {
                try {
                    LOGGER.info("Task {} started", taskName);
                    return task.get();
                } finally {
                    LOGGER.info("Task {} finished", taskName);
                }
            }
        };
        executor.setOnFailed(failedOrCanceled::accept);
        executor.setOnCancelled(failedOrCanceled::accept);
        executor.setOnSucceeded(e -> success.accept(executor.getValue(), e));
        Thread thread = new Thread(executor);
        thread.setDaemon(true);
        thread.setName(taskName);
        thread.start();
        return executor;
    }
}

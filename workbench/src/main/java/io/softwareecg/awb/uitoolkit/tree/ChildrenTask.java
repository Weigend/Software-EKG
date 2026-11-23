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
package io.softwareecg.awb.uitoolkit.tree;

import io.softwareecg.awb.uitoolkit.tree.api.AbstractItem;
import io.softwareecg.awb.uitoolkit.tree.api.ProgressNotifier;
import io.softwareecg.awb.runtime.events.AwbErrorEvent;
import io.softwareecg.wfx.platform.api.EventBus;
import io.softwareecg.wfx.platform.api.events.ProgressEvent;
import io.softwareecg.wfx.lookup.api.Lookup;
import javafx.concurrent.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.EventObject;
import java.util.List;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

/**
 * A task to build child items for a given parent item.
 *
 * @param <T> The type of the parent item.
 */
class ChildrenTask<T extends AbstractItem> extends Task<List<AbstractItem>> implements ProgressNotifier {
    private static final Logger LOGGER = LoggerFactory.getLogger(ChildrenTask.class);
    private final T parentItem;
    private final ItemBuilderRegistryImpl registry;
    private final EventBus<EventObject> eventBus = Lookup.lookup(EventBus.class);

    /**
     * @param parentItem the parent item.
     * @param registry   The registry instance created this task.
     */
    public ChildrenTask(T parentItem, ItemBuilderRegistryImpl registry) {
        this.parentItem = parentItem;
        this.registry = registry;
    }

    @Override
    @SuppressWarnings("unchecked")
    protected List<AbstractItem> call() {
        try {
            return registry.getHandlerFor(parentItem.getClass())
                    .stream()
                    .flatMap(f -> ((BiFunction<T, ProgressNotifier, List<? extends AbstractItem>>) f).apply(parentItem, this).stream())
                    .sorted()
                    .collect(Collectors.toList());
        } catch (RuntimeException e) {
            EventBus<EventObject> bus = Lookup.lookup(EventBus.class);
            bus.publish(new AwbErrorEvent(parentItem, e));
            throw e;
        }
    }

    /**
     * A task is cancelled when the node is loaded anew while it is still loading. Its thread is
     * not interrupted but left to finish; what it found is not used. Interrupting it would hit
     * the database or the repository in the middle of a read: an access to the metadata that
     * was just opening the database failed with "Sleep interrupted" and was logged as an error.
     */
    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        return super.cancel(false);
    }

    @Override
    protected void failed() {
        super.failed();
        updateProgress("Can not load child items of explorerView. Cause: " + getException().getMessage(), 0, 0);
        LOGGER.info("Can not load child items of '{}': {}", parentItem.getValue(), getException().getMessage());
        LOGGER.debug("Detailed failure cause:", getException());
    }

    @Override
    @SuppressWarnings("unchecked")
    public void updateMessage(String message) {
        super.updateMessage(message);
        eventBus.publish(new ProgressEvent(message, 0, this));
    }

    @Override
    @SuppressWarnings("unchecked")
    public void updateProgress(double workDone, double max) {
        super.updateProgress(workDone, max);
        double progress = workDone / max;
        if (!Objects.equals(progress, 1.0)) {
            eventBus.publish(new ProgressEvent("Loading sub entries", progress, this));
        } else {
            eventBus.publish(new ProgressEvent("", progress, this));
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public void updateProgress(String message, double workDone, double max) {
        super.updateProgress(workDone, max);
        super.updateMessage(message);
        eventBus.publish(new ProgressEvent(message, workDone / max, this));
    }
}

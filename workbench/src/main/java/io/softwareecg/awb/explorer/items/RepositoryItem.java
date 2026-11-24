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
package io.softwareecg.awb.explorer.items;


import io.softwareecg.awb.runtime.di.BeanProvider;
import io.softwareecg.awb.repository.api.Repository;
import io.softwareecg.awb.repository.api.RepositoryNotAvailableException;
import io.softwareecg.awb.repository.api.RepositoryService;
import io.softwareecg.awb.repository.events.EditRepositoryRequest;
import io.softwareecg.awb.repository.api.Embedded;
import io.softwareecg.awb.runtime.events.ExplorerUpdateEvent;
import io.softwareecg.awb.repository.api.RepositoryException;
import io.softwareecg.wfx.platform.api.EventBus;
import io.softwareecg.wfx.platform.api.EventSubscriber;
import io.softwareecg.wfx.platform.core.eventbus.AnnotationProcessor;
import io.softwareecg.wfx.lookup.api.Lookup;
import io.avaje.inject.External;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.control.TreeItem;
import javafx.scene.effect.ColorAdjust;

import java.text.NumberFormat;
import java.util.EventObject;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The item that shows types within the explorerView tree.
 *
 */
public class RepositoryItem extends RepositoryBaseItem {

    private static final Logger LOGGER = LoggerFactory.getLogger(RepositoryItem.class);

    /**
     * The icon of the node in the explorerView tree
     */
    private static final String NODE_ICON = "/icons/repository-icon.svg";

    /**
     * Interval in milliseconds between periodic connectivity checks for remote repositories.
     */
    private static final long PING_INTERVAL_MS = 30_000;

    private static final EventBus<EventObject> eventBus = Lookup.lookup(EventBus.class);

    private final NumberFormat formatter = NumberFormat.getInstance(Locale.GERMAN);

    private final String repositoryName;

    private final Node icon;

    private volatile boolean connected = true;

    /**
     * The periodic connectivity check of a remote repository; {@code null} for the embedded one.
     */
    private Timer pingTimer;

    /**
     * Set once this node has left the explorer tree. A disposed item neither pings
     * nor queries its repository anymore.
     */
    private volatile boolean disposed;


    /**
     * Constructs a types
     *
     * @param name         - the name of the types
     * @param canBeRemoved - flag to indicate if the types could be removed
     * @param repository   the containing types.
     */
    public RepositoryItem(String name, boolean canBeRemoved, Repository repository) {
        super(name, repository);

        this.icon = createIcon(NODE_ICON);
        super.setGraphic(icon);
        setId(repository.getId());

        this.repositoryName = name;

        if (canBeRemoved) {
            addContextMenuEntry("Edit Repository", e ->
                    eventBus.publish(new EditRepositoryRequest(getRepository(), getGraphic().getScene().getWindow())));

            // The embedded repository is not a stored configuration and cannot be removed.
            if (!repository.isEmbedded()) {
                RemoveRepositoryHandler removeRepositoryHandler = Lookup.lookup(RemoveRepositoryHandler.class);
                addContextMenuEntry("Remove from Explorer", e -> removeRepositoryHandler.remove(this));
            }
        }

        addContextMenuEntry("Reload Contents", event -> {
                getService().restart();
                updateValueCount(null);
            }
        );

        if (!repository.isEmbedded()) {
            addContextMenuEntry("Connect", event ->
                TaskUtils.runAsTask("ping-" + repositoryName,
                    () -> getRepository().isReachable(),
                    (reachable, e) -> {
                        if (reachable) {
                            setConnected(true);
                            updateValueCount(null);
                        } else {
                            setConnected(false);
                        }
                    },
                    e -> setConnected(false))
            );
        }

        expandedProperty().addListener((obs, wasExpanded, isExpanded) -> {
            if (isExpanded && !connected) {
                Platform.runLater(() -> setExpanded(false));
            }
        });

        BeanProvider.injectFields(this);

        // Every tree refresh builds new RepositoryItems. Once a node has left the tree it
        // must stop pinging and listening, otherwise it keeps poking its (possibly
        // unreachable) backend forever. Checked one pulse later, so that a node which is
        // merely moved within the tree is left alone.
        parentProperty().addListener((obs, oldParent, newParent) -> {
            if (newParent == null) {
                Platform.runLater(() -> {
                    if (getParent() == null) {
                        dispose();
                    }
                });
            }
        });

        if (!repository.isEmbedded()) {
            // Start disconnected until first ping confirms reachability
            setConnected(false);
            startPeriodicPingCheck();
        }
    }

    /**
     * Starts a background daemon timer that periodically pings the remote repository
     * and updates the connected/disconnected state in the UI.
     */
    private void startPeriodicPingCheck() {
        pingTimer = new Timer("ping-timer-" + repositoryName, true);
        pingTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                if (disposed) {
                    return;
                }
                boolean reachable = false;
                try {
                    reachable = getRepository().isReachable();
                } catch (Exception e) {
                    // Im offline-Stadium liefert ping() bereits false; eine Exception
                    // hier ist eine "echte" Anomalie (z.B. CDI-Lookup-Fehler) und wird
                    // ohne Stacktrace zusammengefasst. Der State-Wechsel wird unten gemeldet.
                    if (connected) {
                        LOGGER.warn("Repository '{}' periodic ping raised: {}: {}",
                                repositoryName, e.getClass().getSimpleName(), e.getMessage());
                    }
                    reachable = false;
                }
                if (disposed) {
                    return;
                }
                if (reachable != connected) {
                    setConnected(reachable);
                    if (reachable) {
                        updateValueCount(null);
                    } else {
                        updateLabel(repositoryName + " (disconnected)");
                    }
                }
            }
        }, 0, PING_INTERVAL_MS);
    }

    /**
     * Detaches this item from everything that would make it contact its repository:
     * stops the periodic ping and unsubscribes from the event bus. Purely local, so it
     * also works while the repository is unreachable.
     */
    void dispose() {
        if (disposed) {
            return;
        }
        disposed = true;
        if (pingTimer != null) {
            pingTimer.cancel();
        }
        AnnotationProcessor.unprocess(this);
    }

    /**
     * Refreshes the value count shown in the label.
     *
     * @param event the triggering event, or {@code null} for an explicit refresh of this item
     */
    @EventSubscriber(eventClass = ExplorerUpdateEvent.class)
    public void updateValueCount(ExplorerUpdateEvent event) {
        if (disposed || !isTargetedAtThisRepository(event)) {
            return;
        }
        try {
            long amountValues = getRepository().countValues();
            LOGGER.info("Repository '{}' sumFieldValue succeeded with {}", repositoryName, amountValues);

            setConnected(true);
            updateLabel(repositoryName + " (" + formatter.format(amountValues) + ")");

        } catch (RepositoryNotAvailableException e) {
            LOGGER.warn("Repository '{}' sumFieldValue raised RepositoryNotAvailableException.", repositoryName, e);
            if (e.getNotAvailableCause() == RepositoryNotAvailableException.Cause.HOST_NOT_AVAILABLE) {
                setConnected(false);
                updateLabel(repositoryName + " (disconnected)");
            } else {
                // Table/index does not exist yet (e.g. fresh installation) — server is still reachable.
                updateReachableWithoutData();
            }
        } catch (Exception e) {
            LOGGER.warn("Repository '{}' sumFieldValue failed, retrying with ping fallback.", repositoryName, e);
            // Query failed but server may still be reachable.
            if (getRepository().isReachable()) {
                updateReachableWithoutData();
            } else {
                setConnected(false);
                updateLabel(repositoryName + " (disconnected)");
            }
        }
    }

    /**
     * Returns true when the given {@link ExplorerUpdateEvent} should trigger a
     * value-count refresh on this repository's tree node. The event-bus
     * fan-out is global, but the typical publishers carry a target hint:
     * <ul>
     *   <li>a repository-id {@code String} (post-import refresh) — match by id</li>
     *   <li>a {@link RepositoryBaseItem} (tree-driven refresh) — match by repository</li>
     *   <li>anything else (e.g. admin "refresh all") — act, unless this repository is
     *       currently disconnected</li>
     *   <li>no event at all — an explicit refresh of this item (reload, connect, ping)</li>
     * </ul>
     * Without this filter every connected and disconnected repository in the
     * H2 store would poke its backend on every event — for offline ClickHouse
     * or remote Solr instances that means failed pings or stray DDL attempts.
     */
    private boolean isTargetedAtThisRepository(ExplorerUpdateEvent event) {
        if (event == null) {
            return true;
        }
        Object source = event.getSource();
        String myId = getRepository().getId();
        if (source instanceof String repoId) {
            return myId != null && myId.equalsIgnoreCase(repoId);
        }
        if (source instanceof RepositoryBaseItem item) {
            return getRepository().equals(item.getRepository());
        }
        // An untargeted broadcast is delivered synchronously on the publisher's thread,
        // usually the JavaFX thread. A disconnected repository is left alone here: querying
        // it would block the UI until the connection attempt times out. The periodic ping
        // refreshes the count as soon as the repository is reachable again.
        return connected;
    }

    private void updateReachableWithoutData() {
        setConnected(true);
        updateLabel(repositoryName + " (0)");
    }

    private void updateLabel(String text) {
        Platform.runLater(() -> setValue(text));
    }

    /**
     * Updates the visual connected/disconnected state of this repository item.
     *
     * @param connected {@code true} if the repository is reachable, {@code false} otherwise
     */
    private void setConnected(boolean connected) {
        this.connected = connected;
        LOGGER.info("Repository '{}' setConnected({})", repositoryName, connected);
        Platform.runLater(() -> {
            if (connected) {
                icon.setEffect(null);
                icon.setOpacity(1.0);
            } else {
                ColorAdjust desaturate = new ColorAdjust();
                desaturate.setSaturation(-1);
                icon.setEffect(desaturate);
                icon.setOpacity(0.4);
                setExpanded(false);
                setValue(repositoryName + " (disconnected)");
            }
        });
    }

    public boolean isConnected() {
        return connected;
    }

    @Override
    protected int getOrderPriority() {
        return getRepository().isEmbedded() ? super.getOrderPriority() - 1 : super.getOrderPriority();
    }

    /**
     * Removes a repository from the EKG workbench.
     * <p>
     * Stateless on purpose: the handler is a singleton shared by all repository items,
     * so the item to remove is passed in with every call.
     */
    @Singleton
    public static class RemoveRepositoryHandler {

        private final Repository embeddedEkgRepository;

        @Inject
        public RemoveRepositoryHandler(@External @Embedded Repository embeddedEkgRepository) {
            this.embeddedEkgRepository = embeddedEkgRepository;
        }

        /**
         * Deletes the configuration of the item's repository together with the metadata of
         * its projects and takes the item out of the explorer tree. Works on the local
         * metadata store and the tree only - the repository itself is never contacted, so it
         * can be removed even when its server is unreachable.
         *
         * @param repositoryItem the tree node of the repository to remove
         */
        public void remove(RepositoryItem repositoryItem) {
            RepositoryService service = embeddedEkgRepository.getBoundedService(RepositoryService.class);
            service.removeRepositoryWithProjects(repositoryItem.getRepository());

            repositoryItem.dispose();

            TreeItem<String> parent = repositoryItem.getParent();
            if (parent != null) {
                parent.getChildren().remove(repositoryItem);
            }
        }
    }
}

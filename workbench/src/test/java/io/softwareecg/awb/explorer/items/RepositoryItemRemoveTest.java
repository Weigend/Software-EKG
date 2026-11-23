package io.softwareecg.awb.explorer.items;

import io.avaje.inject.BeanScope;
import io.softwareecg.awb.timeseries.port.TimeSeriesRepository;
import io.softwareecg.awb.repository.api.EkgRepository;
import io.softwareecg.awb.repository.api.RepositoryService;
import io.softwareecg.awb.runtime.events.ExplorerUpdateEvent;
import io.softwareecg.wfx.lookup.api.Lookup;
import io.softwareecg.wfx.lookup.api.LookupStrategy;
import io.softwareecg.wfx.lookup.api.TypeRef;
import io.softwareecg.wfx.platform.api.EventBus;
import io.softwareecg.wfx.platform.core.eventbus.AnnotationProcessor;
import io.softwareecg.wfx.platform.core.eventbus.SimpleEventBus;
import javafx.application.Platform;
import javafx.scene.control.TreeItem;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Removing a repository from the explorer must always work - in particular while
 * its server is unreachable - and must remove exactly the repository of the node
 * the user picked.
 */
public class RepositoryItemRemoveTest {

    private static final Map<Class<?>, Object> BEANS = new HashMap<>();

    private static EventBus<java.util.EventObject> eventBus;

    private RepositoryService repositoryService;

    private RepositoryItem.RemoveRepositoryHandler removeHandler;

    @BeforeClass
    @SuppressWarnings("unchecked")
    public static void startToolkitAndLookup() {
        try {
            Platform.startup(() -> { });
        } catch (IllegalStateException alreadyRunning) {
            // toolkit was started by another test in this JVM
        }
        Platform.setImplicitExit(false);

        eventBus = new SimpleEventBus();
        BEANS.put(EventBus.class, eventBus);
        BEANS.put(BeanScope.class, mock(BeanScope.class));

        // RepositoryItem resolves its collaborators through the static Lookup.
        Lookup.init(new LookupStrategy() {
            @Override
            public <T> T lookup(Class<T> clazz) {
                return clazz.cast(BEANS.get(clazz));
            }

            @Override
            public <T> T lookup(TypeRef<T> type) {
                return null;
            }

            @Override
            public <T> List<T> lookupAll(Class<T> clazz) {
                T bean = lookup(clazz);
                return bean == null ? List.of() : List.of(bean);
            }

            @Override
            public <T> List<T> lookupAll(TypeRef<T> type) {
                return List.of();
            }
        });
    }

    @AfterClass
    public static void shutdownLookup() {
        Lookup.shutdown();
    }

    @Before
    public void setUp() {
        repositoryService = mock(RepositoryService.class);
        EkgRepository embedded = mock(EkgRepository.class);
        when(embedded.getBoundedService(RepositoryService.class)).thenReturn(repositoryService);

        removeHandler = new RepositoryItem.RemoveRepositoryHandler(embedded);
        BEANS.put(RepositoryItem.RemoveRepositoryHandler.class, removeHandler);
    }

    @Test
    public void removesTheRepositoryOfThePickedNodeAndNotTheOneCreatedLast() throws Exception {
        TimeSeriesRepository pickedClient = offlineClient();
        TimeSeriesRepository lastClient = offlineClient();
        EkgRepository picked = remoteRepository("picked", pickedClient);
        EkgRepository last = remoteRepository("last", lastClient);

        TreeItem<String> root = new TreeItem<>("Workbench");
        RepositoryItem pickedItem = new RepositoryItem("picked", true, picked);
        RepositoryItem lastItem = new RepositoryItem("last", true, last);
        attach(root, pickedItem, lastItem);

        onFxThread(() -> removeHandler.remove(pickedItem));

        verify(repositoryService).removeRepositoryWithProjects(picked);
        verify(repositoryService, never()).removeRepositoryWithProjects(last);
        assertEquals(List.of(lastItem), root.getChildren());
    }

    @Test
    public void removingAnUnreachableRepositoryDoesNotContactIt() throws Exception {
        TimeSeriesRepository client = offlineClient();
        EkgRepository repository = remoteRepository("offline", client);

        TreeItem<String> root = new TreeItem<>("Workbench");
        RepositoryItem item = new RepositoryItem("offline", true, repository);
        attach(root, item);

        // the periodic connectivity check has seen the server as unreachable
        verify(client, timeout(5_000)).ping();
        clearInvocations(client);

        onFxThread(() -> removeHandler.remove(item));

        verify(repositoryService).removeRepositoryWithProjects(repository);
        assertTrue(root.getChildren().isEmpty());
        verifyNoInteractions(client);

        // A removed node no longer listens: later refreshes leave the repository alone.
        assertFalse(AnnotationProcessor.isProcessed(item));
        onFxThread(() -> eventBus.publish(new ExplorerUpdateEvent(this)));
        verifyNoInteractions(client);
    }

    @Test
    public void broadcastRefreshDoesNotQueryADisconnectedRepository() throws Exception {
        TimeSeriesRepository client = offlineClient();
        EkgRepository repository = remoteRepository("disconnected", client);

        TreeItem<String> root = new TreeItem<>("Workbench");
        RepositoryItem item = new RepositoryItem("disconnected", true, repository);
        attach(root, item);
        verify(client, timeout(5_000)).ping();
        clearInvocations(client);

        // delivered synchronously on the JavaFX thread - must not block on a dead server
        onFxThread(() -> eventBus.publish(new ExplorerUpdateEvent(this)));

        verifyNoInteractions(client);
        assertFalse(item.isConnected());
    }

    @Test
    public void nodeDroppedByATreeRefreshStopsListening() throws Exception {
        TimeSeriesRepository client = offlineClient();
        EkgRepository repository = remoteRepository("refreshed", client);

        TreeItem<String> root = new TreeItem<>("Workbench");
        RepositoryItem item = new RepositoryItem("refreshed", true, repository);
        attach(root, item);
        assertTrue(AnnotationProcessor.isProcessed(item));

        // a refresh replaces the children with newly built items
        onFxThread(() -> root.getChildren().clear());
        onFxThread(() -> { });

        assertFalse(AnnotationProcessor.isProcessed(item));
    }

    private static TimeSeriesRepository offlineClient() {
        TimeSeriesRepository client = mock(TimeSeriesRepository.class);
        when(client.ping()).thenReturn(false);
        return client;
    }

    private static EkgRepository remoteRepository(String id, TimeSeriesRepository client) {
        EkgRepository repository = mock(EkgRepository.class);
        when(repository.getId()).thenReturn(id);
        when(repository.getRepositoryName()).thenReturn(id);
        when(repository.isEmbedded()).thenReturn(false);
        when(repository.getRepositoryClient()).thenReturn(client);
        return repository;
    }

    private static void attach(TreeItem<String> root, RepositoryItem... items) throws Exception {
        onFxThread(() -> root.getChildren().addAll(items));
    }

    private static void onFxThread(Runnable action) throws Exception {
        CountDownLatch done = new CountDownLatch(1);
        Throwable[] failure = new Throwable[1];
        Platform.runLater(() -> {
            try {
                action.run();
            } catch (Throwable t) {
                failure[0] = t;
            } finally {
                done.countDown();
            }
        });
        assertTrue("JavaFX thread did not answer", done.await(10, TimeUnit.SECONDS));
        if (failure[0] != null) {
            throw new AssertionError("Action on the JavaFX thread failed", failure[0]);
        }
    }
}

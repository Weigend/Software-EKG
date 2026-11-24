package io.softwareecg.awb.explorer.items;

import io.avaje.inject.BeanScope;
import io.softwareecg.awb.explorer.tree.AbstractItem;
import io.softwareecg.awb.explorer.tree.ItemBuilderRegistry;
import io.softwareecg.awb.explorer.tree.ItemBuilderRegistryImpl;
import io.softwareecg.awb.explorer.tree.RootItem;
import io.softwareecg.awb.repository.connection.EkgRepository;
import io.softwareecg.wfx.lookup.api.Lookup;
import io.softwareecg.wfx.lookup.api.LookupStrategy;
import io.softwareecg.wfx.lookup.api.TypeRef;
import io.softwareecg.wfx.platform.api.EventBus;
import io.softwareecg.wfx.platform.core.eventbus.SimpleEventBus;
import javafx.application.Platform;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * When the explorer is loaded anew it gets new nodes. What was open before is open again.
 */
public class ExplorerReloadTest {

    private static final Map<Class<?>, Object> BEANS = new HashMap<>();

    private RootItem root;

    private TreeView<String> tree;

    private volatile boolean reachable = true;

    @BeforeClass
    public static void startToolkitAndLookup() {
        try {
            Platform.startup(() -> { });
        } catch (IllegalStateException alreadyRunning) {
            // toolkit was started by another test in this JVM
        }
        Platform.setImplicitExit(false);

        BEANS.put(EventBus.class, new SimpleEventBus());
        BEANS.put(BeanScope.class, mock(BeanScope.class));
        BEANS.put(RepositoryItem.RemoveRepositoryHandler.class, mock(RepositoryItem.RemoveRepositoryHandler.class));
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
    public void setUp() throws Exception {
        onFxThread(() -> {
            root = new RootItem();
            tree = new TreeView<>(root);
            return null;
        });
    }

    /** Nodes that have left the tree no longer ping. */
    @After
    public void emptyTheTree() throws Exception {
        onFxThread(() -> {
            root.getChildren().clear();
            return null;
        });
    }

    /** The number of values is behind the name of a repository once it is known. A new node has only the name. */
    @Test
    public void aRepositoryIsOpenAgainWhenItsLabelHasChanged() throws Exception {
        build(true);
        open("Local", "Explorer", "Metrics");
        onFxThread(() -> {
            root.getChildren().get(0).setValue("Local (330)");
            return null;
        });

        reload();

        awaitRows("Workbench", "Local", "Explorer", "Metrics", "os");
    }

    /** The second time the tree is loaded, the nodes of the first time have not got their children yet. */
    @Test
    public void whatWasOpenIsOpenAgainWhenTheTreeIsLoadedTwiceInARow() throws Exception {
        build(true);
        open("Local", "Explorer", "Metrics");

        reload();
        reload();

        awaitRows("Workbench", "Local", "Explorer", "Metrics", "os");
    }

    /** A repository with a server is taken for disconnected until the server has answered. */
    @Test
    public void aRepositoryWithAServerIsOpenAgainOnceItIsConnected() throws Exception {
        build(false);
        awaitConnected();
        open("Local", "Explorer", "Metrics");

        reload();

        awaitRows("Workbench", "Local", "Explorer", "Metrics", "os");
    }

    /** What was closed stays closed. */
    @Test
    public void whatWasClosedStaysClosed() throws Exception {
        build(true);
        open("Local", "Explorer", "Metrics");
        onFxThread(() -> {
            root.getChildren().get(0).setExpanded(false);
            return null;
        });

        reload();

        Thread.sleep(500);
        awaitRows("Workbench", "Local");
    }

    /** A repository that cannot be reached is not open, as before. */
    @Test
    public void aRepositoryThatCannotBeReachedStaysClosed() throws Exception {
        build(false);
        awaitConnected();
        open("Local", "Explorer", "Metrics");
        reachable = false;

        reload();

        Thread.sleep(1_000);
        awaitRows("Workbench", "Local (disconnected)");
    }

    // ---------------------------------------------------------------------------------------------

    /** A node below a repository. */
    private static final class Node extends RepositoryBaseItem {

        private Node(String name, EkgRepository repository) {
            super(name, repository);
        }
    }

    private void build(boolean embedded) throws Exception {
        ItemBuilderRegistryImpl registry = new ItemBuilderRegistryImpl();
        BEANS.put(ItemBuilderRegistry.class, registry);
        EkgRepository repository = repository(embedded);
        Map<String, String> below = Map.of("Local", "Explorer", "Explorer", "Metrics", "Metrics", "os");
        registry.registerItemBuilder(RootItem.class,
                (Function<RootItem, List<? extends AbstractItem>>) parent -> itemsOfTheRoot(embedded));
        registry.registerItemBuilder(RepositoryItem.class,
                (Function<RepositoryItem, List<? extends AbstractItem>>) parent -> List.of(new Node("Explorer", repository)));
        registry.registerItemBuilder(Node.class, (Function<Node, List<? extends AbstractItem>>) parent ->
                below.containsKey(parent.getValue()) ? List.of(new Node(below.get(parent.getValue()), repository)) : List.of());
        onFxThread(() -> {
            root.setExpanded(true);
            return null;
        });
    }

    private List<AbstractItem> itemsOfTheRoot(boolean embedded) {
        return List.of(new RepositoryItem("Local", true, repository(embedded)));
    }

    private EkgRepository repository(boolean embedded) {
        EkgRepository repository = mock(EkgRepository.class);
        when(repository.getId()).thenReturn("local");
        when(repository.getRepositoryName()).thenReturn("Local");
        when(repository.isEmbedded()).thenReturn(embedded);
        when(repository.isReachable()).thenAnswer(call -> reachable);
        return repository;
    }

    private void open(String... way) throws Exception {
        await("the way " + List.of(way), () -> onFxThread(() -> {
            TreeItem<String> item = root;
            for (String name : way) {
                item = item.getChildren().stream().filter(child -> child.getValue().startsWith(name)).findFirst().orElse(null);
                if (item == null) {
                    return false;
                }
                item.setExpanded(true);
            }
            return !item.getChildren().isEmpty();
        }));
    }

    private void awaitConnected() throws Exception {
        await("the repository to be connected", () -> onFxThread(() ->
                !root.getChildren().isEmpty() && ((RepositoryItem) (TreeItem<?>) root.getChildren().get(0)).isConnected()));
        Thread.sleep(200);
    }

    private void reload() throws Exception {
        TreeItem<String> before = onFxThread(() -> root.getChildren().get(0));
        onFxThread(() -> {
            root.getService().restart();
            return null;
        });
        await("new nodes", () -> onFxThread(() -> !root.getChildren().isEmpty() && root.getChildren().get(0) != before));
    }

    /** The rows of the tree. The number of values behind the name of a repository is of no interest. */
    private void awaitRows(String... expected) throws Exception {
        long end = System.nanoTime() + TimeUnit.SECONDS.toNanos(8);
        List<String> rows;
        do {
            Thread.sleep(100);
            rows = onFxThread(() -> {
                List<String> shown = new ArrayList<>();
                for (int row = 0; row < tree.getExpandedItemCount(); row++) {
                    shown.add(tree.getTreeItem(row).getValue().replaceAll(" \\(\\d+\\)", ""));
                }
                return shown;
            });
        } while (!rows.equals(List.of(expected)) && System.nanoTime() < end);
        assertEquals(List.of(expected), rows);
    }

    private static void await(String what, Callable<Boolean> condition) throws Exception {
        long end = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
        while (!condition.call()) {
            if (System.nanoTime() > end) {
                throw new AssertionError("Waited in vain for " + what);
            }
            Thread.sleep(50);
        }
    }

    private static <T> T onFxThread(Callable<T> action) throws Exception {
        FutureTask<T> task = new FutureTask<>(action);
        Platform.runLater(task);
        return task.get(10, TimeUnit.SECONDS);
    }
}

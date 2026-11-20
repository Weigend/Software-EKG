package io.softwareecg.awb.common.ui.explorer;

import io.softwareecg.awb.common.ui.explorer.api.AbstractItem;
import javafx.application.Platform;
import javafx.event.Event;
import javafx.scene.control.TreeCell;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.input.ContextMenuEvent;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

/**
 * A right-click must open the context menu of the item shown in the clicked row -
 * also on the first click after the row got another item. Otherwise an action such as
 * "Delete project and its data" runs on an item the user did not pick.
 */
public class TreeViewTreeCellFactoryTest {

    private TreeView<String> treeView;
    private TreeItem<String> root;
    private Item shop;
    private Item yahoo;
    private TreeCell<String> cell;

    @BeforeClass
    public static void startToolkit() {
        try {
            Platform.startup(() -> { });
        } catch (IllegalStateException alreadyRunning) {
            // toolkit was started by another test in this JVM
        }
        Platform.setImplicitExit(false);
    }

    @Test
    public void rightClickOpensTheMenuOfTheItemNowShownInTheRow() throws Exception {
        buildTreeWithCellOnRow(1);

        onFxThread(() -> {
            requestContextMenu(cell);
            assertNotNull(shop.getContextMenu());
            assertSame(shop.getContextMenu(), cell.getContextMenu());

            // "shop" is deleted, "Yahoo" moves up into its row
            root.getChildren().remove(shop);

            requestContextMenu(cell);
            assertSame(yahoo.getContextMenu(), cell.getContextMenu());
        });
    }

    @Test
    public void emptyRowHasNoMenu() throws Exception {
        buildTreeWithCellOnRow(1);

        onFxThread(() -> {
            requestContextMenu(cell);
            assertSame(shop.getContextMenu(), cell.getContextMenu());

            // the row behind the last item
            cell.updateIndex(3);

            requestContextMenu(cell);
            assertNull(cell.getContextMenu());
        });
    }

    /** Rows: 0 = root, 1 = shop, 2 = Yahoo. */
    private void buildTreeWithCellOnRow(int row) throws Exception {
        onFxThread(() -> {
            root = new TreeItem<>("Workbench");
            root.setExpanded(true);
            shop = new Item("shop");
            yahoo = new Item("Yahoo");
            root.getChildren().add(shop);
            root.getChildren().add(yahoo);
            treeView = new TreeView<>(root);
        });
        // a second round trip: the items create their context menus in a deferred step
        onFxThread(() -> {
            cell = new TreeViewTreeCellFactory<>(new KeyNavigationHandler<>(treeView)).call(treeView);
            cell.updateTreeView(treeView);
            cell.updateIndex(row);
        });
    }

    private static void requestContextMenu(TreeCell<String> cell) {
        Event.fireEvent(cell, new ContextMenuEvent(ContextMenuEvent.CONTEXT_MENU_REQUESTED, 0, 0, 0, 0, false, null));
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
        if (failure[0] instanceof AssertionError assertionError) {
            throw assertionError;
        }
        if (failure[0] != null) {
            throw new AssertionError("Action on the JavaFX thread failed", failure[0]);
        }
    }

    /** A tree item without children of its own. */
    private static final class Item extends AbstractItem<String> {
        Item(String value) {
            super(value);
        }
    }
}

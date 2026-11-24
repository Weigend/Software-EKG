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
package io.softwareecg.awb.explorer.metric.items.bookmarks;

import io.softwareecg.awb.uitoolkit.events.OpeningMode;
import io.softwareecg.awb.uitoolkit.tree.api.AbstractItem;
import io.softwareecg.awb.runtime.di.BeanProvider;
import io.softwareecg.awb.bookmark.api.MetricsBookmarkService;
import io.softwareecg.awb.bookmark.api.Bookmark;
import io.softwareecg.awb.explorer.metric.ContextAwareItem;
import io.softwareecg.awb.explorer.metric.FilterContext;
import io.softwareecg.awb.bookmark.ui.BookmarkEvent;
import io.softwareecg.wfx.lookup.api.Lookup;
import io.softwareecg.wfx.platform.api.EventBus;
import javafx.application.Platform;

import java.util.EventObject;
import javafx.event.EventHandler;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;

import static io.softwareecg.awb.explorer.items.TaskUtils.runAsTask;

/**
 * Show a  single bookmark within the explorerView tree.
 *
 */
@SuppressWarnings("CdiManagedBeanInconsistencyInspection")
public class BookmarkItem extends ContextAwareItem {

    private final Bookmark bookmark;

    /**
     * WFX EventBus is acquired via Lookup rather than via {@code @Inject}
     * because BookmarkItem is not a managed bean — it is instantiated by the
     * explorer framework and post-wired through {@code BeanProvider.injectFields}.
     */
    @SuppressWarnings("unchecked")
    private final EventBus<EventObject> bookmarkEvent =
            (EventBus<EventObject>) (EventBus<?>) Lookup.lookup(EventBus.class);

    /**
     * Creates a BookmarkItem with the given bookmark's name and the context.
     *
     * @param bookmark   the bookmark to show
     * @param context    the context
     */
    public BookmarkItem(Bookmark bookmark, FilterContext context) {
        super(bookmark.getName(), context);
        super.setValue(bookmark.getName());
        super.setGraphic(getIconProvider().getBookmarkItemIcon(this));
        BeanProvider.injectFields(this);
        this.bookmark = bookmark;

        addContextMenuEntry("Load in Chart View",
                e -> bookmarkEvent.publish(new BookmarkEvent(this, this.bookmark, getRepository(), OpeningMode.CLEAR_VIEW)));

        addContextMenuEntry("Delete Bookmark", e -> runAsTask("delete-bookmark", this::deleteBookmark));
    }

    public Bookmark getBookmark() {
        return bookmark;
    }

    @Override
    public boolean isLeaf() {
        return true;
    }

    @Override
    public EventHandler<MouseEvent> getDoubleClickHandler() {
        return e -> bookmarkEvent.publish(new BookmarkEvent(this, bookmark, this.getRepository()));
    }

    @Override
    public EventHandler<KeyEvent> getEnterPressedHandler() {
        return e -> bookmarkEvent.publish(new BookmarkEvent(this, bookmark, this.getRepository()));
    }

    private void deleteBookmark() {
        getFilterContext().bookmarks().deleteBookmark(bookmark);
        Platform.runLater(() -> ((AbstractItem) getParent()).getService().restart());
    }
}

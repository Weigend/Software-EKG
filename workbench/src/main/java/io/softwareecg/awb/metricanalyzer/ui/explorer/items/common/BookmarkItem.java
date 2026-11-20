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
package io.softwareecg.awb.metricanalyzer.ui.explorer.items.common;

import io.softwareecg.awb.common.ui.events.OpeningMode;
import io.softwareecg.awb.common.ui.explorer.api.AbstractItem;
import io.softwareecg.awb.commons.cdi.BeanProvider;
import io.softwareecg.awb.metricanalyzer.bl.bookmarks.MetricsBookmarkService;
import io.softwareecg.awb.metricanalyzer.bl.bookmarks.et.Bookmark;
import io.softwareecg.awb.metricanalyzer.ui.api.ContextAwareItem;
import io.softwareecg.awb.metricanalyzer.ui.api.FilterContext;
import io.softwareecg.awb.metricanalyzer.ui.bookmarks.BookmarkEvent;
import io.softwareecg.awb.repository.api.EkgRepository;
import io.softwareecg.wfx.lookup.api.Lookup;
import io.softwareecg.wfx.platform.api.EventBus;
import javafx.application.Platform;

import java.util.EventObject;
import javafx.event.EventHandler;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;

import static io.softwareecg.awb.sdk.core.utils.TaskUtils.runAsTask;

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
     * @param repository the containing types.
     */
    public BookmarkItem(Bookmark bookmark, FilterContext context, EkgRepository repository) {
        super(bookmark.getName(), context, repository);
        super.setValue(bookmark.getName());
        super.setGraphic(getIconProvider().getBookmarkItemIcon(this));
        BeanProvider.injectFields(this);
        this.bookmark = bookmark;

        addContextMenuEntry("Load in Chart View",
                e -> bookmarkEvent.publish(new BookmarkEvent(this, this.bookmark, repository, OpeningMode.CLEAR_VIEW)));

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
        getRepository().getBoundedService(MetricsBookmarkService.class).deleteBookmark(bookmark);
        Platform.runLater(() -> ((AbstractItem) getParent()).getService().restart());
    }
}

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

import io.softwareecg.awb.uitoolkit.tree.api.AbstractItem;
import io.softwareecg.awb.uitoolkit.view.AppWindowProvider;
import io.softwareecg.awb.runtime.di.BeanProvider;
import io.softwareecg.awb.bookmark.api.BookmarkDialogs;
import io.softwareecg.awb.bookmark.model.BookmarkGroup;
import io.softwareecg.awb.explorer.metric.ContextAwareItem;
import io.softwareecg.awb.explorer.metric.FilterContext;
import io.softwareecg.awb.runtime.events.ExplorerUpdateEvent;
import io.softwareecg.wfx.platform.api.EventBus;
import jakarta.inject.Inject;

import java.util.EventObject;

/**
 * A tree node item that represents a domain specific/topic specific group
 * of bookmarks that are defined by a EKG user to group a specific set of bookmarks.
 *
 */
@SuppressWarnings({"CdiManagedBeanInconsistencyInspection", "CdiInjectionPointsInspection"})
public class BookmarkGroupItem extends ContextAwareItem {

    private final BookmarkGroup bookmarkGroup;

    @Inject
    private AppWindowProvider appWindowProvider;

    @Inject
    private BookmarkDialogs dialogs;

    @Inject
    private EventBus<EventObject> eventBus;

    /**
     * Creates a new BookmarkGroupItem based on the given queryParams using 'Bookmarks' as name
     *
     * @param bookmarkGroup the {@link BookmarkGroup} instance this explorer node represents (and is bind to)
     * @param queryParams - the query context.
     */
    public BookmarkGroupItem(BookmarkGroup bookmarkGroup, FilterContext queryParams) {
        super(bookmarkGroup.getName(), queryParams);
        super.setGraphic(getIconProvider().getBookmarkGroupItemIcon(this));
        this.bookmarkGroup = bookmarkGroup;

        BeanProvider.injectFields(this);

        addContextMenuEntry("Delete Group", event -> deleteBookmarkGroup());
    }

    /**
     * Returns the unique id of the underlying bookmark group
     * that is represented by this tree item.
     *
     * @return the unique id of th bookmark group
     */
    public String getBookmarkGroupId() {
        return bookmarkGroup.getId();
    }

    private void deleteBookmarkGroup() {
        if (dialogs.deleteGroup(appWindowProvider.getAppWindow(), bookmarkGroup)) {
            AbstractItem<?> parent = (AbstractItem<?>) getParent();
            parent.getService().restart();
            eventBus.publish(new ExplorerUpdateEvent(parent));
        }
    }
}

package io.softwareecg.awb.explorer.metric.items.bookmarks;

import io.softwareecg.awb.uitoolkit.view.AppWindowProvider;
import io.softwareecg.awb.runtime.di.BeanProvider;
import io.softwareecg.awb.bookmark.MetricsBookmarkService;
import io.softwareecg.awb.explorer.metric.api.ContextAwareItem;
import io.softwareecg.awb.explorer.metric.api.FilterContext;
import io.softwareecg.awb.bookmark.ui.CreateBookmarkGroupDialog;
import io.softwareecg.awb.repository.api.EkgRepository;
import io.softwareecg.awb.runtime.events.ExplorerUpdateEvent;
import io.softwareecg.wfx.platform.api.EventBus;
import io.softwareecg.wfx.lookup.api.Lookup;
import jakarta.inject.Inject;

import java.util.EventObject;
import java.util.UUID;

/**
 * This is the root item (parent node) of all BookmarkItems / BookmarkGroupItems that belongs
 * to a specific repository. It is a section to group the child elements and doesn't define any
 * bookmark data itself.
 *
 * @since 26.02.2020
 */
@SuppressWarnings({"CdiManagedBeanInconsistencyInspection", "CdiInjectionPointsInspection"})
public class ReproBookmarksItem extends ContextAwareItem {

    @Inject
    private AppWindowProvider ownerWindowProvider;

    private final EventBus<EventObject> eventBus;

    /**
     * Creates a new ReproBookmarksItem item based on the given context and the project
     *
     * @param label      the name of the tree item that will used as displayed label in the view
     * @param context    bean that define filters that relating to series data or the view flavor and control the item rendering
     * @param repository the EKG repository the bookmark section item belongs to.
     */
    public ReproBookmarksItem(String label, FilterContext context, EkgRepository repository) {
        super(label, context, repository);
        super.setGraphic(getIconProvider().getReproBookmarksItemIcon(this));
        BeanProvider.injectFields(this);
        eventBus = Lookup.lookup(EventBus.class);

        addContextMenuEntry("Create Bookmark Group", event -> createBookmarkGroup());
    }

    @Override
    protected int getOrderPriority() {
        return 20;
    }

    private void createBookmarkGroup() {

        CreateBookmarkGroupDialog dialog = new CreateBookmarkGroupDialog(getRepository());
        dialog.initOwner(ownerWindowProvider.getAppWindow());

        dialog.showAndWait().ifPresent(bookmarkGroup -> {
            MetricsBookmarkService service = getRepository().getBoundedService(MetricsBookmarkService.class);
            bookmarkGroup.setId(UUID.randomUUID().toString());
            service.persistNewBookmarkGroup(bookmarkGroup);

            eventBus.publish(new ExplorerUpdateEvent(this, getRepository()));
        });
    }
}

package io.softwareecg.awb.explorer.metric.items.bookmarks;

import io.softwareecg.awb.uitoolkit.view.AppWindowProvider;
import io.softwareecg.awb.runtime.di.BeanProvider;
import io.softwareecg.awb.bookmark.MetricsBookmarkService;
import io.softwareecg.awb.explorer.metric.ContextAwareItem;
import io.softwareecg.awb.explorer.metric.FilterContext;
import io.softwareecg.awb.bookmark.ui.CreateBookmarkGroupDialog;
import io.softwareecg.awb.repository.api.EkgRepository;
import io.softwareecg.awb.runtime.events.ExplorerUpdateEvent;
import io.softwareecg.wfx.platform.api.EventBus;
import io.softwareecg.wfx.lookup.api.Lookup;
import jakarta.inject.Inject;

import java.util.EventObject;
import java.util.UUID;

/**
 * Root item for bookmark groups and bookmarks that belong to a specific project.
 * This is the project-level counterpart of {@link ReproBookmarksItem} (which holds global bookmarks).
 * Bookmark groups created under this item are automatically associated with the project.
 *
 */
@SuppressWarnings({"CdiManagedBeanInconsistencyInspection", "CdiInjectionPointsInspection"})
public class ProjectBookmarksItem extends ContextAwareItem {

    @Inject
    private AppWindowProvider ownerWindowProvider;

    private final EventBus<EventObject> eventBus;

    private final String projectName;

    /**
     * Creates a new ProjectBookmarksItem for the given project.
     *
     * @param label       the display label in the tree
     * @param context     the filter context (carries project info)
     * @param repository  the EKG repository
     * @param projectName the name of the project this bookmark section belongs to
     */
    public ProjectBookmarksItem(String label, FilterContext context, EkgRepository repository, String projectName) {
        super(label, context, repository);
        super.setGraphic(getIconProvider().getReproBookmarksItemIcon(this));
        BeanProvider.injectFields(this);
        this.eventBus = Lookup.lookup(EventBus.class);
        this.projectName = projectName;

        addContextMenuEntry("Create Bookmark Group", event -> createBookmarkGroup());
    }

    public String getProjectName() {
        return projectName;
    }

    @Override
    protected int getOrderPriority() {
        return 90;
    }

    private void createBookmarkGroup() {
        CreateBookmarkGroupDialog dialog = new CreateBookmarkGroupDialog(getRepository());
        dialog.initOwner(ownerWindowProvider.getAppWindow());

        dialog.showAndWait().ifPresent(bookmarkGroup -> {
            MetricsBookmarkService service = getRepository().getBoundedService(MetricsBookmarkService.class);
            bookmarkGroup.setId(UUID.randomUUID().toString());
            bookmarkGroup.setProjectName(projectName);
            service.persistNewBookmarkGroup(bookmarkGroup);

            eventBus.publish(new ExplorerUpdateEvent(this, getRepository()));
        });
    }
}

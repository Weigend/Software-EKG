package io.softwareecg.awb.metricanalyzer.ui.explorer.builders;

import io.softwareecg.awb.common.ui.explorer.api.ProgressNotifier;
import io.softwareecg.awb.explorer.ui.items.RepositoryBaseItem;
import io.softwareecg.awb.metricanalyzer.ui.api.FilterContext;
import io.softwareecg.awb.metricanalyzer.ui.explorer.items.bookmarks.ProjectBookmarksItem;

import java.util.List;

/**
 * TreeItem builder that produces ProjectBookmarksItem instances.
 * Creates a single bookmark section node per project that acts as parent for
 * project-specific bookmark groups and bookmarks.
 *
 */
public class ProjectBookmarksItemBuilder<T extends RepositoryBaseItem> extends AbstractItemBuilder<T> {

    private static final String ITEM_DESC = "Bookmarks";

    @Override
    protected List<ProjectBookmarksItem> getChildItemsFor(T item, FilterContext context, ProgressNotifier notifier) {
        String projectName = context.getQueryParameter().getProjectName();
        return List.of(new ProjectBookmarksItem(ITEM_DESC, context, item.getRepository(), projectName));
    }
}

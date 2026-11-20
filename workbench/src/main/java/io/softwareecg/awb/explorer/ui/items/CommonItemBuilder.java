package io.softwareecg.awb.explorer.ui.items;

import io.softwareecg.awb.common.ui.explorer.api.ProgressNotifier;
import io.softwareecg.awb.explorer.ui.items.common.ProjectItem;
import io.softwareecg.awb.explorer.ui.items.common.RepositoryBaseItem;
import io.softwareecg.awb.project.api.ProjectDataAccessService;
import io.softwareecg.awb.project.api.model.Project;
import io.softwareecg.awb.sdk.awbapi.project.ProjectFlavor;
import io.softwareecg.awb.sdk.awbapi.project.ProjectViewFlavor;

import java.util.List;

/**
 * ItemBuilder implementation that will use a factory for all TreeItems that belong to workbench
 * but are not a direct part of time-series. The type of items are:
 * <ul>
 *   <li>{@link ProjectItem}</li>
 * </ul>
 *
 * @since 24.06.2018
 */
public class CommonItemBuilder extends ItemBuilder {

    /**
     * Private constructor
     */
    private CommonItemBuilder() {
        // this class should only use in a static way
    }

    /**
     * Build the Project items of explorer for a given parent item.
     *
     * @param parent   the parent item.
     * @param notifier a notifier to show the progress
     * @return the built series items.
     */
    public static List<ProjectItem> buildProjectItems(RepositoryBaseItem parent, ProgressNotifier notifier) {

        ProjectDataAccessService projectService = parent.getRepository().getBoundedService(ProjectDataAccessService.class);

        if (projectService == null) {
            return List.of();
        }

        List<Project> projectList = projectService.listProjects();

        return getItems(
                projectList.stream(),
                project -> {
                    ProjectItem projectItem = new ProjectItem(project, parent.getRepository());

                    if (project.getProjectFlavor() == ProjectFlavor.CLASSIC) {
                        projectItem.setActiveViewFlavor(ProjectViewFlavor.PHYSICAL_VIEW);
                    } else {
                        projectItem.setActiveViewFlavor(ProjectViewFlavor.LOGICAL_VIEW);
                    }

                    return projectItem;
                },
                notifier,
                projectList.size(),
                "EKG Worbench ProjectConfiguration");
    }
}

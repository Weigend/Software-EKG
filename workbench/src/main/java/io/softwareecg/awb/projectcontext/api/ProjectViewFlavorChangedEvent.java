package io.softwareecg.awb.projectcontext.api;


/**
 * An specialized ProjectPropertyChangeEvent that will used to communicate changes
 * of the view flavor for a specific Project
 *
 * @since 07.07.2018
 */
public class ProjectViewFlavorChangedEvent extends ProjectPropertyChangeEvent {

    /**
     * Constructs a new {@code ProjectViewFlavorChangedEvent}.
     *
     * @param source         the source that as published the event
     * @param relatedProject the project the changed view flavor belongs to
     * @param oldFlavor      the view flavor that was selected before (maybe null)
     * @param newFlavor      the active view flavor for the specified project
     * @throws IllegalArgumentException if {@code source} is {@code null}
     */
    public ProjectViewFlavorChangedEvent(Object source, Project relatedProject, ProjectViewFlavor oldFlavor,
                                         ProjectViewFlavor newFlavor) {

        super(source, relatedProject, ProjectViewFlavor.class.getSimpleName(), oldFlavor, newFlavor);
    }

    public ProjectViewFlavor getNewViewFlavor() {
        return (ProjectViewFlavor) getNewValue();
    }

    public ProjectViewFlavor getPreviousViewFlavor() {
        return (ProjectViewFlavor) getOldValue();
    }
}

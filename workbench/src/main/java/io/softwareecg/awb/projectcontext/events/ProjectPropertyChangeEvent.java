package io.softwareecg.awb.projectcontext.events;

import io.softwareecg.awb.projectcontext.model.Project;

import java.beans.PropertyChangeEvent;

/**
 * A specific type of PropertyChangeEvent that will use to provide
 * changes on projects itself or it's view state in the workbench
 *
 * @since 21.06.2018
 */
public class ProjectPropertyChangeEvent extends PropertyChangeEvent {

    private final Project relatedProject;

    /**
     * Constructs a new {@code ProjectPropertyChangeEvent}.
     *
     * @param source         the source that as published the event
     * @param relatedProject the project the changed property belongs to
     * @param propertyName   the programmatic name of the property that was changed
     * @param oldValue       the old value of the property
     * @param newValue       the new value of the property
     * @throws IllegalArgumentException if {@code source} is {@code null}
     */
    public ProjectPropertyChangeEvent(Object source, Project relatedProject, String propertyName, Object oldValue, Object newValue) {
        super(source, propertyName, oldValue, newValue);
        this.relatedProject = relatedProject;
    }

    public Project getRelatedProject() {
        return relatedProject;
    }
}

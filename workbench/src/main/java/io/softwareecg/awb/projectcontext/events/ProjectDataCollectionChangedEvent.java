package io.softwareecg.awb.projectcontext.events;

import java.util.EventObject;

/**
 * The state of the data collection of a project changed - it started, stopped, ran into an
 * error or recovered - or newer values arrived. Views that show the project ask
 * {@link io.softwareecg.awb.projectcontext.spi.ProjectDataCollection} for the new state.
 */
public class ProjectDataCollectionChangedEvent extends EventObject {

    private final String projectName;

    public ProjectDataCollectionChangedEvent(Object source, String projectName) {
        super(source);
        this.projectName = projectName;
    }

    public String getProjectName() {
        return projectName;
    }
}

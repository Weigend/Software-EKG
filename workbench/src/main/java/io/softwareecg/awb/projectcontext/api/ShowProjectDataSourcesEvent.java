package io.softwareecg.awb.projectcontext.api;

import java.util.EventObject;

/**
 * The user wants to see and manage the data sources of a project, e.g. by choosing
 * "Data Sources..." at the project.
 */
public class ShowProjectDataSourcesEvent extends EventObject {

    private final String projectName;

    public ShowProjectDataSourcesEvent(Object source, String projectName) {
        super(source);
        this.projectName = projectName;
    }

    public String getProjectName() {
        return projectName;
    }
}

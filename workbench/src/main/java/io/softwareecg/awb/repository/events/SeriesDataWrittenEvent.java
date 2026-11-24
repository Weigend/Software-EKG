package io.softwareecg.awb.repository.events;

import java.util.EventObject;

/**
 * New values of a project were written to its repository, as happens continuously while the
 * data of an analysis is collected. Views that show the project can follow. Only the collection
 * reports this; an import does not.
 */
public class SeriesDataWrittenEvent extends EventObject {

    private final String repositoryId;

    private final String projectName;

    private final long newestTimestamp;

    /**
     * @param source          who wrote the values
     * @param repositoryId    the id of the repository the project lives in
     * @param projectName     the project that got new values
     * @param newestTimestamp the time of the newest value written, in milliseconds since the epoch
     */
    public SeriesDataWrittenEvent(Object source, String repositoryId, String projectName, long newestTimestamp) {
        super(source);
        this.repositoryId = repositoryId;
        this.projectName = projectName;
        this.newestTimestamp = newestTimestamp;
    }

    public String getRepositoryId() {
        return repositoryId;
    }

    public String getProjectName() {
        return projectName;
    }

    public long getNewestTimestamp() {
        return newestTimestamp;
    }
}

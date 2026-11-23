package io.softwareecg.awb.sdk.importer.tasks;

import io.softwareecg.awb.sdk.awbapi.explorer.ExplorerUpdateEvent;
import io.softwareecg.awb.sdk.awbapi.project.TargetProjectConfig;
import io.softwareecg.awb.sdk.awbapi.repository.RepositoryDataEvent;
import io.softwareecg.awb.sdk.awbapi.repository.SeriesImportService;
import io.softwareecg.awb.runtime.events.AwbErrorEvent;
import io.softwareecg.awb.runtime.events.CancellableTaskEvent;
import io.softwareecg.wfx.platform.api.EventBus;
import io.softwareecg.wfx.platform.api.events.ProgressEvent;
import io.softwareecg.awb.sdk.core.log.EkgLogger;
import io.softwareecg.wfx.lookup.api.Lookup;
import javafx.application.Platform;
import javafx.concurrent.Task;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import java.util.EventObject;
import java.util.stream.Stream;

/**
 * Base representation of all import task that will
 * specify/implement the common API and save deriving concrete task for unnecessary glue code.
 *
 * @since 01.06.2018
 */
public abstract class ImportTask extends Task<Void> {

    private static final Logger LOGGER = EkgLogger.get();

    /**
     * EKG event bus to send progress, failure and completion evens to other components
     */
    private final EventBus<EventObject> eventBus;

    /**
     * A service that provides the functionality to import time series data
     * and query some series metadata.
     */
    private final SeriesImportService importService;

    private final TargetProjectConfig projectConfig;

    /**
     * Set when the user cancels the import. Distinct from {@link #isCancelled()},
     * which this task also uses to give up on an invalid configuration.
     */
    private volatile boolean cancelledByUser;

    //=================================================================================================================
    // constructor
    //=================================================================================================================

    /**
     * Constructs a new instance of ImportTask with with properties given as parameters.
     *
     * @param importService service that provides the functionality to import time series data
     * @param targetProjectConfig the name of the project the time-series belongs to
     */
    public ImportTask(SeriesImportService importService, TargetProjectConfig targetProjectConfig) {
        this.projectConfig = targetProjectConfig;
        this.importService = importService;
        this.eventBus = Lookup.lookup(EventBus.class);
    }

    //=================================================================================================================
    // API of the import task class
    //=================================================================================================================

    public void start() {
        new Thread(this).start();
    }

    public SeriesImportService getImportService() {
        return importService;
    }

    public String getProjectName() {
        return projectConfig.getProjectName();
    }

    public TargetProjectConfig getTargetProject() {
        return projectConfig;
    }

    /**
     * Ask the import to stop. Returns at once; the import stops before it reads
     * or writes the next series. A call to the repository that is already under
     * way - a write, or a connection attempt that is waiting for its timeout -
     * is not interrupted but left to finish first: interrupting the thread
     * could close files of the embedded repository it may be writing to.
     * <p>
     * Series written before the import stopped stay in the repository.
     */
    public void cancelImport() {
        cancelledByUser = true;
        cancel(false);
        eventBus.publish(new ProgressEvent("Cancelling the import into project '" + projectConfig.getProjectName()
                + "' ...", -1, this));
    }

    /** {@code true} once the user has cancelled the import. */
    public boolean isCancelledByUser() {
        return cancelledByUser;
    }

    /**
     * Ends a stream of series as soon as the user cancels the import. Import
     * tasks put their source stream through this, so that a cancelled import
     * processes at most the series it is working on.
     */
    protected <T> Stream<T> untilCancelled(Stream<T> series) {
        return series.takeWhile(element -> !cancelledByUser);
    }

    //=================================================================================================================
    // methods that should overwritten / implemented by deriving classes
    //=================================================================================================================

    /**
     * Executes the complete import by define and running all import
     * steps that are necessary for it.
     * As results the method will return the amount of all records that
     * are imported successfully.
     *
     * @return the number of records (series) that are imported.
     */
    abstract long doImport();

    //=================================================================================================================
    // Overwritten API of javafx.concurrent.Task class this Import Task is derived from
    //=================================================================================================================

    @Override
    protected Void call() {

        // check that the target repositorySelector is selected
        if (importService == null) {
            eventBus.publish(new ProgressEvent("Error during import in project '" + projectConfig.getProjectName()
                    + "'. No EkgRepository selected!", 1.0, this));
            cancel();
            return null;
        }

        // check that the a name/id the time series is specified
        if (StringUtils.isEmpty(projectConfig.getProjectName())) {
            eventBus.publish(new ProgressEvent("Error during import in project '" + projectConfig.getProjectName()
                    + "'. No series series name selected!", 1.0, this));
            cancel();
            return null;
        }

        eventBus.publish(CancellableTaskEvent.started(this, "Cancel import", this::cancelImport));
        try {
            doImport();
            if (cancelledByUser) {
                publishCancelled();
            } else {
                // Publish success events on JavaFX thread after import completes
                Platform.runLater(this::publishSuccessEvents);
            }
        } catch (Exception e) {
            if (cancelledByUser) {
                // Whatever the import was waiting for has failed after the user gave up
                // on it - typically a repository that cannot be reached. Not an error.
                LOGGER.info("Import for project '{}' ended after it was cancelled: {}",
                        projectConfig.getProjectName(), e.toString());
                publishCancelled();
            } else {
                LOGGER.error("Import failed for project '{}'", projectConfig.getProjectName(), e);
                eventBus.publish(new AwbErrorEvent(this, e));
            }
        } finally {
            eventBus.publish(CancellableTaskEvent.finished(this));
        }

        return null;
    }
    
    private void publishCancelled() {
        LOGGER.info("Import for project '{}' cancelled by the user", projectConfig.getProjectName());
        eventBus.publish(new ProgressEvent("Import into project '" + projectConfig.getProjectName()
                + "' cancelled.", 1.0, this));
    }

    /**
     * Publishes the success events after import completion.
     * Called via Platform.runLater() to ensure execution on JavaFX thread.
     */
    private void publishSuccessEvents() {
        try {
            eventBus.publish(new ProgressEvent("Import in series " + projectConfig.getProjectName()
                    + " successful.", 1.0, this));

            String repositoryId = importService.getConfiguredRepository().getId();
            eventBus.publish(new RepositoryDataEvent(importService.getConfiguredRepository()));
            eventBus.publish(new ExplorerUpdateEvent(repositoryId));
        } catch (Exception e) {
            LOGGER.error("Failed to publish success events for project '{}'", projectConfig.getProjectName(), e);
        }
    }

    @Override
    protected void succeeded() {
        // Note: This method is not called when the task is started via new Thread(this).start().
        // Success events are published via Platform.runLater() at the end of call().
    }
}

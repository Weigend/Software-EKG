package io.softwareecg.awb.projectcontext.api;

import io.softwareecg.awb.projectcontext.model.CloudPlatformType;
import io.softwareecg.awb.projectcontext.model.ProjectFlavor;

/**
 * An interface that represents the ProjectView data model object of EKG workbench.
 *
 * The ProjectView is a user defined context for a bunch of series data
 * the belongs together. For example this can be a software project or
 * in a specific flight than analyzing flight recorder logs.
 *
 * This interface only defines a base set of method that are required by importers in
 * some circumstances. The whole data model object will have much more properties
 * that are not relevant for the contract between AWB and importers.
 *
 * @since 04.02.2020
 */
public interface ProjectView {

    /**
     * The (unique) name of the project the series data
     * will imported to.
     *
     * @return the name of the project
     */
    String getName();

    /**
     * An (generated) identifier for the project.
     *
     * @return the id of the project
     */
    String getId();

    /**
     * The configured project flavor that means, the project
     * uses classic view, logical view or both.
     * Which on can chosen by the user is controlled by the importer itself.
     *
     * @return the configured project flavor that that controls which kind of view flavor is supported by the project
     */
    ProjectFlavor getProjectFlavor();

    /**
     * The configured cloud platform type that is only relevant
     * for time series data that is measured in context of it systems run inside a cloud platform.
     * The platform type only controls which of the default icons in the tree explorer will shown to the user.
     * For other data domains this property can be ignored.
     *
     * @return the configured cloud platform type of the current project
     */
    CloudPlatformType getCloudPlatformType();

    /**
     * A boolean flag that indicate if the project searches the series data by querying the EKG repository and
     * fetches the series payload on demand from a remote source (split source) or retrieve all data the EKG repository.
     *
     * @return a boolean flag if the project if of type split source or not.
     */
    boolean useSplitSource();
}

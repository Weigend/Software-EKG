package io.softwareecg.awb.project.api.model;

import io.softwareecg.awb.sdk.core.NamedEnum;

/**
 * Enum that defines the supported project types
 * of Software-EKG AWB.
 *
 * @since 14.07.2018
 */
public enum ProjectType implements NamedEnum {

    /**
     * Projects that store the meta data of each time series
     * in the EKG own repository and load the time series data
     * on demand from the remote source.
     */
    SPLIT_SOURCE_PROJECT("Project with Time-Series in External System"),

    /**
     * Projects that store the metadata and time series data itself
     * in the EKG onw repository. The repository can be the embedded or
     * remote instance.
     */
    LOCAL_PROJECT ("Project with Time-Series in EKG Repository");

    /**
     * The enum alias that will shown on UI
     */
    private final String name;

    /**
     * Constructs a new instance of the ProjectType
     * enumeration.
     *
     * @param name the alias name for this enum
     */
    ProjectType(String name) {
        this.name = name;
    }

    /**
     * Returns the human readable alias for the
     * enum value.
     *
     * @return the enum alias
     */
    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return super.toString();
    }
}

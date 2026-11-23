package io.softwareecg.awb.projectcontext.model;

import io.softwareecg.awb.runtime.NamedEnum;

/**
 * An Enum that represents the type of the project
 *
 * @since 21.06.2018
 */
public enum ProjectFlavor implements NamedEnum {

    // a single node/host or bare metal infrastructure composition without
    // an cloud platform parts
    CLASSIC("Classic (Bare-Metal System Group or Single System)"),

    // an mixed infrastructure with cloud native platform parts and traditional
    // bare-metal or visualized root nodes (for eg. OpenShift with external databases and EAI)
    HYBRID("Hybrid (Combined Cloud/Classic Usage)"),

    // a project that only contains time-series of cloud native platform components
    CLOUD_NATIVE("Cloud Native (Cloud Platform Only Infrastructure)");

    /**
     * the readable alias for the enumeration value
     */
    private final String name;

    /**
     * Constructs a ProjectFlavor Enum with the
     * specified value as metric value
     *
     * @param name the human readable name of the project type
     */
    ProjectFlavor(final String name) {
        this.name = name;
    }

    /* (non-Javadoc)
     * @see io.softwareecg.awb.runtime.NamedEnum#getRepositoryName()
     */
    @Override
    public String getName() {
        return name;
    }

    /* (non-Javadoc)
     * @see java.lang.Enum#toString()
     */
    @Override
    public String toString() {
        return super.toString();
    }

}

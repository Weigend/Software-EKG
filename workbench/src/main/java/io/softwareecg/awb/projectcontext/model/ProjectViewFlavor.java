package io.softwareecg.awb.projectcontext.model;

import io.softwareecg.awb.runtime.NamedEnum;

/**
 * An enum that represents the view state of the project.
 * The state LOGICAL_VIEW is only valid in combination with hybrid or cloud_native projects
 *
 * @since 21.06.2018
 */
public enum ProjectViewFlavor implements NamedEnum {

    /**
     * State "logical view" that represents the logical cloud native
     * infrastructure running on the physical one like deployments, pods, container ...
     */
    LOGICAL_VIEW ("Cloud-Native / Logical View"),

    /**
     * State "physical view" that represents the physical infrastructure
     * like hosts, processes
     */
    PHYSICAL_VIEW ("Classic / Physical View");

    /**
     * the readable alias for the enumeration value
     */
    private final String name;

    /**
     * Constructs a ProjectViewFlavor Enum with the
     * specified value as alias value
     *
     * @param name the human readable name of the project flavor
     */
    ProjectViewFlavor(final String name) {
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

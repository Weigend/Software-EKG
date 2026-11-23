package io.softwareecg.awb.projectcontext.model;

import io.softwareecg.awb.runtime.NamedEnum;

/**
 * An enum that represents the type of the CloudPlatform (if in use)
 * to control the view representation of the ProjectFlavor more detailed
 *
 * @since 23.06.2018
 */
public enum CloudPlatformType implements NamedEnum {

    NONE("Not Applicable"),

    OPEN_SHIFT("OpenShift"),

    KUBERNETES("Kubernetes"),

    OTHER("Other Cloud Platform");

    private final String name;

    /**
     * Constructs a new CloudPlatformType enum
     * this the value that is given by the name parameter.
     *
     * @param name user readable representation of the ENUM value
     */
    CloudPlatformType(String name) {
        this.name = name;
    }

    /**
     * Returns the user readable representation of the ENUM value
     *
     * @return the readable representation of the ENUM value
     */
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

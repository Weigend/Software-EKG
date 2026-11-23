package io.softwareecg.awb.projectcontext.model;

import io.softwareecg.awb.runtime.NamedEnum;

/**
 * Enumeration of supported filter dimension mapping types.
 * The mapping feature supports to define individual label for
 * the fixed set of filter dimensions.
 *
 * @since 11.07.2018
 */
public enum AliasMappingType implements NamedEnum {

    /**
     * EKG default filter dimensions (like host, metric, pod or namespace)
     */
    EKG_STANDARD ("EKG Standard"),

    /**
     * Importer specific mapping that are optional provided by each importer module
     */
    IMPORTER_SPECIFIC ("Importer-Specific Filter Dimensions"),

    /**
     * User specific mappings defined per project
     */
    CUSTOM ("User-Defined Filter Dimensions");

    private final String name;


    AliasMappingType(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return super.toString();
    }

}

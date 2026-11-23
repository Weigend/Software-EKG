package io.softwareecg.awb.timeseries.dimension;

import org.apache.commons.lang3.StringUtils;

/**
 * A domain entity that represents a process group 
 * and stores the properties that are part of it.
 *
 */
public class ProcessGroup extends NamedValueEntity {

    public static final ProcessGroup DEFAULT = new ProcessGroup("*");

    public ProcessGroup() {
        this("");
    }

    public ProcessGroup(String valueName) {
        super(valueName);
    }

    @Override
    public String toString() {
        return ProcessGroup.class.getSimpleName() + "[ "
                + (StringUtils.isBlank(getValueName()) ? "-BLANK-" : getValueName()) + " ]";
    }
}

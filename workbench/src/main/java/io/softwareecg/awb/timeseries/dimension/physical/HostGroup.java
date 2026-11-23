package io.softwareecg.awb.timeseries.dimension.physical;

import io.softwareecg.awb.timeseries.dimension.NamedValueEntity;
import org.apache.commons.lang3.StringUtils;

/**
 * An domain entity that represents a host group / cluster
 * and stores the properties that are part of it.
 *
 * @since 10.07.2018
 */
public class HostGroup extends NamedValueEntity {

    public static final HostGroup DEFAULT = new HostGroup("*");

    public HostGroup() {
        this("");
    }

    public HostGroup(String valueName) {
        super(valueName);
    }

    @Override
    public String toString() {
        return HostGroup.class.getSimpleName() + "[ "
                + (StringUtils.isBlank(getValueName()) ? "-BLANK-" : getValueName()) + " ]";
    }
}

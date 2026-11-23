package io.softwareecg.awb.metricview.queryeditor.converter;

import io.softwareecg.awb.timeseries.dimension.physical.HostGroup;
import javafx.util.StringConverter;

/**
 * todo enter comment here
 *
 * @since 10.07.2018
 */
public class HostGroupStringConverter extends StringConverter<HostGroup> {

    @Override
    public String toString(HostGroup entity) {
        if (entity != null) {
            return entity.getValueName();
        }
        return "*";
    }

    @Override
    public HostGroup fromString(String s) {
        if (s != null) {
            return new HostGroup(s);
        }
        return new HostGroup("*");
    }
}

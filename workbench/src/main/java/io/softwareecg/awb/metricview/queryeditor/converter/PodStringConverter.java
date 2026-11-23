package io.softwareecg.awb.metricview.queryeditor.converter;

import io.softwareecg.awb.timeseries.dimension.logical.Pod;
import javafx.util.StringConverter;

/**
 * todo enter comment here
 *
 * @since 10.07.2018
 */
public class PodStringConverter extends StringConverter<Pod> {

    @Override
    public String toString(Pod entity) {
        if (entity != null) {
            return entity.getValueName();
        }
        return "*";
    }

    @Override
    public Pod fromString(String s) {
        if (s != null) {
            return new Pod(s);
        }
        return new Pod("*");
    }
}

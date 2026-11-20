package io.softwareecg.awb.metricanalyzer.ui.filterheader.converter;

import io.softwareecg.awb.metricanalyzer.bl.tsquery.et.logical.Pod;
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

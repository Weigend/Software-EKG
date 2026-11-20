package io.softwareecg.awb.metricanalyzer.ui.filterheader.converter;

import io.softwareecg.awb.metricanalyzer.bl.tsquery.et.logical.Container;
import javafx.util.StringConverter;

/**
 * todo enter comment here
 *
 * @since 10.07.2018
 */
public class ContainerStringConverter extends StringConverter<Container> {

    @Override
    public String toString(Container container) {
        if (container != null) {
            return container.getValueName();
        }
        return "*";
    }

    @Override
    public Container fromString(String s) {
        if (s != null) {
            return new Container(s);
        }
        return new Container("*");
    }
}
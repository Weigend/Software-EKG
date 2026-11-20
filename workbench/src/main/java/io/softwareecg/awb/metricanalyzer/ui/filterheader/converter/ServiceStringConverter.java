package io.softwareecg.awb.metricanalyzer.ui.filterheader.converter;

import io.softwareecg.awb.metricanalyzer.bl.tsquery.et.logical.Service;
import javafx.util.StringConverter;

/**
 * todo enter comment here
 *
 * @since 10.07.2018
 */
public class ServiceStringConverter extends StringConverter<Service> {

    @Override
    public String toString(Service service) {
        if (service != null) {
            return service.getValueName();
        }
        return "*";
    }

    @Override
    public Service fromString(String s) {
        if (s != null) {
            return new Service(s);
        }
        return new Service("*");
    }
}
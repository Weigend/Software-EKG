package io.softwareecg.awb.metricview.queryeditor.converter;

import io.softwareecg.awb.timeseries.dimension.logical.Namespace;
import javafx.util.StringConverter;

import java.util.Objects;

/**
 * todo enter comment here
 *
 * @since 10.07.2018
 */
public class NamespaceStringConverter extends StringConverter<Namespace> {

    @Override
    public String toString(Namespace entity) {
        if (entity != null) {
            return entity.getValueName();
        }
        return "*";
    }

    @Override
    public Namespace fromString(String s) {
        return new Namespace(Objects.requireNonNullElse(s, "*"));
    }
}


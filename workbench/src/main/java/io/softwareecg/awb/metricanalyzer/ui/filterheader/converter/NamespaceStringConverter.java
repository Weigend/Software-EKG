package io.softwareecg.awb.metricanalyzer.ui.filterheader.converter;

import io.softwareecg.awb.metricanalyzer.bl.tsquery.et.logical.Namespace;
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


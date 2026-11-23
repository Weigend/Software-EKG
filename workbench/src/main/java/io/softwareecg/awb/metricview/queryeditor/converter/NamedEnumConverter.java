package io.softwareecg.awb.metricview.queryeditor.converter;

import io.softwareecg.awb.runtime.NamedEnum;
import javafx.util.StringConverter;

/**
 * An Enum to String converter that will provide the alias name
 * for the given enum
 *
 * @since 23.07.2018
 */
public class NamedEnumConverter<T extends NamedEnum> extends StringConverter<T> {

    @Override
    public String toString(NamedEnum namedEnum) {
        return namedEnum.getName();
    }

    @Override
    public T fromString(String string) {
        return null;
    }
}

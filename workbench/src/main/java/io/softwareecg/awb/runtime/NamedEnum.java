package io.softwareecg.awb.runtime;

/**
 * Interface that represents Enum classes that provide an
 * alias name of the value for the UI.
 *
 * @since 29.06.2018
 */
public interface NamedEnum {

    /**
     * Return an user friendly to read alias name
     * that describes the enum value.
     *
     * @return the alias name of the enum value
     */
    String getName();

    /**
     * What choosing the value does, in a sentence for a tooltip. A name such as "Concatenate"
     * does not say it.
     *
     * @return the description, or an empty text if the value has none
     */
    default String getDescription() {
        return "";
    }
}

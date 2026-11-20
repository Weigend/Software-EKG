package io.softwareecg.awb.sdk.core;

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
}

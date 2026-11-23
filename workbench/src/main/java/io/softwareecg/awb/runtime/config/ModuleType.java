package io.softwareecg.awb.runtime.config;

/**
 * Enumeration that defines the type of an EKG AWB ui module.
 *
 * @since 18.11.2018
 */
public enum ModuleType {

    /**
     * Import modules that is responsible to load and transform
     * data from domain specific sources into the AWB repository
     */
    IMPORT_MODULE,

    /**
     * Modules that are part of the base application
     */
    CORE_MODULE,

    /**
     * Modules that are not import data but extend the base application
     * with additional features like automated analytics or visualisations.
     */
    EXTENSION_MODULE
}

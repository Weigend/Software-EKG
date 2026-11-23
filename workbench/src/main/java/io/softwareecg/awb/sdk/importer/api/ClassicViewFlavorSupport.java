package io.softwareecg.awb.sdk.importer.api;

import io.softwareecg.awb.timeseries.dimension.FilterDimension;

/**
 * A marker interface that represents all import modules that support the concept of
 * classic filter dimensions (physical view).
 *
 * If a module is marked with this interface, the user gets the option to select classic
 * only (or together which {@link CloudViewFlavorSupport})
 * hybrid view flavors in projects.
 *
 * Be care that an importer must fulfill classic filter dimensions
 * (see {@link FilterDimension} on import error will
 * caused at analyses.
 *
 * @since 16.07.2018
 */
public interface ClassicViewFlavorSupport {
}

package io.softwareecg.awb.sdk.importer.api;


import io.softwareecg.awb.sdk.awbapi.repository.FilterDimension;

/**
 * A marker interface that represents all import modules that support the concept of
 * cloud native filter dimensions (logical view).
 *
 * If a module is marked with this interface, the user gets the option to select cloud native
 * only (or together which {@link ClassicViewFlavorSupport})
 * hybrid view flavors in projects.
 *
 * Be care that an importer must fulfill cloud native filter dimensions
 * (see {@link FilterDimension} on import error will
 * caused at analyses.
 *
 * @since 13.07.2018
 */
public interface CloudViewFlavorSupport {

}

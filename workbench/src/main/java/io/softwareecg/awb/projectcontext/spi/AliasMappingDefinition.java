package io.softwareecg.awb.projectcontext.spi;

import io.softwareecg.awb.timeseries.dimension.FilterDimension;

/**
 * This interface represents bean that provides the custom aliases
 * for the Software-EKG filter dimensions.
 * These mapping will used to use the EKG model in context of different
 * domains.
 *
 * @since 17.07.2018
 */
public interface AliasMappingDefinition {

    /**
     * Returns the human readable label for the given filter
     * dimension that will displayed at the UI.
     * If there is no custom label for the dimension available null will returned.
     *
     * @param dimension the EKG filter dimension
     * @return the label for the specified dimension or null if no custom dimension exists
     */
    String getAliasLabel(FilterDimension dimension);
}

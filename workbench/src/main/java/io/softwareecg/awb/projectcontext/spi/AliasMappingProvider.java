package io.softwareecg.awb.projectcontext.spi;

import io.softwareecg.awb.metadata.model.AliasMappingDefinition;

/**
 * Interface that represents factory classes that provide
 * a {@link AliasMappingDefinition} bean that supply alias labels
 * for each supported EKG filter dimension.
 *
 * This interface will implemented by EKG importer modules that want to use
 * the standard filter dimensions in the context of it's own specific domain.
 *
 * @since 17.07.2018
 */
public interface AliasMappingProvider {

    /**
     * Returns a {@link AliasMappingDefinition} instance with domain specific aliases
     * for each supported filter dimension.
     *
     * @return the container that provides alias definitions of a specific domain mapped to the EKG standard dimensions.
     */
    AliasMappingDefinition getAliasMappingDefinition();
}

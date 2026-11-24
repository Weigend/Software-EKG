package io.softwareecg.awb.importer.framework.steps.mapper;

import io.softwareecg.awb.repository.api.Repository;
import io.softwareecg.awb.importer.model.SeriesData;

import java.util.function.Function;

/**
 * todo enter comment here
 *
 * @since 14.07.2018
 */
public interface ComputeMetadataProcessor<TIN extends SeriesData, TOUT extends SeriesData>
        extends Function<TIN, TOUT> {

    default void setMetaDataRepository(Repository repository) {
        // no operation
    }
}
package io.softwareecg.awb.sdk.importer.tasks.steps.mapper;

import io.softwareecg.awb.sdk.awbapi.repository.Repository;
import io.softwareecg.awb.sdk.datamodel.SeriesData;

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
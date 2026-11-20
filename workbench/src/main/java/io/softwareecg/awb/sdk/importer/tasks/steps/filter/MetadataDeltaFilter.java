package io.softwareecg.awb.sdk.importer.tasks.steps.filter;

import io.softwareecg.awb.sdk.awbapi.repository.Repository;
import io.softwareecg.awb.sdk.datamodel.SeriesData;

import java.util.function.Predicate;

/**
 * todo enter comment here
 *
 * @since 14.07.2018
 */
public interface MetadataDeltaFilter<T extends SeriesData> extends Predicate<T> {

      default void setMetaDataRepository(Repository repository) {
          // no operation an default implementation
      }
}
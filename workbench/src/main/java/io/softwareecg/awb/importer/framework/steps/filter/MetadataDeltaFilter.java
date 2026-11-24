package io.softwareecg.awb.importer.framework.steps.filter;

import io.softwareecg.awb.repository.api.Repository;
import io.softwareecg.awb.importer.model.SeriesData;

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
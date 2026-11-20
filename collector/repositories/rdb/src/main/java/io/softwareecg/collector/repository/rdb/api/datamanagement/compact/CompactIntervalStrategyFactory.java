package io.softwareecg.collector.repository.rdb.api.datamanagement.compact;

import io.softwareecg.collector.repository.rdb.api.RdbProperties;

import java.lang.reflect.Constructor;

/**
 * todo insert comment here
 *
 * @since 29.11.2018
 */
public class CompactIntervalStrategyFactory {

    public static CompactIntervalStrategy getCompactStrategy() {

        String compactStrategy = RdbProperties.getCompactIntervalStrategy();

        String strategyName;

        if (compactStrategy.contains(".")) {
            // full qualified class name including packages
            strategyName = compactStrategy;

        } else {
            // only class name given, use own package
            strategyName = CompactIntervalStrategyFactory.class.getPackageName() + "." + compactStrategy;
        }

        try {

            Class strategyClass = Class.forName(strategyName);
            Constructor<CompactIntervalStrategy>[] constructors = strategyClass.getConstructors();

            CompactIntervalStrategy compactInterval = null;
            for (Constructor<CompactIntervalStrategy> constructor : constructors) {
                if (constructor.getParameterCount() == 0) {
                    compactInterval = constructor.newInstance();
                }
            }

            return compactInterval;

        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}

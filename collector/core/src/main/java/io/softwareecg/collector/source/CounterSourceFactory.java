//______________________________________________________________________________
//
//          Project:    Software EKG
//______________________________________________________________________________
//
//       created by:    Johannes Weigend, Josef Adersberger
//       creation date: 2009
//       changed by:    $Author: weigend $
//       change date:   $Date: 2010-03-04 17:28:30 +0100 (Do, 04 Mrz 2010) $
//       revision:      $Revision: 421 $
//       description:   Access performance counter.
//______________________________________________________________________________
//
//        Copyright:    Weigend AM
//______________________________________________________________________________
package io.softwareecg.collector.source;

import io.softwareecg.collector.common.Registry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.ServiceConfigurationError;


/**
 * Factory that provides prototype and singleton instances
 * of the various counter sources.
 * The requested classes will resolved automatically using class path scan/services loader.
 *
 * @since 18.03.2014
 */
public final class CounterSourceFactory {

    /**
     * the logger used by the factory
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(CounterSourceFactory.class);

    /**
     * Map with all the different counter source implementations.
     */
    private static final Map<String, CounterSource> COUNTER_SOURCES = new HashMap<>();

    /**
     * Not used.
     */
    private CounterSourceFactory() {
        // no op
    }

    /**
     * Returns a singleton instance of CounterSource with the given id.
     * The id is the collector wide unique counter source id like 'jmx' or 'jolokia'
     * and must be equal to the one of the id the {@link CounterSource} implementation's
     * return calling the getId() method.
     *
     * @param id the id of the counter source (type)
     * @return a singleton instance of the requested counter source
     */
    public static CounterSource getSingletonInstance(String id) {

        if (COUNTER_SOURCES.isEmpty()) {
            Iterator<CounterSource> sources = Registry.getDefault().lookupAll(CounterSource.class);
            while (true) {
                try {
                    if (!sources.hasNext()) {
                        break;
                    }
                    CounterSource next = sources.next();
                    COUNTER_SOURCES.put(next.getId(), next);
                } catch (ServiceConfigurationError | LinkageError e) {
                    // A source whose libraries are not on the classpath - e.g. the calculated
                    // counters without the JavaScript engine, when the collector is embedded
                    // into another application - is left out; the others stay usable.
                    LOGGER.warn("Skipping a counter source that cannot be loaded: {}", e.toString());
                }
            }
            LOGGER.info("Found " + COUNTER_SOURCES.size() + " Sources in classpath: " + COUNTER_SOURCES);
        }

        CounterSource result = COUNTER_SOURCES.get(id);
        if (result == null) {
            throw new IllegalStateException("Invalid counter source: '" + id + "'");
        }

        return result;
    }

    /**
     * Returns a new created instance of CounterSource with the given id.
     * The id is the collector wide unique counter source id like 'jmx' or 'jolokia'
     * and must be equal to the one of the id the {@link CounterSource} implementation's
     * return calling the getId() method.
     *
     * @param id the id of the counter source (type)
     * @return a prototype instance of the requested counter source
     */
    public static CounterSource getPrototypeInstance(String id) {
        try {
            return COUNTER_SOURCES.get(id).getClass().getDeclaredConstructor().newInstance();
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Can not instantiate counter for: " + id, e);
        }
    }
}

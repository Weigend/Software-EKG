//______________________________________________________________________________
//
//          Project:    Software EKG
//______________________________________________________________________________
//
//         Author:      Weigend AM 2009 - 2014
//______________________________________________________________________________
//
// Notice: This piece of software was created, designed and implemented by
// experienced craftsmen and innovators in Munich, Germany.
// Changes should be done with respect to the original design.
//______________________________________________________________________________
package io.softwareecg.collector.common;

import java.util.Iterator;
import java.util.ServiceLoader;


/**
 * Minimal Registry for collector source plugins. The plugins will be loaded from the classpath.
 * A plugin services is identified by the META-INF/services mechanism. @see java.datamanagement.ServiceLoader.
 */
public class Registry {

    /**
     * The registry.
     */
    private static final Registry registry = new Registry();

    /**
     * Singleton.
     *
     * @return the one registry.
     */
    public static Registry getDefault() {
        return registry;
    }

    /**
     * Lookup a service by using JDK 6 ServiceLoader. If more than one service is available, you can select
     * by using a system property for the service name (-Dio.softwareecg.xy.IService=io.softwareecg.xy.impl.XYServiceImpl).
     * The service has in every case declared in the META-INF/services/io.softwareecg.xy.IService file.
     *
     * @param <T>   clazz the interface of the services.
     * @param clazz the interface of the services.
     * @return the impl.
     */
    public <T> T lookup(Class<T> clazz) {
        ServiceLoader<T> loader = ServiceLoader.load(clazz);
        String property = CollectorProperties.getProperties().getProperty(clazz.getName());
        if (property == null) {
            // get first service in META-INF/services/clazz.getName()
            if (!loader.iterator().hasNext()) {
                throw new IllegalStateException("Unknown service: " + clazz.getName() + ". Please check the META-INF/services/" + clazz.getName() + " file.");
            }
            return loader.iterator().next();
        }

        property = property.trim();

        for (T service : loader) {
            if (service.getClass().getName().equals(property)) {
                return service;
            }
        }
        throw new IllegalStateException("Configuration Error: System property '" + clazz.getName() + "' does not match service in META-INF/services.");
    }

    /**
     * Lookup all services by using JDK 6 ServiceLoader.
     *
     * @param <T>   clazz the interface of the services.
     * @param clazz the interface of the services.
     * @return the impl.
     */
    public <T> Iterator<T> lookupAll(Class<T> clazz) {
        ServiceLoader<T> loader = ServiceLoader.load(clazz);
        return loader.iterator();
    }
}

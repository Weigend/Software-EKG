package io.softwareecg.awb.runtime.discovery;

import io.softwareecg.awb.runtime.di.DiInjection;
import io.softwareecg.wfx.lookup.api.Lookup;
import org.apache.commons.lang3.reflect.ConstructorUtils;

/**
 * A service discovery that creates prototypes of services
 * that have a default and a parameterized constructor.
 *
 * @since 17.08.2018
 */
public class ServiceDiscovery {

    public static <T> T lookup(Class<T> clazz, Object ... params) {

        if (params == null ||params.length == 0) {
            return Lookup.lookup(clazz);
        }

        try {
            //noinspection unchecked
            T instance = (T) ConstructorUtils.invokeConstructor(Lookup.lookup(clazz).getClass(), params);
            DiInjection.injectFields(instance);
            return instance;

        } catch (Exception e) {
            if (e instanceof NoSuchMethodException) {
                throw new IllegalStateException("The class of typ " + clazz.getName()
                        + " doesn't provide an constructor matching to the given argument types.");
            }

            throw new IllegalStateException("Unable to find an implementation of typ " + clazz.getName());
        }
    }
}

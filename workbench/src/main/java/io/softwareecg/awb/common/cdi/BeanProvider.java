//______________________________________________________________________________
//
//                  Project:    Software EKG
//______________________________________________________________________________
package io.softwareecg.awb.common.cdi;

import io.avaje.inject.BeanScope;
import io.softwareecg.wfx.lookup.api.Lookup;
import io.softwareecg.wfx.platform.core.eventbus.AnnotationProcessor;
import jakarta.inject.Inject;

import java.lang.reflect.Field;

/**
 * Performs dependency injection on instances that are not managed by the
 * active DI container themselves (FXML controllers, ServiceLoader-discovered
 * items, dialogs instantiated via {@code new}).
 * <p>Uses the Avaje {@link BeanScope} exposed through WFX {@link Lookup}.
 */
public final class BeanProvider {

    private BeanProvider() {
    }

    /**
     * Performs dependency injection on the given instance.
     *
     * @param instance current instance, may be {@code null}
     * @param <T>      current type
     * @return the same instance with {@code @Inject}-fields populated, or
     *         {@code null} if the input was {@code null}
     */
    public static <T> T injectFields(T instance) {
        if (instance == null) {
            return null;
        }
        BeanScope scope = tryLookup(BeanScope.class);
        if (scope == null) {
            throw new IllegalStateException(
                    "No DI container active - Avaje BeanScope is not reachable through Lookup");
        }
        injectViaAvaje(instance, scope);
        AnnotationProcessor.process(instance);
        return instance;
    }

    private static <T> T injectViaAvaje(T instance, BeanScope scope) {
        Class<?> cls = instance.getClass();
        while (cls != null && cls != Object.class) {
            for (Field field : cls.getDeclaredFields()) {
                if (!field.isAnnotationPresent(Inject.class)) {
                    continue;
                }
                field.setAccessible(true);
                try {
                    if (!scope.contains(field.getType())) {
                        continue;
                    }
                    String qualifier = qualifierName(field);
                    Object dependency = (qualifier != null)
                            ? scope.get(field.getType(), qualifier)
                            : scope.get(field.getType());
                    field.set(instance, dependency);
                } catch (ReflectiveOperationException e) {
                    throw new IllegalStateException(
                            "Failed to inject field '" + field.getName() + "' on " + cls.getName(), e);
                }
            }
            cls = cls.getSuperclass();
        }
        return instance;
    }

    /**
     * Tolerant {@code Lookup.lookup}: returns {@code null} if the strategy
     * does not have the type registered (instead of throwing). The two DI
     */
    private static <T> T tryLookup(Class<T> type) {
        try {
            return Lookup.lookup(type);
        } catch (RuntimeException e) {
            return null;
        }
    }

    /**
     * Avaje stores a single qualifier name per bean (simple name of the
     * {@code @Qualifier}-meta annotation). A field annotated {@code @Embedded}
     * thus requests the bean named "Embedded".
     */
    private static String qualifierName(Field field) {
        for (java.lang.annotation.Annotation a : field.getAnnotations()) {
            Class<? extends java.lang.annotation.Annotation> type = a.annotationType();
            if (type == Inject.class) {
                continue;
            }
            if (type.isAnnotationPresent(jakarta.inject.Qualifier.class)) {
                return type.getSimpleName();
            }
        }
        return null;
    }
}

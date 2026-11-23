//______________________________________________________________________________
//
//                  Project:    Software EKG
//______________________________________________________________________________
package io.softwareecg.awb.runtime.di;

import io.avaje.inject.BeanScope;
import io.softwareecg.wfx.lookup.api.Lookup;
import io.softwareecg.wfx.platform.core.eventbus.AnnotationProcessor;
import jakarta.inject.Inject;
import jakarta.inject.Qualifier;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;

/**
 * Performs reflection-based dependency injection on instances that are not
 * managed by the active DI container themselves (FXML controllers,
 * ServiceLoader-discovered items, dialogs instantiated via {@code new}).
 * <p>
 * Resolves {@code @Inject} fields through the Avaje {@link BeanScope}
 * exposed via WFX {@link Lookup}, honouring JSR-330 {@code @Qualifier}
 * annotations. After field injection, hands the instance to
 * {@link AnnotationProcessor} so any {@code @EventSubscriber}-annotated
 * methods are wired to the WFX event bus.
 *
 * <p>Avaje does not provide an out-of-the-box {@code BeanScope.inject(instance)}
 * because its compile-time DI model expects beans to be wired by their
 * generated {@code $DI} class. For non-bean instances the only path is
 * reflection — which means classes injected this way require GraalVM
 * reachability metadata for native-image builds.
 *
 * <p>Replaces the legacy {@code CdiInjection}/{@code AvajeInjection} helpers.
 */
public final class DiInjection {

    private DiInjection() {
    }

    /**
     * Performs dependency injection on an instance, resolving the
     * {@link BeanScope} through {@link Lookup}.
     *
     * @param instance the target instance, may be {@code null}
     * @param <T>      the instance type
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
                    "No DI container active — Avaje BeanScope is not reachable through Lookup");
        }
        return injectFields(instance, scope);
    }

    /**
     * Performs dependency injection on an instance using an explicit
     * {@link BeanScope}. Useful for tests that build a scope directly without
     * going through {@link Lookup}.
     */
    public static <T> T injectFields(T instance, BeanScope scope) {
        if (instance == null) {
            return null;
        }
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
        AnnotationProcessor.process(instance);
        return instance;
    }

    private static <T> T tryLookup(Class<T> type) {
        try {
            return Lookup.lookup(type);
        } catch (RuntimeException e) {
            return null;
        }
    }

    private static String qualifierName(Field field) {
        for (Annotation a : field.getAnnotations()) {
            Class<? extends Annotation> type = a.annotationType();
            if (type == Inject.class) {
                continue;
            }
            if (type.isAnnotationPresent(Qualifier.class)) {
                return type.getSimpleName();
            }
        }
        return null;
    }
}

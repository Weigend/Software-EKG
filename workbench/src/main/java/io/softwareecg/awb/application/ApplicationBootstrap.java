package io.softwareecg.awb.application;

import io.avaje.inject.InjectModule;
import jakarta.inject.Scope;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Marks beans that must be registered before the generic WFX window module.
 */
@Scope
@InjectModule(name = "ApplicationWindow")
@Retention(RUNTIME)
@Target(TYPE)
public @interface ApplicationBootstrap {
}

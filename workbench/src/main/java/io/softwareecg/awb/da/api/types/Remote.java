package io.softwareecg.awb.da.api.types;

import jakarta.inject.Qualifier;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Qualifier to identify the resources that our outside the Workbench process/application
 * like an external server-side EKG repository used by multiple EKG applications.
 *
 * @since 25.08.2018
 */
@Qualifier
@Retention(RUNTIME)
@Target({TYPE, METHOD, FIELD, PARAMETER})
public @interface Remote {
}
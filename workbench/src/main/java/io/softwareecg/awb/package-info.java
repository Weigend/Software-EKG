/**
 * Software EKG desktop workbench.
 *
 * <p>The former API, business-logic, UI, importer and SDK artifacts are now
 * packages of one Maven module. Most beans share this injection module; the
 * application window has a small bootstrap scope so it can replace the WFX
 * fallback window before the remaining beans are wired.</p>
 */
@InjectModule(requires = EkgApplicationController.class)
package io.softwareecg.awb;

import io.avaje.inject.InjectModule;
import io.softwareecg.awb.application.EkgApplicationController;

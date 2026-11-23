/**
 * Software EKG desktop workbench.
 *
 * <p>The packages are the components of the workbench, named by what they do.
 * Most beans share this injection module; the
 * application window has a small bootstrap scope so it can replace the WFX
 * fallback window before the remaining beans are wired.</p>
 */
@InjectModule(requires = EkgApplicationController.class)
package io.softwareecg.awb;

import io.avaje.inject.InjectModule;
import io.softwareecg.awb.application.EkgApplicationController;

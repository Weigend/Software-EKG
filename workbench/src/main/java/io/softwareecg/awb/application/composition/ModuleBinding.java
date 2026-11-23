//______________________________________________________________________________
//
//                  ProjectConfiguration:    Software EKG
//______________________________________________________________________________
//
//                   Author:    Weigend AM 2009 - 2017
//______________________________________________________________________________
//
// Notice: This piece of software was created, designed and implemented by
// experienced craftsmen and innovators in Munich, Germany.
// Changes should be done with respect to the original design.
//______________________________________________________________________________
package io.softwareecg.awb.application.composition;

import io.softwareecg.wfx.lookup.api.Lookup;
import io.softwareecg.wfx.lookup.serviceloader.ServiceLoaderLookupStrategy;
import javafx.fxml.FXMLLoader;

/**
 * Bootstraps the {@link Lookup} for AWB applications that don't go through
 * {@code wfx.main.Main} — primarily test harnesses.
 * <p>
 * If a strategy was already installed (the normal case when running through
 * WFX startup), this is a no-op. Otherwise a
 * {@link ServiceLoaderLookupStrategy} is configured with the JavaFX
 * {@link FXMLLoader} producer.
 * <p>
 * Historical context (2026-04): this class used to bridge a separate
 * {@code EkgLookup}/{@code EkgEventBus} into the WFX equivalents. Both
 * abstractions have since been removed and the AWB code uses {@code Lookup}
 * and {@code EventBus} directly, so this bootstrap is the only thing left.
 */
public final class ModuleBinding {
    private ModuleBinding() {
    }

    /**
     * Initialises the {@link Lookup} with a {@link ServiceLoaderLookupStrategy}
     * if no strategy is set yet. Idempotent.
     */
    public static void bind() {
        if (Lookup.getLookupStrategy() != null) {
            return;
        }
        ServiceLoaderLookupStrategy strategy = new ServiceLoaderLookupStrategy();
        strategy.init(FXMLLoader.class, (ServiceLoaderLookupStrategy.Producer<FXMLLoader>) FXMLLoader::new);
        Lookup.init(strategy);
    }
}

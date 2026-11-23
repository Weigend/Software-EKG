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
package io.softwareecg.awb.metricanalyzer.ui;

import io.softwareecg.awb.runtime.module.EkgModule;
import io.softwareecg.awb.explorer.ui.TreeIconsProviderRegistry;
import io.softwareecg.awb.metricanalyzer.ui.api.MetricTreeIconsProvider;
import io.softwareecg.awb.metricanalyzer.ui.explorer.ItemBuilderManager;
import io.softwareecg.wfx.lookup.api.Lookup;
import jakarta.inject.Singleton;

/**
 * The EKG Module. Prepare the ui while preloading and handles
 * the start and stop of the UI.
 *
 */
@Singleton
public class MetricAnalyzerUiModule implements EkgModule {

    /**
     * Get the human readable module name.
     *
     * @return The module name.
     */
    public String getName() {
        return "Software-EKG - MetricAnalyzer UI";
    }

    /**
     * Get the version of this module.
     *
     * @return The version of the module.
     */
    public String getVersion() {
        return getClass().getPackage().getImplementationVersion();
    }

    @Override
    public void preload() {
        Lookup.lookup(TreeIconsProviderRegistry.class).registerDefaultTreeIconProvider(MetricTreeIconsProvider.DEFAULT);
        ItemBuilderManager.registerTreeItemBuilders();
    }

    @Override
    public void start() {

    }

    @Override
    public void stop() {

    }
}

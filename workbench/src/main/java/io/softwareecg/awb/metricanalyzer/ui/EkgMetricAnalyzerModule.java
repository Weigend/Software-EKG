package io.softwareecg.awb.metricanalyzer.ui;

import io.softwareecg.awb.application.api.EkgApplicationController;
import io.softwareecg.awb.application.api.events.BeforeRestoreDefaultLayoutEvent;
import io.softwareecg.awb.common.ui.events.FinishEvent;
import io.softwareecg.awb.metricanalyzer.ui.adapter.ChartPanelAdapter;
import io.softwareecg.wfx.lookup.api.Lookup;
import io.softwareecg.wfx.platform.api.EventBus;
import io.softwareecg.wfx.platform.api.Module;
import io.softwareecg.wfx.platform.api.events.ProgressEvent;
import jakarta.annotation.Priority;
import jakarta.inject.Singleton;
import javafx.scene.control.MenuItem;

import java.util.EventObject;

/**
 * Application-level wrapper that starts the Metric Analyzer UI integration.
 */
@Singleton
@Priority(200)
@SuppressWarnings("unused")
public class EkgMetricAnalyzerModule extends MetricAnalyzerUiModule implements Module {

    @Override
    public void preload() {
        EventBus<EventObject> eventBus = Lookup.lookup(EventBus.class);
        try {
            // Force to execute all other preloaders
            Thread.sleep(500);
            eventBus.publish(new ProgressEvent("Start local Solr Server...", 0, this));

            ChartPanelAdapter.ChartPanelManager chartPanelManager = Lookup.lookup(ChartPanelAdapter.ChartPanelManager.class);
            chartPanelManager.init();
            eventBus.subscribe(BeforeRestoreDefaultLayoutEvent.class, event -> {
                chartPanelManager.removeAllDynamicChartPanels();
                return false;
            });
            eventBus.publish(new ProgressEvent("Finished starting local Solr Server...", 1, this));

            super.preload();
        } catch (InterruptedException e) {
            eventBus.publish(new FinishEvent(-1, this));
            Thread.currentThread().interrupt();
        }
    }

    @Override
    public void start() {
        EkgApplicationController appController = Lookup.lookup(EkgApplicationController.class);
        MenuItem newChart = new MenuItem("New Chart");
        newChart.setMnemonicParsing(false);
        newChart.setOnAction(e -> Lookup.lookup(ChartPanelAdapter.ChartPanelManager.class).createNewChartPanel());
        appController.addWindowMenuItem(newChart);
    }
}

package io.softwareecg.awb.metricview;

import io.softwareecg.awb.application.api.EkgApplicationController;
import io.softwareecg.awb.application.api.events.BeforeRestoreDefaultLayoutEvent;
import io.softwareecg.wfx.lookup.api.Lookup;
import io.softwareecg.wfx.platform.api.EventBus;
import io.softwareecg.wfx.platform.api.Module;
import jakarta.annotation.Priority;
import jakarta.inject.Singleton;
import javafx.scene.control.MenuItem;

import java.util.EventObject;

/**
 * Application-level wrapper that starts the metric view.
 */
@Singleton
@Priority(200)
@SuppressWarnings("unused")
public class EkgMetricViewModule implements Module {

    @Override
    public String getName() {
        return "Software-EKG - Metric View";
    }

    @Override
    @SuppressWarnings("unchecked")
    public void preload() {
        ChartPanelManager chartPanelManager = Lookup.lookup(ChartPanelManager.class);
        chartPanelManager.init();

        EventBus<EventObject> eventBus = Lookup.lookup(EventBus.class);
        eventBus.subscribe(BeforeRestoreDefaultLayoutEvent.class, event -> {
            chartPanelManager.removeAllDynamicChartPanels();
            return false;
        });
    }

    @Override
    public void start() {
        EkgApplicationController appController = Lookup.lookup(EkgApplicationController.class);
        MenuItem newChart = new MenuItem("New Chart");
        newChart.setMnemonicParsing(false);
        newChart.setOnAction(e -> Lookup.lookup(ChartPanelManager.class).createNewChartPanel());
        appController.addWindowMenuItem(newChart);
    }

    @Override
    public void stop() {
        // nothing to end
    }
}

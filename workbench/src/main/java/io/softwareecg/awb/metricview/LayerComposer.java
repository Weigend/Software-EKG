package io.softwareecg.awb.metricview;

import io.softwareecg.awb.metricview.command.ChartCommand;
import io.softwareecg.awb.metricview.command.ChartCommandProcessor;

import java.util.Objects;

/** Translates metric-view layer changes into chart commands. */
public final class LayerComposer {

    private final ChartCommandProcessor commandProcessor;

    public LayerComposer(ChartCommandProcessor commandProcessor) {
        this.commandProcessor = Objects.requireNonNull(commandProcessor);
    }

    public void pushBaseToBackground(String backgroundId) {
        commandProcessor.executeObjectArgCommand(ChartCommand.PUSH_BASE_TO_BG, null, backgroundId);
    }

    public void exchangeWithBackground(String foregroundId, String displacedBackgroundId) {
        commandProcessor.executeObjectArgCommand(
                ChartCommand.BRING_BG_TO_FRONT, foregroundId, displacedBackgroundId);
    }
}

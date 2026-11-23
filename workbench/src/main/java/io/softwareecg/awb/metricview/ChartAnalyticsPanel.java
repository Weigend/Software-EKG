package io.softwareecg.awb.metricview;

import io.softwareecg.awb.uitoolkit.view.EkgView;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * This class implements the complete chart view component with
 * chart panel itself, the chart legend & actions and the
 * filter/control panel.
 *
 * @since 03.09.2018
 */
public class ChartAnalyticsPanel {

    public static final String VIEW_PREFIX = "ekg:metric:";

    private static final AtomicInteger NEXT_ID = new AtomicInteger(0);

    /**
     * private constructor because this class has to create view
     * {@link ChartAnalyticsPanel#createInstance()} method only.
     */
    private ChartAnalyticsPanel() {
        // no op
    }

    /**
     * Initialize a new chart panel view.
     *
     * @return the new chart panel view.
     * @throws IOException In case of the view can not be initialized.
     */
    public static EkgView<MetricViewController> createInstance() throws IOException {
        return createInstance(null);
    }

    /**
     * Initialize a new chart panel view with a title of its own.
     *
     * @param title the title of the view, usually the project it shows; {@code null} for the
     *              numbered title of a view that shows no project yet
     * @return the new chart panel view.
     * @throws IOException In case of the view can not be initialized.
     */
    public static EkgView<MetricViewController> createInstance(String title) throws IOException {

        int id = NEXT_ID.getAndIncrement();

        return new EkgView.Builder<MetricViewController>()
                .withId(VIEW_PREFIX + id)
                .withTitle(title == null || title.isBlank() ? "Metric (" + id + ")" : title)
                .withPos(EkgView.Position.CENTER)
                .withFile(ChartAnalyticsPanel.class.getResource("ChartPanel.fxml"))
                .withToolTipInfo("The metric viewer")
                .build();
    }
}

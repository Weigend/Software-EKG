package io.softwareecg.awb.query.model;


/**
 * todo insert comment here
 *
 * @since 31.08.2018
 */
public class ComputationSpec {

    /**
     * smoothingGranularity that controls if smoothing is enabled and how detailed it works
     */
    private SeriesSmoothingGranularity seriesSmoothingGranularity = SeriesSmoothingGranularity.AUTO;

    /**
     * specifies the smoothing type of the values (min, max, average, median).
     */
    private SeriesSmoothingType seriesSmoothingType = SeriesSmoothingType.NONE;

    /**
     * specifies if the series should be combined and if, that kind of algorithm should use
     */
    private SeriesCombineMode seriesCombineMode = SeriesCombineMode.NONE;

    /*
     * threshold per individual series
     *
     * specifies the maximum amount of data points per individual series;
     * 0 indicates, that no simplification should be started
     */
    private int threshold = 2000;

    /**
     * maximum number of series to display simultaneously in the chart.
     * Additional series beyond this limit will be truncated and the UI will indicate how many were omitted.
     */
    private int maxDisplaySeries = 50;


    public SeriesSmoothingGranularity getSeriesSmoothingGranularity() {
        return seriesSmoothingGranularity;
    }

    public void setSeriesSmoothingGranularity(SeriesSmoothingGranularity seriesSmoothingGranularity) {
        this.seriesSmoothingGranularity = seriesSmoothingGranularity;
    }

    public SeriesSmoothingType getSeriesSmoothingType() {
        return seriesSmoothingType;
    }

    public void setSeriesSmoothingType(SeriesSmoothingType seriesSmoothingType) {
        this.seriesSmoothingType = seriesSmoothingType;
    }

    public SeriesCombineMode getSeriesCombineMode() {
        return seriesCombineMode;
    }

    public void setSeriesCombineMode(SeriesCombineMode seriesCombineMode) {
        this.seriesCombineMode = seriesCombineMode;
    }

    public int getThreshold() {
        return threshold;
    }

    public void setThreshold(int threshold) {
        this.threshold = threshold;
    }

    public int getMaxDisplaySeries() {
        return maxDisplaySeries;
    }

    public void setMaxDisplaySeries(int maxDisplaySeries) {
        this.maxDisplaySeries = maxDisplaySeries;
    }
}

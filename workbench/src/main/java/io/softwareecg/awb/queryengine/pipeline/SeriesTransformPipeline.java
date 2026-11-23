package io.softwareecg.awb.queryengine.pipeline;

import io.softwareecg.awb.investigation.model.SeriesCombineMode;
import io.softwareecg.awb.investigation.model.SeriesSmoothingGranularity;
import io.softwareecg.awb.investigation.model.SeriesSmoothingType;
import io.softwareecg.awb.queryengine.pipeline.combine.CombinerFactory;
import io.softwareecg.awb.queryengine.pipeline.combine.TimeSeriesCombiner;
import io.softwareecg.awb.queryengine.pipeline.simplification.SimplificationService;
import io.softwareecg.awb.queryengine.pipeline.simplification.algorithms.ValueChangeSimplificationService;
import io.softwareecg.awb.queryengine.pipeline.smoothing.SmootherFactory;
import io.softwareecg.awb.queryengine.pipeline.smoothing.TimeSeriesSmoother;
import io.softwareecg.awb.runtime.logging.EkgLogger;
import io.softwareecg.awb.timeseries.TimeSeries;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.apache.commons.lang3.time.StopWatch;
import org.slf4j.Logger;

import java.util.Collections;
import java.util.List;

import static io.softwareecg.awb.investigation.model.SeriesCombineMode.CONCAT;

/** Applies the ordered, repository-independent transformations to queried time series. */
@Singleton
public final class SeriesTransformPipeline {

    private static final Logger LOGGER = EkgLogger.get();

    private final SimplificationService simplificationService;
    private final SmootherFactory smootherFactory;

    @Inject
    public SeriesTransformPipeline(SimplificationService simplificationService, SmootherFactory smootherFactory) {
        this.simplificationService = simplificationService;
        this.smootherFactory = smootherFactory;
    }

    public List<TimeSeries> keepValueChanges(List<TimeSeries> series) {
        return new ValueChangeSimplificationService().simplify(series);
    }

    public List<TimeSeries> simplify(List<TimeSeries> series, int valueLimit) {
        StopWatch stopwatch = StopWatch.createStarted();
        List<TimeSeries> simplified = simplificationService.simplify(series, valueLimit);
        LOGGER.info("Simplify {} time series in {}", series.size(), stopwatch);
        return simplified;
    }

    public void smooth(List<TimeSeries> series, SeriesSmoothingGranularity granularity,
                       SeriesSmoothingType type) {
        if (type == SeriesSmoothingType.NONE || series.isEmpty()) {
            return;
        }
        TimeSeriesSmoother smoother = smootherFactory.resolveSmoother(type, granularity);
        series.parallelStream().forEach(timeSeries ->
                timeSeries.setSortedValues(smoother.computeSmoothing(timeSeries.getValues())));
    }

    public List<TimeSeries> combine(String metricName, List<TimeSeries> series, SeriesCombineMode mode) {
        if (mode == SeriesCombineMode.NONE || mode == CONCAT || series.isEmpty()) {
            return series;
        }
        StopWatch stopwatch = StopWatch.createStarted();
        LOGGER.info("Begin combining time series list with {} elements.", series.size());
        TimeSeriesCombiner combiner = CombinerFactory.resolveCombiner(mode);
        List<TimeSeries> result = Collections.singletonList(combiner.combine(metricName, series));
        LOGGER.info("Finished combining {} time series to a single one in {}.", series.size(), stopwatch);
        return result;
    }
}

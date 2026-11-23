package io.softwareecg.awb.explorer.metric.builders;

import io.softwareecg.awb.uitoolkit.tree.api.ProgressNotifier;
import io.softwareecg.awb.explorer.items.ItemBuilder;
import io.softwareecg.awb.explorer.items.RepositoryBaseItem;
import io.softwareecg.awb.query.MetricDataAccessService;
import io.softwareecg.awb.timeseries.dimension.Metric;
import io.softwareecg.awb.query.model.MetricQuery;
import io.softwareecg.awb.query.model.QuerySpec;
import io.softwareecg.awb.explorer.metric.api.FilterContext;
import io.softwareecg.awb.explorer.metric.items.series.MetricItem;
import io.softwareecg.awb.repository.port.TimeSeriesImportSource;
import io.softwareecg.awb.repository.SourceRepositoryService;
import io.softwareecg.awb.runtime.logging.EkgLogger;
import io.softwareecg.awb.runtime.discovery.ServiceDiscovery;
import io.softwareecg.awb.timeseries.Counter;
import org.apache.commons.lang3.time.StopWatch;
import org.slf4j.Logger;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

/**
 * TreeItem builder that produces TreeItem instances of type MetricItem
 *
 * @since 19.08.2018
 */
public class MetricItemBuilder<T extends RepositoryBaseItem> extends AbstractConditionalItemBuilder<T> {

    private static final Logger LOGGER = EkgLogger.get();

    @Override
    protected boolean isValidParentForChilds(T parentItem, FilterContext parentFilterContext) {
        return true;// parentFilterContext.getQueryParameter().getProcess() == Process.DEFAULT;
    }

    @Override
    protected List<MetricItem> getChildItemsFor(T item, FilterContext context, ProgressNotifier notifier) {

        MetricDataAccessService metricDataAccess = item.getRepository().getBoundedService(MetricDataAccessService.class);

        List<Metric> metrics = metricDataAccess.getMetricsNames(new MetricQuery(context.getQueryParameter()));

        StopWatch stopWatch = StopWatch.createStarted();
        try {
            return ItemBuilder.getItems(
                    metrics.parallelStream(),
                    new MetricMapper(context, item),
                    notifier, metrics.size(),
                    "Metrics");
        } finally {
            LOGGER.info("** Metrics loaded in: " + stopWatch.getTime(TimeUnit.MILLISECONDS) + "ms");
        }

    }



    private static class MetricMapper implements Function<Metric, MetricItem> {

        private final FilterContext parentFilterContext;

        private final QuerySpec queryParams;

        private final RepositoryBaseItem parentItem;

        private final TimeSeriesImportSource importerSourceRepository;

        public MetricMapper(FilterContext filterContext, RepositoryBaseItem parentItem) {
            this.parentFilterContext = filterContext;
            this.parentItem = parentItem;
            this.queryParams = filterContext.getQueryParameter();

            SourceRepositoryService service = ServiceDiscovery.lookup(SourceRepositoryService.class, parentItem.getRepository());
            importerSourceRepository = service.queryImportSourceRepository(queryParams.getProjectName());
        }

        @Override
        public MetricItem apply(Metric metric) {

            QuerySpec query = new QuerySpec.Builder<>(queryParams)
                    .withMetric(metric)
                    .withImporterSourceRepository(importerSourceRepository)
                    .build();

            return new MetricItem(
                    new Counter(metric.getName()),
                    parentFilterContext.cloneWith(query),
                    query,
                    parentItem.getRepository()
            );
        }
    }
}
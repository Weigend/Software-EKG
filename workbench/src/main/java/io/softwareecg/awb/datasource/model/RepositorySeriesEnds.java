package io.softwareecg.awb.datasource.model;

import io.softwareecg.awb.timeseries.port.TimeSeriesRepository;
import io.softwareecg.awb.timeseries.port.query.expr.Expression;
import io.softwareecg.awb.timeseries.port.query.search.SearchParams;
import io.softwareecg.awb.timeseries.port.query.search.SearchResult;
import io.softwareecg.awb.timeseries.port.query.search.SortField;
import io.softwareecg.awb.timeseries.schema.DocumentType;
import io.softwareecg.awb.timeseries.schema.EkgSchemaField;
import io.softwareecg.awb.repository.api.EkgRepository;
import io.softwareecg.awb.repository.model.BinaryTimeSeries;
import io.softwareecg.awb.timeseries.port.RepositoryException;

import java.util.ArrayList;
import java.util.List;

import static io.softwareecg.awb.timeseries.port.query.expr.ExprFactory.exactFilter;

/**
 * Reads the end of a series from the repository of the analyses: the newest stored part of
 * exactly this series - same systems, same process, same metric - says where it ends.
 */
public final class RepositorySeriesEnds implements SeriesEnds {

    private final EkgRepository repository;

    public RepositorySeriesEnds(EkgRepository repository) {
        this.repository = repository;
    }

    @Override
    public long endOf(String projectName, String measurement, SeriesDimensions series) throws RepositoryException {
        List<Expression> filters = new ArrayList<>();
        filters.add(exactFilter(EkgSchemaField.DOC_TYPE, DocumentType.TIME_SERIES.toString()));
        filters.add(exactFilter(EkgSchemaField.PROJECT_NAME, projectName));
        filters.add(exactFilter(EkgSchemaField.TS_MEASUREMENT, measurement));
        filters.add(exactFilter(EkgSchemaField.TS_HOST_GROUP_NAME, series.hostGroup()));
        filters.add(exactFilter(EkgSchemaField.TS_HOST_NAME, series.host()));
        filters.add(exactFilter(EkgSchemaField.TS_PROCESS_GROUP_NAME, series.processGroup()));
        filters.add(exactFilter(EkgSchemaField.TS_PROCESS_NAME, series.process()));
        filters.add(exactFilter(EkgSchemaField.TS_METRIC_GROUP, series.metricGroup()));
        filters.add(exactFilter(EkgSchemaField.TS_METRIC_NAME, series.metric()));

        SearchParams search = new SearchParams();
        search.withFilterQueries(filters.toArray(new Expression[0]));
        search.withMaxRows(1);
        search.withSortField(EkgSchemaField.TS_STOP, SortField.SortMode.DESC);

        TimeSeriesRepository client = repository.getRepositoryClient();
        SearchResult<BinaryTimeSeries> result = client.search(BinaryTimeSeries.class, search);
        if (result.queryStatus().getNumberOfHits() == 0 || result.rows().isEmpty()) {
            return 0;
        }
        return result.rows().get(0).getEnd();
    }
}

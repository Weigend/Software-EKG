package io.softwareecg.awb.repository.service;

import io.softwareecg.awb.repository.api.EkgRepository;
import io.softwareecg.awb.repository.port.TimeSeriesRepository;
import io.softwareecg.awb.repository.port.search.expr.Expression;
import io.softwareecg.awb.repository.port.search.SearchParams;
import io.softwareecg.awb.repository.port.search.SearchResult;
import io.softwareecg.awb.repository.port.search.SortField;
import io.softwareecg.awb.repository.schema.DocumentType;
import io.softwareecg.awb.repository.schema.EkgSchemaField;
import io.softwareecg.awb.repository.model.BinaryTimeSeries;
import io.softwareecg.awb.repository.api.Repository;
import io.softwareecg.awb.repository.port.RepositoryException;
import io.softwareecg.awb.repository.port.TimeSeriesWriteService;
import io.softwareecg.awb.timeseries.codec.DateValuePairCodec;
import io.softwareecg.awb.timeseries.TimeSeries;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static io.softwareecg.awb.repository.port.search.expr.ExprFactory.exactFilter;
import static io.softwareecg.awb.repository.schema.EkgSchemaField.*;

/**
 * todo insert comment here
 *
 * @since 31.01.2020
 */
@Singleton
public class TimeSeriesWriteServiceImpl implements TimeSeriesWriteService {

    private TimeSeriesRepository repositoryClient;

    private Repository repository;

    /**
     * Default constructor used by Avaje for the singleton bean (acts as
     * a class-discovery template for {@link io.softwareecg.awb.runtime.discovery.ServiceDiscovery},
     * which then creates per-call instances via the {@link Repository} ctor below).
     */
    @Inject
    public TimeSeriesWriteServiceImpl() {
        // NoOp
    }

    public TimeSeriesWriteServiceImpl(Repository repository) {
        if (!(repository instanceof EkgRepository)) {
            throw new IllegalArgumentException("The given repository instance '" + repository.getRepositoryName()
                    + "' isn't supported by the EKG AWB TimeSeriesWriteService");
        }

        this.repository = repository;
        this.repositoryClient = ((EkgRepository) repository).getRepositoryClient();
    }

    @Override
    public String getRepositoryId() {
        return repository.getId();
    }

    @Override
    public long resolveLastUpdate(TimeSeries seriesWithoutValues) throws RepositoryException {

        // prepare time series query
        SearchParams searchParams = new SearchParams();
        List<Expression> filterExpressions = new ArrayList<>();


        // static parts that must always match
        filterExpressions.add(exactFilter(DOC_TYPE, DocumentType.TIME_SERIES.toString()));
        filterExpressions.add(exactFilter(PROJECT_NAME, seriesWithoutValues.getProject()));

        // the facet filter for the different dimensions
        addWildcardExpressionIfNotNull(filterExpressions, TS_HOST_GROUP_NAME, seriesWithoutValues.getHostGroup());
        addWildcardExpressionIfNotNull(filterExpressions, TS_HOST_NAME, seriesWithoutValues.getHost());
        addWildcardExpressionIfNotNull(filterExpressions, TS_NAMESPACE_NAME, seriesWithoutValues.getNamespace());
        addWildcardExpressionIfNotNull(filterExpressions, TS_SERVICE_NAME, seriesWithoutValues.getService());
        addWildcardExpressionIfNotNull(filterExpressions, TS_POD_NAME, seriesWithoutValues.getPod());
        addWildcardExpressionIfNotNull(filterExpressions, TS_CONTAINER_NAME, seriesWithoutValues.getContainer());
        addWildcardExpressionIfNotNull(filterExpressions, TS_PROCESS_NAME, seriesWithoutValues.getProcess());
        addWildcardExpressionIfNotNull(filterExpressions, TS_MEASUREMENT, seriesWithoutValues.getMeasurement());
        addWildcardExpressionIfNotNull(filterExpressions, TS_METRIC_GROUP, seriesWithoutValues.getMetricGroup());


        searchParams.withFilterQueries(filterExpressions.toArray(new Expression[0]));
        searchParams.withMaxRows(1);
        searchParams.withSortField(TS_STOP, SortField.SortMode.DESC);

        // fetch time series with filter query as export stream
        SearchResult<BinaryTimeSeries> searchResult = repositoryClient.search(BinaryTimeSeries.class, searchParams);
        long totalHits = searchResult.queryStatus().getNumberOfHits();

        if (totalHits == 0) {
            return -1;
        }

        return searchResult.rows().get(0).getEnd();
    }

    private void addWildcardExpressionIfNotNull(List<Expression> expressions, EkgSchemaField searchField, String filterExp) {
        if (StringUtils.isNotBlank(filterExp)) {
            expressions.add(exactFilter(searchField, filterExp));
        }
    }

    @Override
    public void add(TimeSeries timeSeries) throws RepositoryException {
        repositoryClient.add(mapToBinarySeries(timeSeries));
    }

    @Override
    public void add(Stream<TimeSeries> timeSeriesStream) throws RepositoryException {
        repositoryClient.add(timeSeriesStream.map(TimeSeriesWriteServiceImpl::mapToBinarySeries).iterator());
    }

    @Override
    public void add(Iterable<TimeSeries> series) throws RepositoryException {
        add(StreamSupport.stream(series.spliterator(), false));
    }

    @Override
    public void commit() throws RepositoryException {
        repositoryClient.commit();
    }

    private static BinaryTimeSeries mapToBinarySeries(TimeSeries timeSeries) {
        if (timeSeries == null) {
            throw new IllegalArgumentException("Null was given as TimeSeries");
        }

        BinaryTimeSeries binaryTimeSeries = new BinaryTimeSeries();

        // physical metric filter attributes
        binaryTimeSeries.setHost(timeSeries.getHost());
        binaryTimeSeries.setHostGroup(timeSeries.getHostGroup());

        // logical metric filter attributes
        binaryTimeSeries.setNamespace(timeSeries.getNamespace());
        binaryTimeSeries.setService(timeSeries.getService());
        binaryTimeSeries.setPod(timeSeries.getPod());
        binaryTimeSeries.setContainer(timeSeries.getContainer());

        // common metric filter attributes
        binaryTimeSeries.setMeasurement(timeSeries.getMeasurement());
        binaryTimeSeries.setProcess(timeSeries.getProcess());
        binaryTimeSeries.setProcessGroup(timeSeries.getProcessGroup());
        binaryTimeSeries.setMetric(timeSeries.getMetricName());
        binaryTimeSeries.setMetricGroup(timeSeries.getMetricGroup());
        binaryTimeSeries.setStart(timeSeries.getStartDate());
        binaryTimeSeries.setEnd(timeSeries.getEndDate());
        binaryTimeSeries.setProjectName(timeSeries.getProject());

        // other
        binaryTimeSeries.setRemoteSeriesKey(timeSeries.getRemoteSeriesKey());
        binaryTimeSeries.setData(DateValuePairCodec.compressAndEncodeDataPlainBytes(timeSeries.getValues()));

        return binaryTimeSeries;
    }
}

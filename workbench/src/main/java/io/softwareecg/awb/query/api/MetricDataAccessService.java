package io.softwareecg.awb.query.api;

import io.softwareecg.awb.timeseries.dimension.Process;
import io.softwareecg.awb.timeseries.dimension.logical.Container;
import io.softwareecg.awb.timeseries.dimension.logical.Namespace;
import io.softwareecg.awb.timeseries.dimension.logical.Pod;
import io.softwareecg.awb.timeseries.dimension.logical.Service;
import io.softwareecg.awb.timeseries.dimension.physical.Host;
import io.softwareecg.awb.timeseries.dimension.physical.HostGroup;
import io.softwareecg.awb.query.model.MetricQuery;
import io.softwareecg.awb.query.model.TimeSeriesQuery;
import io.softwareecg.awb.query.model.TimeSeriesQueryResponse;
import io.softwareecg.awb.repository.api.RepositoryException;

import java.util.List;

import io.softwareecg.awb.timeseries.dimension.Measurement;
import io.softwareecg.awb.timeseries.dimension.Metric;
import io.softwareecg.awb.timeseries.dimension.MetricGroup;
import io.softwareecg.awb.timeseries.dimension.ProcessGroup;
/**
 * Interface that represents services that provide the access to
 * time series and it metadata.
 * This interface has two groups of methods:
 * - one to query time series data (mass data) with paging/cursor function
 * - a second with some methods query facet data about the time series meta data (filter data/labels)
 *
 * @since 18.08.2018
 */
public interface MetricDataAccessService {

    /**
     * Fetches time series data from persistence storage that matches to the specified filters in
     * the given query and post filter all values in the series that are not inside an (optional)
     * specified time interval
     *
     * @param query a query instance that defines the filter parameters for time series search
     * @return a response with 0 to n time series data
     */
    TimeSeriesQueryResponse queryTimeSeriesData(TimeSeriesQuery query) throws RepositoryException ;

    //==================================================================================================================
    // facet methods for classic / physical time series filter dimensions
    //==================================================================================================================

    /**
     * Fetches and returns the facet data for host groups / cluster.
     * The service method will respect the specified query filters and proceed the query
     * with all filter except filter restrictions of the HostGroup filter dimension self.
     * This will ensure that the facet call don't restricts itself in selection
     * other filter values for this dimension.
     *
     * @param query the filter query that will used to find the right set of facet that matches to the filter set.
     * @return the facets based on the given query in alphabetical order
     */
    List<HostGroup> getHostGroups(MetricQuery query);


    /**
     * Fetches and returns the facet data for the hosts / physical/vm nodes.
     * The service method will respect the specified query filters and proceed the query
     * with all filter except filter restrictions of the Host filter dimension self.
     * This will ensure that the facet call don't restricts itself in selection
     * other filter values for this dimension.
     *
     * @param query the filter query that will used to find the right set of facet that matches to the filter set.
     * @return the facets based on the given query in alphabetical order
     */
    List<Host> getHosts(MetricQuery query);


    //==================================================================================================================
    // facet methods for cloud native / logical time series filter dimensions
    //==================================================================================================================

    /**
     * Fetches and returns the facet data for the CloudPlatform namespace / OpenShift project.
     * The service method will respect the specified query filters and proceed the query
     * with all filter except filter restrictions of the namespace filter dimension self.
     * This will ensure that the facet call don't restricts itself in selection
     * other filter values for this dimension.
     *
     * @param query the filter query that will used to find the right set of facet that matches to the filter set.
     * @return the facets based on the given query in alphabetical order
     */
    List<Namespace> getNamespaces(MetricQuery query);

    /**
     * Fetches and returns the facet data for the CloudPlatform services.
     * The implementation will respect the specified query filters and proceed the query
     * with all filter except filter restrictions of the service filter dimension self.
     * This will ensure that the facet call don't restricts itself in selection
     * other filter values for this dimension.
     *
     * @param query the filter query that will used to find the right set of facet that matches to the filter set.
     * @return the facets based on the given query in alphabetical order
     */
    List<Service> getServices(MetricQuery query);

    /**
     * Fetches and returns the facet data for pods (deployment units of Kubernetes/OpenShift).
     * The implementation will respect the specified query filters and proceed the query
     * with all filter except filter restrictions of the pod filter dimension self.
     * This will ensure that the facet call don't restricts itself in selection
     * other filter values for this dimension.
     *
     * @param query the filter query that will used to find the right set of facet that matches to the filter set.
     * @return the facets based on the given query in alphabetical order
     */
    List<Pod> getPods(MetricQuery query);

    /**
     * Fetches and returns the facet data for the container that are parts of pods.
     * The implementation will respect the specified query filters and proceed the query
     * with all filter except filter restrictions of the container filter dimension self.
     * This will ensure that the facet call don't restricts itself in selection
     * other filter values for this dimension.
     *
     * @param query the filter query that will used to find the right set of facet that matches to the filter set.
     * @return the facets based on the given query in alphabetical order
     */
    List<Container> getContainers(MetricQuery query);


    //==================================================================================================================
    //
    //==================================================================================================================

    long getAmountMeasuredPointsInProject(String projectName);

    /**
     * Returns a list of processes.
     *
     * @param query
     * @return a List of processes.
     */
    List<Process> getProcesses(MetricQuery query);

    /**
     * Returns a list of process groups.
     *
     * @param query the filter query that will used to find the right set of facet that matches to the filter set.
     * @return a List of process groups. The list can be empty but not null.
     */
    List<ProcessGroup> getProcessGroups(MetricQuery query);

    /**
     * Return all available series series.
     * @param query
     * @return a list of measurements. The list can be empty but not null.
     */
    List<Measurement> getMeasurements(MetricQuery query);

    /**
     *
     *
     * @param query
     * @return
     */
    List<MetricGroup> getMetricGroups(MetricQuery query);



    List<Metric> getMetricsNames(MetricQuery metricQuery);
}

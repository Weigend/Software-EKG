package io.softwareecg.awb.query.api;

import io.softwareecg.awb.timeseries.dimension.Process;
import io.softwareecg.awb.timeseries.dimension.logical.Container;
import io.softwareecg.awb.timeseries.dimension.logical.Namespace;
import io.softwareecg.awb.timeseries.dimension.logical.Pod;
import io.softwareecg.awb.timeseries.dimension.logical.Service;
import io.softwareecg.awb.timeseries.dimension.physical.Host;
import io.softwareecg.awb.timeseries.dimension.physical.HostGroup;

import java.util.List;

import io.softwareecg.awb.timeseries.dimension.Measurement;
import io.softwareecg.awb.timeseries.dimension.Metric;
import io.softwareecg.awb.timeseries.dimension.MetricGroup;
import io.softwareecg.awb.timeseries.dimension.ProcessGroup;
/**
 * What a repository holds: the values the dimensions of the time series have. The series themselves
 * are queried with {@link MetricQueryService}.
 *
 * @since 18.08.2018
 */
public interface MetricDataAccessService {

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
    List<HostGroup> getHostGroups(QuerySpec query);


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
    List<Host> getHosts(QuerySpec query);


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
    List<Namespace> getNamespaces(QuerySpec query);

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
    List<Service> getServices(QuerySpec query);

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
    List<Pod> getPods(QuerySpec query);

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
    List<Container> getContainers(QuerySpec query);


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
    List<Process> getProcesses(QuerySpec query);

    /**
     * Returns a list of process groups.
     *
     * @param query the filter query that will used to find the right set of facet that matches to the filter set.
     * @return a List of process groups. The list can be empty but not null.
     */
    List<ProcessGroup> getProcessGroups(QuerySpec query);

    /**
     * Return all available series series.
     * @param query
     * @return a list of measurements. The list can be empty but not null.
     */
    List<Measurement> getMeasurements(QuerySpec query);

    /**
     *
     *
     * @param query
     * @return
     */
    List<MetricGroup> getMetricGroups(QuerySpec query);



    List<Metric> getMetricsNames(QuerySpec metricQuery);
}

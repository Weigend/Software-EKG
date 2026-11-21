//______________________________________________________________________________
//
//          Project:    Software EKG
//______________________________________________________________________________
//
//         Author:      Weigend AM 2009 - 2014
//______________________________________________________________________________
//
// Notice: This piece of software was created, designed and implemented by
// experienced craftsmen and innovators in Munich, Germany.
// Changes should be done with respect to the original design.
//______________________________________________________________________________

package io.softwareecg.collector.source.ekgjolokia;

import io.softwareecg.collector.common.CollectorProperties;
import io.softwareecg.collector.common.LoggerFactory;
import io.softwareecg.collector.config.CounterDescriptor;
import io.softwareecg.collector.dt.MetricDefinition;
import io.softwareecg.collector.source.*;
import io.softwareecg.collector.source.AbstractCounterSource;
import org.jolokia.client.J4pClient;
import org.jolokia.client.exception.J4pException;
import org.jolokia.client.request.J4pListRequest;
import org.jolokia.client.request.J4pListResponse;
import org.jolokia.client.request.J4pReadRequest;
import org.jolokia.client.request.J4pReadResponse;
import org.json.simple.JSONArray;
import org.slf4j.Logger;

import javax.management.MalformedObjectNameException;
import java.util.*;
import java.util.regex.Pattern;


/**
 * Access to Jolokia-EGK-Edition via rest services.
 * The Jolokia-EGK-Edition is an extended Jolokia version for Software-EKG.
 * Based on an extended Jolokia version for Software-EKG.
 *
 */
public class EkgJolokiaPerformanceCounterAccess extends AbstractCounterSource implements CounterWildcardSupport {

    private static final String EKG_MODE_FLAG = "mode=ekg";

    /**
     * Logger of the EkgJolokiaPerformanceCounterAccess class
     */
    private static final Logger LOGGER = LoggerFactory.create();

    /**
     * if false we disable the bulk query and use single access.
     */
    private static final boolean USE_BULK_QUERY = CollectorProperties.isBulkQuery();

    /* (non-Javadoc)
     * @see io.softwareecg.collector.source.AbstractCounterSource#querySingleCounter(CounterDescriptor, MetricDefinition)
     */
    @Override
    public double querySingleCounter(CounterDescriptor descriptor, MetricDefinition metricDefinition) {
        try {
            J4pReadResponse response = JolokiaConnectionManager.getConnection(descriptor)
                    .execute(buildReadRequest(metricDefinition), JolokiaUtil.getDefaultOptions());

            return JolokiaUtil.handleResponseValue(response);
        } catch (Exception e) {
            return -1;
        }
    }

    /* (non-Javadoc)
     * @see io.softwareecg.collector.source.CounterWildcardSupport#replaceWildcardCounter(CounterDescriptor)
     */
    @Override
    public ExpandWildcardResponse replaceWildcardCounter(CounterDescriptor descriptor) {

        synchronized (EkgJolokiaPerformanceCounterAccess.class) {

            //If a colon occurs two times, and if the second one is escaped
            String onlyValidMBeans = "(^MXBean\\(.*:.*=\".*:.*\"\\).*)";

            Pattern pattern = Pattern.compile(onlyValidMBeans);

            Set<MetricDefinition> expandedMetricDefinition = new HashSet<>();
            J4pClient jolokiaConnection = JolokiaConnectionManager.getConnection(descriptor);
            MetricDefinition[] originalMetricDefinition = descriptor.getMetricDefinitions();

            Collection<MetricDefinition> countersToExpand = new ArrayList<>();

            for (MetricDefinition metricDefinition : originalMetricDefinition) {
                if (metricDefinition.useWildcard()) {
                    countersToExpand.add(metricDefinition);
                } else {
                    expandedMetricDefinition.add(metricDefinition);
                }
            }

            //noinspection CatchMayIgnoreException
            try {
                expandedMetricDefinition.addAll(expandCounter(jolokiaConnection, countersToExpand, pattern));
            } catch (Exception ignored) {
                LOGGER.error("Problem during replacing", ignored);
                return new ExpandWildcardResponse(ExpandWildcardResultType.ERROR);
            }

            return new ExpandWildcardResponse(expandedMetricDefinition.toArray(new MetricDefinition[0]));
        }
    }

    /* (non-Javadoc)
     * @see io.softwareecg.collector.source.AbstractCounterSource#queryCounter(CounterDescriptor)
     */
    @Override
    public CounterSeries queryCounter(CounterDescriptor descriptor) {

        if (!USE_BULK_QUERY) {
            return super.queryCounter(descriptor);
        }

        // build request list
        List<J4pReadRequest> requests = new ArrayList<>();
        for (MetricDefinition metricDefinition : descriptor.getMetricDefinitions()) {
            J4pReadRequest request = buildReadRequest(metricDefinition);
            if (request != null) {
                requests.add(request);
            }
        }

        try {
            List<J4pReadResponse> responses = JolokiaConnectionManager.getConnection(descriptor)
                    .execute(requests, JolokiaUtil.getDefaultOptions());
            // Handle response
            List<Double> results = new ArrayList<>();
            for (J4pReadResponse response : responses) {
                results.add(Double.parseDouble(response.getValue().toString()));
            }

            return new CounterSeries(results);

        } catch (J4pException e) {
            // its normal when the server is restarted to get exceptions. Then we fill -1.
            List<Double> results = new ArrayList<>();
            for (int i = 0; i < descriptor.getMetricDefinitions().length; i++) {
                results.add(-1d);
            }
            return new CounterSeries(results);
        }
    }

    @Override
    public String getId() {
        return "ekgjolokia";
    }

    /**
     * Expand counter.
     *
     * @param connection       the con
     * @param countersToExpand a collection of wildcard counters
     * @return the collection<? extends counter>
     */
    private Collection<? extends MetricDefinition> expandCounter(J4pClient connection, Collection<MetricDefinition> countersToExpand, Pattern pattern) {

        List<J4pListRequest> requests = new ArrayList<>();
        Set<MetricDefinition> expandedMetricDefinition = new HashSet<>();

        for (MetricDefinition metricDefinitionToExpand : countersToExpand) {
            List<String> argList = new ArrayList<>();
            argList.add(EKG_MODE_FLAG);
            argList.add(metricDefinitionToExpand.getDefinition());
            requests.add(new J4pListRequest(argList));
        }

        try {
            List<J4pListResponse> responses = connection.execute(requests, JolokiaUtil.getDefaultOptions());
            for (J4pListResponse response : responses) {
                // results.add(handleResponseValue(response));
                LOGGER.debug(response.toString());
                JSONArray counterNameArray = response.getValue();

                for (Object counterNameObj : counterNameArray) {
                    String counterName = counterNameObj.toString();
                    //if the pattern does no match
                    if (!pattern.matcher(counterName).matches()) {
                        expandedMetricDefinition.add(new MetricDefinition(counterName, null));
                    }
                }
            }
        } catch (J4pException e) {
            throw new IllegalStateException(e);
        }

        return expandedMetricDefinition;
    }


    /**
     * Build the Jolokia request for an ekg metricDefinition.
     *
     * @param metricDefinition The metricDefinition
     * @return The generated request
     */
    private J4pReadRequest buildReadRequest(MetricDefinition metricDefinition) {

        // Only process on full metricDefinition names
        if (metricDefinition.useWildcard()) {
            return null;
        }

        J4pReadRequest request;
        try {
            request = new J4pReadRequest(metricDefinition.getDefinition(), "");
        } catch (MalformedObjectNameException e) {
            throw new IllegalStateException("Exception during build request with metricDefinition '" +
                    metricDefinition.getDefinition() + "'", e);
        }
        request.setPath(EKG_MODE_FLAG);
        return request;
    }
}

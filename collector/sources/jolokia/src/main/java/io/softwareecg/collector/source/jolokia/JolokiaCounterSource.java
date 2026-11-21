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

package io.softwareecg.collector.source.jolokia;

import io.softwareecg.collector.common.CollectorProperties;
import io.softwareecg.collector.common.LoggerFactory;
import io.softwareecg.collector.common.SleepUtil;
import io.softwareecg.collector.config.CounterDescriptor;
import io.softwareecg.collector.dt.MetricDefinition;
import io.softwareecg.collector.source.*;
import io.softwareecg.collector.source.AbstractCounterSource;
import org.apache.commons.lang3.StringUtils;
import org.jolokia.client.J4pClient;
import org.jolokia.client.exception.J4pConnectException;
import org.jolokia.client.exception.J4pException;
import org.jolokia.client.exception.J4pRemoteException;
import org.jolokia.client.request.J4pReadRequest;
import org.jolokia.client.request.J4pReadResponse;
import org.json.simple.JSONObject;
import org.slf4j.Logger;

import javax.management.InstanceNotFoundException;
import javax.management.MalformedObjectNameException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * Access to jolokia rest services.
 *
 * @since 02.05.2014
 */
public class JolokiaCounterSource extends AbstractCounterSource implements CounterWildcardSupport {

    /**
     * Our logger.
     */
    private static final Logger LOGGER = LoggerFactory.create();

    // Group 1 = ObjectName
    private static final Pattern COUNTER_NAME_PATTERN_OBJECTNAME_ONLY = Pattern.compile("([^/]+)");

    // Group 1 = ObjectName, Group 3 = attribute name
    private static final Pattern COUNTER_NAME_PATTERN_WITH_ATTR = Pattern.compile("(.*)/((.*)/)");

    // Group 1 = ObjectName, Group 2 = attribute name
    private static final Pattern COUNTER_NAME_PATTERN_WITH_ATTR_STD = Pattern.compile("(.*)/((.*))");

    // Group 1 = ObjectName, Group 3 = attribute name, Group 4 = path
    private static final Pattern COUNTER_NAME_PATTERN_WITH_PATH = Pattern.compile("(.*)/(([^/]+)/(.+))");

    /**
     * Our jolokia connection cache.
     */
    private static final Map<String, J4pClient> cache = Collections.synchronizedMap(new HashMap<>());

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
            J4pReadResponse response = getConnection(descriptor)
                    .execute(buildRequest(metricDefinition), JolokiaUtil.getDefaultOptions());

            return JolokiaUtil.handleResponseValue(response);
        }
        catch (Exception e) {
            return -1;
        }
    }

    /* (non-Javadoc)
     * @see io.softwareecg.collector.source.CounterWildcardSupport#replaceWildcardCounter(CounterDescriptor)
     */
    @Override
    public ExpandWildcardResponse replaceWildcardCounter(CounterDescriptor descriptor) {

        Set<MetricDefinition> expandedMetricDefinition = new HashSet<>();
        J4pClient con = getConnection(descriptor);
        MetricDefinition[] originalMetricDefinition = descriptor.getMetricDefinitions();

        for (MetricDefinition metricDefinition : originalMetricDefinition) {
            try {
                if (metricDefinition.useWildcard()) {
                    expandedMetricDefinition.addAll(expandCounter(con, metricDefinition));
                }
                else {
                    expandedMetricDefinition.add(metricDefinition);
                }

            // check if error occurred on remote side (at Jolokia)
            } catch (J4pRemoteException e) {

                if (InstanceNotFoundException.class.getName().equals(e.getErrorType())) {
                    LOGGER.error("Unable to expand the wildcards for metric definition '{}'. The expression is invalid.",
                            metricDefinition.getDefinition(), e);

                    return new ExpandWildcardResponse(ExpandWildcardResultType.METRIC_NOT_FOUND);

                } else {
                    LOGGER.error("Error occurred during Jolokia call expanding the wildcards.", e);
                }

                return new ExpandWildcardResponse(ExpandWildcardResultType.ERROR);

            // check if error occurred on remote side (at Jolokia)
            } catch (MalformedObjectNameException e) {
                LOGGER.error("Unable to expand the wildcards for metric definition '{}'. The expression is invalid." +
                        " It must match the MBean format 'domain:key' like 'java.lang:type=*,*'.", metricDefinition.getDefinition(), e);

                return new ExpandWildcardResponse(ExpandWildcardResultType.METRIC_DEFINITION_INVALID);

            // check Jolokia server couldn't reach at the moment
            } catch (J4pConnectException e) {
                return new ExpandWildcardResponse(ExpandWildcardResultType.METRIC_SOURCE_NOT_AVAILABLE);

            // catch all other errors
            } catch (Exception e) {
                LOGGER.error("Error occurred during Jolokia call expanding the wildcards.", e);
                return new ExpandWildcardResponse(ExpandWildcardResultType.ERROR);
            }
        }

        if (expandedMetricDefinition.isEmpty()) {
            return new ExpandWildcardResponse(ExpandWildcardResultType.METRIC_NOT_FOUND);
        }

        return new ExpandWildcardResponse(expandedMetricDefinition.toArray(new MetricDefinition[0]));
    }

    /* (non-Javadoc)
     * @see io.softwareecg.collector.source.AbstractCounterSource#queryCounter(CounterDescriptor)
     */
    @Override
    public CounterSeries queryCounter(CounterDescriptor descriptor) {

        if (!USE_BULK_QUERY) {
            return super.queryCounter(descriptor);
        }

        // build request
        List<J4pReadRequest> requests = new ArrayList<>();

        for (MetricDefinition metricDefinition : descriptor.getMetricDefinitions()) {
            try {
                J4pReadRequest request = buildRequest(metricDefinition);

                if (request != null) {
                    requests.add(request);
                }
            }
            catch (MalformedObjectNameException e) {
                throw new IllegalStateException("Malformed metricDefinition name.", e);
            }
        }

        try {
            List<J4pReadResponse> responses = getConnection(descriptor).execute(requests, JolokiaUtil.getDefaultOptions());
            // Handle response
            List<Double> results = new ArrayList<>();
            for (J4pReadResponse response : responses) {
                results.add(JolokiaUtil.handleResponseValue(response));
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
        return "jolokia";
    }

    /**
     * Expand metricDefinition.
     *
     * @param con     the con
     * @param metricDefinition the metricDefinition
     * @return the collection<? extends metricDefinition>
     * @throws MalformedObjectNameException the malformed object name exception
     * @throws J4pException                 the j4p exception
     */
    @SuppressWarnings("unchecked")
    private Collection<? extends MetricDefinition> expandCounter(J4pClient con, MetricDefinition metricDefinition)
            throws MalformedObjectNameException, J4pException {

        Set<MetricDefinition> expandedMetricDefinition = new HashSet<>();

        J4pReadRequest req = null;
        J4pReadResponse resp = null;
        try {
            req = new J4pReadRequest(metricDefinition.getDefinition(), "");
            resp = con.execute(req, JolokiaUtil.getDefaultOptions());

        } catch (J4pRemoteException e) {

            if (InstanceNotFoundException.class.getName().equals(e.getErrorType())) {
                resp = retryExpandCall(con, req, e);
            } else {
                throw new J4pException("unable to expand counter as result of an remote error.", e);
            }

        } catch (J4pConnectException e) {
            resp = retryExpandCall(con, req, e);
        }

        JSONObject value = resp.getValue();

        if (req.getObjectName().isPattern()) {
            // Response is expected as a list of beans with the counters within.
            Set<Map.Entry<String, JSONObject>> beanEntries = value.entrySet();
            for (Map.Entry<String, JSONObject> bean : beanEntries) {
                expandedMetricDefinition.addAll(getBeanCounter(bean.getKey(), bean.getValue(), metricDefinition));
            }
        }
        else {
            // Response is expected as counters within a bean
            expandedMetricDefinition.addAll(getBeanCounter(req.getObjectName().getCanonicalName(), value, metricDefinition));
        }

        return expandedMetricDefinition;
    }

    private J4pReadResponse retryExpandCall(J4pClient con, J4pReadRequest req, J4pException e) throws J4pException {

        LOGGER.error("unable to retrieve metrics via Jolokia on the first attempt. " +
                "Maybe the source isn't available at the moment. Try 60sec to reach the source.");

        J4pReadResponse resp = null;

        // if we do not reach the target on the first try, give it a second as workaround
        // for delayed initialisation inside Cloud environments
        for (int i = 0; i < 30; i++) {
            try {
                SleepUtil.sleep(2000);
                resp = con.execute(req, JolokiaUtil.getDefaultOptions());
                break;

            } catch (J4pRemoteException | J4pConnectException e1) {
                // no op
            }
        }

        if (resp == null) {
            LOGGER.error("unable to retrieve metrics from Jolokia after 60 sec.");
            throw e;
        }

        return resp;
    }


    /**
     * Gets the bean counter.
     *
     * @param beanName the bean name
     * @param bean     the bean
     * @return the bean counter
     */
    @SuppressWarnings("unchecked")
    private Collection<? extends MetricDefinition> getBeanCounter(String beanName, JSONObject bean,
                                                                  MetricDefinition orgMetricDefinition) {

        Set<MetricDefinition> expandedMetricDefinition = new HashSet<>();
        Set<Map.Entry<String, Object>> attribSet = bean.entrySet();
        for (Map.Entry<String, Object> attrib : attribSet) {

            if (attrib.getValue() instanceof Number) {
                String counterName = String.format("%s/%s/", beanName, attrib.getKey());

                postFilterAwareAddMetric(expandedMetricDefinition, counterName, orgMetricDefinition);
            }
            else if (attrib.getValue() instanceof JSONObject) {
                Set<Map.Entry<String, Object>> pathSet = ((JSONObject) attrib.getValue()).entrySet();
                for (Map.Entry<String, Object> path : pathSet) {
                    if (path.getValue() instanceof Number) {
                        String counterName = String.format("%s/%s/%s", beanName, attrib.getKey(), path.getKey());
                        postFilterAwareAddMetric(expandedMetricDefinition, counterName, orgMetricDefinition);
                    }
                }
            }
        }
        return expandedMetricDefinition;
    }


    /**
     * Get the connection to the jolokia instance.
     *
     * @param descriptor a descriptor that provides endpoint config for the counter source and the metrics to fetch
     * @return The jolokia client instance
     */
    protected J4pClient getConnection(CounterDescriptor descriptor) {

        String connectionUrl = descriptor.getProcessMetricsSource(); // source field is used here
        if (connectionUrl == null) {
            throw new IllegalStateException("\"source\" Attribute is missing in <process>. " +
                    "This is a configuration error. This attribute is the connection url string for Jolokia access.");
        }

        if (!connectionUrl.endsWith("/")) {
            LOGGER.warn("the configured connection URL '{}' for Jolokia doesn't end with a slash. This will cause errors" +
                    " if you use default setup of Jolokia on server (remote) side. Ensure this is correct!");
        }

        J4pClient connection;
        if ((connection = cache.get(connectionUrl)) == null) {
            synchronized (JolokiaCounterSource.class) {
                connection = JolokiaConnectionManager.getConnection(descriptor);
                LOGGER.info("Using Jolokia URL:" + connectionUrl);
                cache.put(connectionUrl, connection);
            }
        }
        return connection;
    }


    /**
     * Build the jolokia request for an ekg metricDefinition.
     *
     * @param metricDefinition The metricDefinition
     * @return The generated request
     * @throws MalformedObjectNameException the malformed object name exception
     */
    private J4pReadRequest buildRequest(MetricDefinition metricDefinition) throws MalformedObjectNameException {

        // Only process on full metricDefinition names
        if (metricDefinition.useWildcard()) {
            return null;
        }
        String counterName = metricDefinition.getDefinition();

        String objectName, attr = null, path = null;
        Matcher matcher;

        int firstSlashPos = StringUtils.indexOf(counterName, '/');
        int lastSlashPos = StringUtils.lastIndexOf(counterName, '/');

        if (!counterName.contains("/")) {
            matcher = COUNTER_NAME_PATTERN_OBJECTNAME_ONLY.matcher(counterName);
        } else if (firstSlashPos == lastSlashPos && lastSlashPos > 0 && lastSlashPos < counterName.length() - 1) {
            matcher = COUNTER_NAME_PATTERN_WITH_ATTR_STD.matcher(counterName);
        }else if (counterName.endsWith("/")) {
            matcher = COUNTER_NAME_PATTERN_WITH_ATTR.matcher(counterName);
        } else {
            matcher = COUNTER_NAME_PATTERN_WITH_PATH.matcher(counterName);
        }

        if (matcher.matches()) {
            objectName = matcher.group(1);

            if (matcher.groupCount() >= 3) {
                attr = matcher.group(3);
            }

            if (matcher.groupCount() >= 4) {
                path = matcher.group(4);
            }

        } else {
            throw new IllegalArgumentException("Illegal metricDefinition name: " + counterName);
        }

        J4pReadRequest req = attr == null ?  new J4pReadRequest(objectName) : new J4pReadRequest(objectName, attr);
        req.setPath(path);
        return req;
    }
}

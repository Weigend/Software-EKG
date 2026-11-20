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

package io.softwareecg.collector.source.jmx;

import io.softwareecg.collector.common.LoggerFactory;
import io.softwareecg.collector.config.CounterDescriptor;
import io.softwareecg.collector.dt.MetricDefinition;
import io.softwareecg.collector.source.CounterWildcardSupport;
import io.softwareecg.collector.source.ExpandWildcardResponse;
import io.softwareecg.collector.source.ExpandWildcardResultType;
import io.softwareecg.collector.source.common.AbstractCounterSource;
import org.slf4j.Logger;

import javax.management.*;
import javax.management.openmbean.*;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;
import java.io.IOException;
import java.util.*;

import static java.lang.Double.parseDouble;


/**
 * Access to jmx beans.
 *
 */
public class JmxPerformanceCounterSource extends AbstractCounterSource implements CounterWildcardSupport {

    /**
     * The logger used by the JmxPerformanceCounterSource to notify about errors and special events
     */
    private static final Logger LOGGER = LoggerFactory.create();

    /**
     * Our JMX connection cache.
     */
    private static final Map<String, MBeanServerConnection> cache = Collections.synchronizedMap(new HashMap<>());

    /**
     * Retry connect every 120s.
     */
    private static final int RETRY_CONNECT_WAIT_TIME_IN_MS = 120 * 1000;


    /* (non-Javadoc)
     * @see io.softwareecg.collector.source.common.AbstractCounterSource#querySingleCounter(CounterDescriptor, MetricDefinition)
     */
    @Override
    public double querySingleCounter(CounterDescriptor descriptor, MetricDefinition metricDefinition) {
        try {
            if (descriptor.getProcessMetricsSource() != null) {
                return getCounter(descriptor, metricDefinition.getDefinition());
            } else {
                throw new IllegalArgumentException("Please specify JMX service URL in source attribute");
            }
        } catch (Exception e) {
            LOGGER.debug("Can not query single metricDefinition", e);
            return -1;
        }
    }

    /**
     * Gets the counter for a jmx source.
     *
     * @param descriptor a descriptor that provides endpoint config for the counter source and the metrics to fetch
     * @param metricDefExpr the metric metricDefinitionExpr, this means the name or the matching expression of the metric.
     * @return -1 if no value can be accessed.
     * @throws JMException if an error occurs.
     */
    public double getCounter(CounterDescriptor descriptor, String metricDefExpr) throws JMException {

        if (!metricDefExpr.startsWith("MXBean")) {
            throw new IllegalStateException("Only MXBeans are supported.");
        }

        String mbeanName = metricDefExpr.substring(metricDefExpr.indexOf('(') + 1, metricDefExpr.lastIndexOf(')'));
        String mbeanAttribute = metricDefExpr.substring(metricDefExpr.lastIndexOf(')') + 2);

        Object value;
        try {
            MBeanServerConnection connection = getConnection(descriptor);
            if (mbeanAttribute.contains(".")) {
                //a composite attribute is collected (f.e. "Usage.max")
                value = readCompositeAttribute(connection, mbeanName, mbeanAttribute);
            } else {
                //a simple attribute
                value = connection.getAttribute(new ObjectName(mbeanName), mbeanAttribute);
            }
            value = value != null && value.toString().matches("[+-]?[0-9]+(\\.[0-9]+)?") ? value.toString() : "-1";
            return parseDouble(value.toString());

        } catch (RuntimeMBeanException e) {
            // can be normal for some MBean Attributes.
            // See http://220.247.227.52/jmx-console/HtmlAdaptor?action=inspectMBean&name=java.lang%3Atype%3DMemoryPool%2Cname%3DPS+Eden+Space
            LOGGER.debug("JXM MetricDefinition Source: Can not get value from MBean", e);

        } catch (IOException e) {
            LOGGER.debug("IO Problem", e);
            removeConnectionFromCache(descriptor.getProcessMetricsSource());
            try {
                Thread.sleep(RETRY_CONNECT_WAIT_TIME_IN_MS);
            } catch (InterruptedException e1) {
                LOGGER.debug("Interrupted by exception", e1);
                Thread.currentThread().interrupt();
            }
        }

        return -1;
    }

    /**
     * Helper to read composite data.
     *
     * @param connection     the connection.
     * @param mbeanName      the mbean.
     * @param mbeanAttribute the attribute (composite)
     * @return a valid string. Never null.
     */
    protected String readCompositeAttribute(MBeanServerConnection connection, String mbeanName, String mbeanAttribute) {
        String value;
        try {
            String data = getMBeanData(mbeanAttribute);
            CompositeData cd = (CompositeData) connection.getAttribute(new ObjectName(mbeanName), data);
            if (cd == null) {
                return "-1";
            }
            String key = getMBeanKey(mbeanAttribute);
            Object object = cd.get(key);
            if (object == null) {
                LOGGER.debug("Can not get object for key: " + key);
                return "-1";
            }
            value = object.toString();
        } catch (IOException | JMException e) {
            throw new IllegalStateException("No composite jmx counter: " + mbeanName + "." + mbeanAttribute, e);
        }
        return value;
    }

    /**
     * We remove the connection from our cache.
     *
     * @param jmxConnectionUrl the url.
     */

    private void removeConnectionFromCache(String jmxConnectionUrl) {
        // make sure we clear the connection if an error occours
        cache.put(jmxConnectionUrl, null);
    }

    /**
     * Gets a new jmx connection or a already used connection from the cache.
     *
     * @param descriptor a descriptor that provides endpoint config for the counter source and the metrics to fetch
     * @return a connection - never null.
     * @throws IOException if an IO error occurs.
     */
    protected static MBeanServerConnection getConnection(CounterDescriptor descriptor) throws IOException {
        if ("self".equals(descriptor.getProcessMetricsSource())) {
            JMXServiceURL url = JmxServiceUrlHelper.getLocalEkgCollectorServiceUrl();
            String jmxConnectionUrl = url.toString();
            // replace for later use
            descriptor.setProcessMetricsSource(jmxConnectionUrl);
        }
        return getMBeanServerConnection(descriptor);
    }


    /**
     * Helper to get a MBeanServerConnection.
     *
     * @param descriptor a descriptor that provides endpoint config for the counter source and the metrics to fetch
     * @return a valid connection. Never null.
     * @throws IOException if the connection can not establish.
     */
    protected static synchronized MBeanServerConnection getMBeanServerConnection(CounterDescriptor descriptor) throws IOException {

        // source field is used here
        String jmxConnectionUrl = descriptor.getProcessMetricsSource();
        if (jmxConnectionUrl == null) {
            throw new IllegalStateException("\"source\" Attribute is missing in <process>. " +
                    "This is a configuration error. This attribute is the connection url string for JMX access.");
        }

        MBeanServerConnection connection = cache.get(jmxConnectionUrl);
        if (connection == null) {
            JMXServiceURL url = new JMXServiceURL(jmxConnectionUrl);
            LOGGER.info("Using JMX Service URL: " + url);

            String userName = descriptor.getUsername();
            String password = descriptor.getPassword();

            JMXConnector connector;
            if (userName != null && password != null) {
                final Map<String, String[]> environment = new HashMap<>();
                environment.put(JMXConnector.CREDENTIALS, new String[]{userName, password});
                connector = JMXConnectorFactory.connect(url, environment);
            } else {
                connector = JMXConnectorFactory.connect(url);
            }

            LOGGER.info("Open connection to JMX MBean Server: " + connector.toString());
            connection = connector.getMBeanServerConnection();
            cache.put(jmxConnectionUrl, connection);
        }
        return connection;
    }

    /* (non-Javadoc)
     * @see io.softwareecg.collector.source.CounterWildcardSupport#replaceWildcardCounter(CollectorJobConfiguration)
     */
    @Override
    public ExpandWildcardResponse replaceWildcardCounter(CounterDescriptor descriptor) {

        MBeanServerConnection con = getServerConnectionRetry(descriptor);

        if (con == null) {
            LOGGER.error("Unable to instantiate a connection to source endpoint " + descriptor.getProcessMetricsSource());
            return new ExpandWildcardResponse(ExpandWildcardResultType.ERROR);
        }

        return expandCounter(descriptor, con);
    }

    /**
     * Helper gets a connection. This method blocks until forever if no connection can be established.
     *
     * @param descriptor a descriptor that provides endpoint config for the counter source and the metrics to fetch
     * @return a valid connection. Never null.
     */
    protected static MBeanServerConnection getServerConnectionRetry(CounterDescriptor descriptor) {
        MBeanServerConnection con = null;
        while (con == null) {
            try {
                con = getConnection(descriptor);
            } catch (IOException e) {
                LOGGER.debug("Connection problem", e);
                LOGGER.warn("Can not connect to '" + descriptor.getProcessMetricsSource() + "'. Retrying in 120s");
                try {
                    Thread.sleep(RETRY_CONNECT_WAIT_TIME_IN_MS);
                } catch (InterruptedException e1) {
                    LOGGER.warn("Interrupted. ", e1);
                    return null;
                }
            }
        }
        return con;
    }

    /**
     * Expands all counters in a given counter group.
     *
     * @param descriptor a descriptor that provides endpoint config for the counter source and the metrics to fetch
     * @param con the connection.
     * @return a set of counters. Never null.
     */
    protected ExpandWildcardResponse expandCounter(CounterDescriptor descriptor, MBeanServerConnection con) {
        Set<MetricDefinition> expandedMetricDefinition = new HashSet<>();
        MetricDefinition[] originalMetricDefinition = descriptor.getMetricDefinitions();
        for (MetricDefinition metricDefinition : originalMetricDefinition) {
            try {
                if (metricDefinition.useWildcard()) {
                    expandedMetricDefinition.addAll(expandCounter(con, metricDefinition));
                } else {
                    expandedMetricDefinition.add(metricDefinition);
                }
            } catch (@SuppressWarnings("CatchMayIgnoreException") Exception ignored) {
                LOGGER.error("Expansion problem is ignored:", ignored);
            }
        }

        if (expandedMetricDefinition.isEmpty()) {
            return new ExpandWildcardResponse(ExpandWildcardResultType.METRIC_NOT_FOUND);
        }

        return new ExpandWildcardResponse(expandedMetricDefinition.toArray(new MetricDefinition[0]));
    }

    /**
     * Expand counter.
     *
     * @param con     the con
     * @param originalMetricDefinition the counter
     * @return the collection<? extends counter>
     * @throws IOException the exception
     */
    private Collection<? extends MetricDefinition> expandCounter(MBeanServerConnection con, MetricDefinition originalMetricDefinition) throws IOException {

        Set<MetricDefinition> expandedMetricDefinition = new HashSet<>();
        List<String> mbeanAccessErrors = new ArrayList<>();
        MetricDefinition metricDefinition = new MetricDefinition(originalMetricDefinition);
        String originalCounterName = metricDefinition.getDefinition();
        if (originalCounterName.contains("*")) {
            // wildcards like w32 *abc* -> .*abc.*
            originalCounterName = originalCounterName.replace("*", ".*");
            metricDefinition.setDefinition(originalCounterName);
        }

        Set<ObjectName> names = new TreeSet<>(con.queryNames(null, null));
        for (ObjectName name : names) {
            expandObjectName(con, metricDefinition, expandedMetricDefinition, mbeanAccessErrors, name);
        }

        if (!mbeanAccessErrors.isEmpty()) {
            LOGGER.warn("MBean access errors: " + mbeanAccessErrors);
        }

        return expandedMetricDefinition;
    }


    /**
     * Helper to expand one object name.
     *
     * @param con               the connection.
     * @param metricDefinition           the metricDefinition.
     * @param expandedMetricDefinition   the set of expanded counters.
     * @param mbeanAccessErrors a list of errors. If a error happens the error will be added here.
     * @param name              a object name.
     */
    private void expandObjectName(MBeanServerConnection con, MetricDefinition metricDefinition,
                                  Set<MetricDefinition> expandedMetricDefinition, List<String> mbeanAccessErrors,
                                  ObjectName name) {

        String counterDefinition = metricDefinition.getDefinition();

        String prefix = "MXBean(" + name.getCanonicalName() + ").";
        MBeanInfo info;
        try {
            info = con.getMBeanInfo(name);
        } catch (Exception ex) {
            LOGGER.debug("Can not get MBean info", ex);
            mbeanAccessErrors.add(name.getCanonicalName());
            return;
        }
        MBeanAttributeInfo[] mbAttrInfo = info.getAttributes();
        for (MBeanAttributeInfo mBeanAttributeInfo : mbAttrInfo) {

            String newCounter = prefix + mBeanAttributeInfo.getName();
            if (!(newCounter.equals(counterDefinition) || newCounter.matches(counterDefinition))) {
                continue;
            }

            // primitive type
            if (isNumeric(mBeanAttributeInfo) || isNumericStringRepresentation(mBeanAttributeInfo, newCounter, con)) {
                postFilterAwareAddMetric(expandedMetricDefinition, newCounter, metricDefinition);
                // open mbean
            } else if (mBeanAttributeInfo instanceof OpenMBeanAttributeInfo) {
                OpenMBeanAttributeInfoSupport attrInfo = (OpenMBeanAttributeInfoSupport) mBeanAttributeInfo;
                OpenType type = attrInfo.getOpenType();
                if (type != null && isCompatible(type)) {
                    // simple type
                    expandedMetricDefinition.add(new MetricDefinition(newCounter, newCounter, null, metricDefinition.useWildcard()));
                } else {
                    // composite type
                    if (mBeanAttributeInfo.getType() != null) {
                        String[] fieldNames = mBeanAttributeInfo.getDescriptor().getFieldNames();
                        for (String fieldName : fieldNames) {
                            Object field = mBeanAttributeInfo.getDescriptor().getFieldValue(fieldName);
                            if (field instanceof CompositeType t) {
                                Collection col = t.keySet();
                                for (Object o : col) {
                                    OpenType t2 = t.getType((String) o);
                                    if (isCompatible(t2)) {
                                        String newCounterString = prefix + mBeanAttributeInfo.getName() + "." + o;
                                        if (newCounterString.equals(counterDefinition) ||
                                                newCounterString.matches(counterDefinition)) {
                                            postFilterAwareAddMetric(expandedMetricDefinition, newCounterString, metricDefinition);
                                        }
                                    } else if (t2 instanceof TabularType) {
                                        LOGGER.info("Tabular type attribute: " + name + "." + mBeanAttributeInfo.getName() + " is not supported.");

                                    } else {
                                        LOGGER.info("Unknown attribute: " + name + "." + mBeanAttributeInfo.getName());
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                LOGGER.info("Unknown attribute: " + name + "." + mBeanAttributeInfo.getName());
            }
        }
    }


    /**
     * Checks if a open type is numeric.
     *
     * @param type the type.
     * @return a numeric type.
     */
    private static boolean isCompatible(OpenType type) {
        return checkOr(new boolean[]{
                "java.lang.Long".equals(type.getTypeName()),
                "java.lang.Integer".equals(type.getTypeName()),
                "java.lang.Float".equals(type.getTypeName()),
                "java.lang.Boolean".equals(type.getTypeName()),
                "java.lang.Short".equals(type.getTypeName()),
                "java.lang.Double".equals(type.getTypeName())
        });
    }

    /**
     * Returns true if one part is true.
     *
     * @param parts a array of parts.
     * @return true if one part is true.
     */
    private static boolean checkOr(boolean[] parts) {
        for (boolean part : parts) {
            if (part) {
                return true;
            }
        }
        return false;
    }

    /**
     * Helper to check if this is numeric datatype.
     *
     * @param mBeanAttributeInfo the bean info.
     * @return true if it is readable and numeric.
     */
    private static boolean isNumeric(MBeanAttributeInfo mBeanAttributeInfo) {
        String type = mBeanAttributeInfo.getType();
        return mBeanAttributeInfo.getType() != null && mBeanAttributeInfo.isReadable() && (
                checkOr(new boolean[]{
                        "long".equals(type),
                        "double".equals(type),
                        "int".equals(type),
                        "short".equals(type),
                        "float".equals(type),
                        "boolean".equals(type)
                }));
    }

    /**
     * Helper to check if the value is numeric and in string representation
     * <p/>
     * Some code duplicate from above.
     *
     * @param mBeanAttributeInfo - the mbean attribute info
     * @param counter            - the counter
     * @param con                - the mbean connection
     * @return false if not numerice, otherwise true
     */
    protected boolean isNumericStringRepresentation(MBeanAttributeInfo mBeanAttributeInfo,
                                                    String counter, MBeanServerConnection con) {

        //Only for string mbeans
        if (!"java.lang.String".equals(mBeanAttributeInfo.getType())) {
            return false;
        }

        String value = "";
        try {
            String mbeanName = counter.substring(counter.indexOf('(') + 1, counter.lastIndexOf(')'));
            String mbeanAttribute = counter.substring(counter.lastIndexOf(')') + 2);
            //a composite data is collected (f.e. "Usage.max")
            if (mbeanAttribute.contains(".")) {
                String data = getMBeanData(mbeanAttribute);
                String key = getMBeanKey(mbeanAttribute);

                CompositeData cd = (CompositeData) con.getAttribute(new ObjectName(mbeanName), data);
                if (cd == null || cd.get(key) == null) {
                    LOGGER.debug("Can not get object for key: " + key);
                    return false;
                }
                Object object = cd.get(key);
                value = object.toString();

            } else {
                if (con.getAttribute(new ObjectName(mbeanName), mbeanAttribute) != null) {
                    value = con.getAttribute(new ObjectName(mbeanName), mbeanAttribute).toString();
                } else {
                    return false;
                }
            }
        } catch (JMException | IOException e) {
            LOGGER.warn("Could not query the mbean server for data", e);
        }
        try {
            return !"nan".equalsIgnoreCase(value);
        } catch (NumberFormatException e) {
            LOGGER.debug("Value '" + value + "' of the MBean '" + counter + "' is a string and is not numeric. We will ignore the MBean.", e);
            return false;
        }
    }

    /**
     * Split the key out of a attribute name.
     *
     * @param mbeanAttribute - the attribute name.
     * @return the key.
     */
    private String getMBeanKey(String mbeanAttribute) {
        if (mbeanAttribute.contains(".")) {
            return mbeanAttribute.substring(mbeanAttribute.indexOf('.') + 1);
        }
        return mbeanAttribute;
    }

    /**
     * Gets the data of a attribute.
     *
     * @param mbeanAttribute - the mBean attribute.
     * @return the data part.
     */
    private String getMBeanData(String mbeanAttribute) {
        if (mbeanAttribute.contains(".")) {
            return mbeanAttribute.substring(0, mbeanAttribute.indexOf('.'));
        }
        return mbeanAttribute;
    }

    @Override
    public String getId() {
        return "jmx";
    }
}
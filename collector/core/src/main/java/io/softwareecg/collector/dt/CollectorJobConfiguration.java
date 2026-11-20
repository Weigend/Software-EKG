package io.softwareecg.collector.dt;

import io.softwareecg.collector.common.LoggerFactory;
import io.softwareecg.collector.config.CounterDescriptor;
import io.softwareecg.collector.config.InvalidConfigurationException;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * todo insert comment here
 *
 * @since 09.08.2018
 */
public abstract class CollectorJobConfiguration implements Cloneable, CounterDescriptor {

    private static final Logger LOGGER = LoggerFactory.create();

    private static final Pattern SYS_ENV_SEARCH_PATTERN =
            Pattern.compile("(.*\\$\\{ENV:([^}]+)}.*)", Pattern.CASE_INSENSITIVE);

    private static final Pattern JVM_OPT_SEARCH_PATTERN =
            Pattern.compile("(.*\\$\\{JVMOPT:([^}]+)}.*)", Pattern.CASE_INSENSITIVE);

    private static final Pattern REF_SEARCH_PATTERN =
            Pattern.compile("\\$\\{REF:([^}]+)}", Pattern.CASE_INSENSITIVE);

    //================================================================================================================
    //  property section
    //================================================================================================================

    /**
     * The name of EKG workbench project the captured time series data belongs to.
     * The common usage of this value is to use it at time series storage that will read by
     * EKG directly (Solr or ElasticSearch) and must completely filled to get work.
     */
    protected String ekgProjectName;

    /**
     * The Process-Info.
     */
    protected ProcessInfo processInfo;

    /**
     * The metricDefinitions.
     */
    protected MetricDefinition[] metricDefinitions;

    /**
     * The MetricGroupInfo.
     */
    protected MetricGroupInfo metricGroupInfo;

    /**
     * The node the collector fetches the metrics from.
     * This can be a physical host (server), and physical device/gadget,
     * a virtual machine or a Pod/Container inside a cloud native platform
     */
    protected NodeInfo nodeInfo;


    //================================================================================================================
    //  different constructors for default initialization and copy use-cases
    //================================================================================================================

    /**
     * Constructs a new instance of CollectorJobConfiguration
     *
     * @param ekgProjectName the name of the EKG project used in the Software-EKG AWB to display the data
     * @param nodeInfo a simple container that store information's about the physical or visualized host.
     * @param processInfo a config bean that store process details like name or it's API endpoints to fetch metrics
     * @param metricGroupInfo a config bean that stores settings of the metric processing fetch intervals or the parser
     * @param metricDefinitions a config bean that store details about the metrics and how to process them
     */
    protected CollectorJobConfiguration(String ekgProjectName, NodeInfo nodeInfo, ProcessInfo processInfo,
                                        MetricGroupInfo metricGroupInfo, MetricDefinition[] metricDefinitions) {
        this.nodeInfo = nodeInfo;
        this.processInfo = processInfo;
        this.metricGroupInfo = metricGroupInfo;
        this.metricDefinitions = metricDefinitions;
        this.ekgProjectName = ekgProjectName;

        dissolveVarExpressions();
    }

    /**
     *
     * @param jobConfig
     */
    protected CollectorJobConfiguration(CollectorJobConfiguration jobConfig) {
        this(jobConfig.getEkgProjectName(), jobConfig.nodeInfo, jobConfig.processInfo,
                jobConfig.metricGroupInfo, jobConfig.metricDefinitions);
    }


    //================================================================================================================
    // The instance type specific CollectorJobConfiguration API that must implemented by derived config classes
    //================================================================================================================

    /**
     * Returns a bean that represents parts of the collector config that
     * specify the time series metadata without measurement.
     *
     * @return the time series header data configured in the collector config
     */
    public abstract TimeSeriesHeader getTimeSeriesHeader();

    /**
     * Creates a deep copy of the current collector configuration instance including
     * all properties that are specific in derived configs.
     *
     * @return a deep copy of this CollectorJobConfiguration
     */
    public abstract CollectorJobConfiguration copy();

    /**
     * Creates a deep copy of the current collector configuration instance except the
     * metric definitions that will taken the given ones. The deep copy also includes
     * all properties that are specific in derived configs.
     *
     * @param replacingMetricDefinitions the metric definitions that should replace ones of this instance
     *
     * @return a deep copy of this CollectorJobConfiguration but with replaced MetricDefinition's
     */
    public CollectorJobConfiguration copyWithMetrics(MetricDefinition[] replacingMetricDefinitions) {
        CollectorJobConfiguration config = this.copy();
        config.setMetricDefinitions(replacingMetricDefinitions);
        return config;
    }

    //================================================================================================================
    // the implementation of CounterDescriptor API that provides a more restricted view on the job configuration
    //================================================================================================================

    @Override
    public String getProcessLabel() {
        String label = processInfo.getProcessLabel();
        if (StringUtils.isNotBlank(label)) {
            return label;
        }
        // processLabel not set
        return processInfo.getProcessName();
    }

    @Override
    public String getProcessMetricsSource() {
        return getProcessInfo().getSource();
    }

    @Override
    public String getAdditionalArguments() {
        return getProcessInfo().getAdditionalArguments();
    }

    @Override
    public void setProcessMetricsSource(String source) {
        processInfo.setSourceEndpoint(source);
    }

    @Override
    public MetricDefinition[] getMetricDefinitions() {
        return metricDefinitions.clone();
    }

    @Override
    public String getUsername() {
        return nodeInfo.getUsername();
    }

    @Override
    public String getPassword() {
        return nodeInfo.getPassword();
    }

    @Override
    public String getNodeName() {
        return nodeInfo.getNodeName();
    }

    @Override
    public String getNodeAddress() {
        return nodeInfo.getNodeAddress();
    }

    @Override
    public String getMetricGroupId() {
        return metricGroupInfo.getMetricGroupId();
    }

    @Override
    public long getMetricFetchInterval() {
        return metricGroupInfo.getMetricFetchInterval();
    }

    @Override
    public String getNodeGroupId() {
        return nodeInfo.getNodeGroupId();
    }


    //================================================================================================================
    // extended accessor API of CollectorJobConfiguration class
    //================================================================================================================

    public String getEkgProjectName() {
        return ekgProjectName;
    }

    public String getMetricFetchAt() {
        return metricGroupInfo.getMetricFetchAt();
    }

    /**
     * Getter for property 'process'.
     *
     * @return Value for property 'process'.
     */
    public String getProcessName() {
        return processInfo.getProcessName();
    }

    public void setMetricDefinitions(MetricDefinition[] metrics) {
        metricDefinitions = metrics;
    }

    /**
     * Gets the process info.
     *
     * @return the process info.
     */
    public ProcessInfo getProcessInfo() {
        return processInfo;
    }

    /**
     * Checks if is refresh wildcards.
     *
     * @return true, if is refresh wildcards
     */
    public boolean isRefreshWildcards() {
        return metricGroupInfo.getWildcardExpandRefreshInterval() != null;
    }

    /**
     * Gets the counter group type.
     *
     * @return the counter group type
     */
    public String getCounterGroupType() {
        return metricGroupInfo.getMetricGroupType();
    }

    /**
     * Gets the refresh wildcards.
     *
     * @return the refresh wildcards
     */
    public Long getWildcardExpandRefreshInterval() {
        return metricGroupInfo.getWildcardExpandRefreshInterval();
    }


    //================================================================================================================
    // internal logic to initialize and replace config properties
    //================================================================================================================

    /**
     * Replaces all variable expressions ${xx:yy} that are stored in different string properties with
     * the values from the referenced data like system-environment properties or data of other config elements.
     */
    private void dissolveVarExpressions() {
        for (MetricDefinition metricDefinition : metricDefinitions) {
            metricDefinition.setMetricLabel(replaceVars(metricDefinition.getLabel()));
            metricDefinition.setDefinition(replaceVars(metricDefinition.getDefinition()));
        }

        processInfo.setSourceEndpoint(replaceVars(processInfo.getSource()));
        processInfo.setProcessName(replaceVars(processInfo.getProcessName()));
        processInfo.setProcessLabel(replaceVars(processInfo.getProcessLabel()));

        if (nodeInfo instanceof PodInfo) {
            ((PodInfo)nodeInfo).setNamespace(replaceVars(((PodInfo)nodeInfo).getNamespace()));
            ((PodInfo)nodeInfo).setServiceName(replaceVars(((PodInfo)nodeInfo).getServiceName()));
        }

        nodeInfo.setNodeName(replaceVars(nodeInfo.getNodeName()));
        nodeInfo.setNodeAddress(replaceVars(nodeInfo.getNodeAddress()));
        nodeInfo.setNodeGroupId(replaceVars(nodeInfo.getNodeGroupId()));
        nodeInfo.setUsername(replaceVars(nodeInfo.getUsername()));
        nodeInfo.setPassword(replaceVars(nodeInfo.getPassword()));
        nodeInfo.setNodeGroupId(replaceVars(nodeInfo.getNodeGroupId()));
    }

    /**
     * A helper method that call specific functions for each type of variable
     * in the correct order and replace the vars inside the given property with
     * the results if necessary.
     *
     * @param stringProperty the to lookup and replace vars
     * @return the property with replace vars (if vars were included)
     */
    private String replaceVars(String stringProperty) {

        if (stringProperty == null) {
            return null;
        }

        stringProperty = replaceSysEnvVars(stringProperty);
        stringProperty = replaceJvmOptVars(stringProperty);
        stringProperty = replaceRefVars(stringProperty, new ArrayList<>());
        return stringProperty;
    }

    /**
     * Specialized method that will replace reference values "${REF:xxxx}" by
     * traversing the reference graph recursive with cycle check.
     *
     * @param stringProperty the string property to replace
     * @param referenceGraph a list of string that represents the graph of variables to resolve
     *
     * @return the variable value that with resolved vars if required
     */
    protected String replaceRefVars(String stringProperty, List<String> referenceGraph) {

        Matcher refMatcher = REF_SEARCH_PATTERN.matcher(stringProperty);

        if (!refMatcher.find()) {
            return stringProperty;
        }

        String refKey = refMatcher.group(1);

        // cycle check
        if (referenceGraph.contains(refKey)) {
            StringBuilder graphBuilder = new StringBuilder();
            referenceGraph.forEach(key -> graphBuilder.append(key).append(" > "));
            referenceGraph.add("!!");
            referenceGraph.add(refKey);
            referenceGraph.add("!!");
            throw new InvalidConfigurationException("Unable to resolve reference graph a result of a reference cycle: "
                    + graphBuilder);
        }

        String envReplacement = dissolveRefReplacement(refKey);

        if (StringUtils.isBlank(envReplacement)) {
            LOGGER.error("Unable to resolve var replacement of property '" + envReplacement
                    + "' with reference value name: " + refKey);
            return stringProperty;
        }

        return replaceRefVars(refMatcher.replaceFirst(replaceRefVars(envReplacement, referenceGraph)), new ArrayList<>());
    }

    /**
     *
     * @param refKey
     * @return
     */
    protected String dissolveRefReplacement(String refKey) {

        switch (refKey) {
            case "procSource":
                return processInfo.getSource();

            case "procName":
                return processInfo.getProcessName();

            case "procLabel":
                return processInfo.getProcessLabel();

            case "hostAddress":
                return nodeInfo.getNodeAddress();

            case "podAddress":
                return nodeInfo.getNodeAddress();

            case "hostName":
                return nodeInfo.getNodeName();

            case "podName":
                return nodeInfo.getNodeName();

            case "podUsername":
                return nodeInfo.getUsername();

            case "podPassword":
                return nodeInfo.getPassword();
        }

        return null;
    }

    /**
     *
     * @param stringProperty
     * @return
     */
    private String replaceSysEnvVars(String stringProperty) {

        Matcher envMatcher = SYS_ENV_SEARCH_PATTERN.matcher(stringProperty);

        if (!envMatcher.matches()) {
            return stringProperty;
        }

        String envConfigName = envMatcher.group(2);
        String envReplacement = System.getenv(envConfigName);

        if (StringUtils.isBlank(envReplacement)) {
            envReplacement = System.getProperty(envConfigName);
        }

        if (StringUtils.isBlank(envReplacement)) {
            LOGGER.error("Unable to resolve var replacement of property '" + envReplacement
                    + "' with ENV config name: " + envConfigName);

            return stringProperty;
        }

        return envMatcher.replaceAll(envReplacement);
    }

    /**
     *
     * @param stringProperty
     * @return
     */
    private String replaceJvmOptVars(String stringProperty) {
        Matcher jvmOptMatcher = JVM_OPT_SEARCH_PATTERN.matcher(stringProperty);

        if (!jvmOptMatcher.matches()) {
            return stringProperty;
        }

        String envConfigName = jvmOptMatcher.group(1);
        String envReplacement = System.getProperty(envConfigName);

        if (StringUtils.isBlank(envReplacement)) {
            LOGGER.error("Unable to resolve var replacement of property '" + envReplacement
                    + "' with JVM_OPT config name: " + envConfigName);
            return stringProperty;
        }

        return jvmOptMatcher.replaceAll(envReplacement);
    }


}
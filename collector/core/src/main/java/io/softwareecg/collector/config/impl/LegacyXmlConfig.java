package io.softwareecg.collector.config.impl;

import io.softwareecg.collector.config.InvalidConfigurationException;
import io.softwareecg.collector.config.model.CollectorConfig;
import io.softwareecg.collector.config.model.CollectorDef;
import io.softwareecg.collector.config.model.Host;
import io.softwareecg.collector.config.model.HostGroup;
import io.softwareecg.collector.config.model.Metric;
import io.softwareecg.collector.config.model.MetricGroup;
import io.softwareecg.collector.config.model.Platform;
import io.softwareecg.collector.config.model.ProcessDef;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Reads a configuration in the XML format of the collector 6.1 to 7.0 ({@code config.xml}).
 * Configurations are written as YAML now; this is what is left of XML, so that a configuration
 * that exists can still be started and converted.
 */
final class LegacyXmlConfig {

    private LegacyXmlConfig() {
    }

    /**
     * @return {@code true} if the text is XML rather than YAML
     */
    static boolean isXml(String text) {
        return text.stripLeading().startsWith("<");
    }

    /**
     * @throws InvalidConfigurationException if the text is not a configuration of that format
     */
    static CollectorConfig read(String xml) {
        Element root = parse(xml);
        if (!"CollectorConfiguration".equals(root.getTagName())) {
            throw new InvalidConfigurationException("Not a collector configuration: the XML starts with <"
                    + root.getTagName() + "> instead of <CollectorConfiguration>.");
        }
        if (child(root, "CounterGroups") != null || child(root, "Collectors") != null) {
            throw new InvalidConfigurationException("This is a configuration of a collector before version 6.1 "
                    + "(<CounterGroups>, <Collectors>). That format is not read any more.");
        }

        Element projectName = child(root, "ekgProjectName");
        CollectorConfig config = new CollectorConfig(
                projectName == null ? null : blankToNull(projectName.getTextContent()),
                metricGroups(child(root, "Counter")),
                processGroups(child(root, "ProcessGroups")),
                hostGroups(child(root, "PhysicalEnv")),
                platform(child(root, "LogicalEnv")),
                collectors(child(root, "CollectorJobs")));
        config.validate();
        return config;
    }

    private static Map<String, MetricGroup> metricGroups(Element section) {
        Map<String, MetricGroup> groups = new LinkedHashMap<>();
        for (Element group : children(section, "MetricGroup")) {
            List<Metric> metrics = new ArrayList<>();
            for (Element metric : children(group, "MetricDefinition")) {
                String wildcard = attribute(metric, "wildcard");
                metrics.add(new Metric(metric.getTextContent(), attribute(metric, "label"),
                        attribute(metric, "regexPostFilter"), wildcard == null ? null : Boolean.valueOf(wildcard)));
            }
            groups.put(id(group), new MetricGroup(attribute(group, "type"), metrics));
        }
        return groups;
    }

    private static Map<String, List<ProcessDef>> processGroups(Element section) {
        Map<String, List<ProcessDef>> groups = new LinkedHashMap<>();
        for (Element group : children(section, "ProcessGroup")) {
            List<ProcessDef> processes = new ArrayList<>();
            for (Element process : children(group, "Process")) {
                processes.add(new ProcessDef(attribute(process, "name"), attribute(process, "label"),
                        attribute(process, "source"), attribute(process, "args")));
            }
            groups.put(id(group), processes);
        }
        return groups;
    }

    private static Map<String, HostGroup> hostGroups(Element section) {
        Map<String, HostGroup> groups = new LinkedHashMap<>();
        for (Element group : children(section, "HostGroups")) {
            List<Host> hosts = new ArrayList<>();
            for (Element host : children(group, "Host")) {
                hosts.add(new Host(attribute(host, "name"), attribute(host, "address"),
                        attribute(host, "username"), attribute(host, "password")));
            }
            groups.put(id(group), new HostGroup(attribute(group, "username"), attribute(group, "password"), hosts));
        }
        return groups;
    }

    private static Platform platform(Element section) {
        Element project = child(section, "PlatformProject");
        if (project == null) {
            return null;
        }
        Map<String, Platform.PodGroup> podGroups = new LinkedHashMap<>();
        for (Element group : children(project, "PodGroup")) {
            List<Platform.Pod> pods = new ArrayList<>();
            for (Element pod : children(group, "Pod")) {
                List<Platform.Container> containers = new ArrayList<>();
                for (Element container : children(pod, "Container")) {
                    containers.add(new Platform.Container(attribute(container, "containerId"), attribute(container, "name"),
                            attribute(container, "address"), attribute(container, "username"), attribute(container, "password")));
                }
                pods.add(new Platform.Pod(attribute(pod, "id"), attribute(pod, "name"), attribute(pod, "address"),
                        attribute(pod, "username"), attribute(pod, "password"), containers));
            }
            podGroups.put(id(group), new Platform.PodGroup(attribute(group, "service"), attribute(group, "namespace"),
                    attribute(group, "username"), attribute(group, "password"), pods));
        }
        String refresh = attribute(project, "platformRefreshStateInterval");
        return new Platform(attribute(project, "name"), attribute(project, "platformServiceAddress"),
                attribute(project, "platformServiceUser"), attribute(project, "platformServicePassword"),
                refresh == null ? null : Duration.ofMillis(Long.parseLong(refresh.strip())), podGroups);
    }

    private static List<CollectorDef> collectors(Element section) {
        List<CollectorDef> collectors = new ArrayList<>();
        for (Element collector : children(section, null)) {
            switch (collector.getTagName()) {
                case "ClassicRemoteCollector" -> collectors.add(new CollectorDef(CollectorDef.Type.REMOTE,
                        attribute(collector, "metricGroupId"), attribute(collector, "processGroupId"),
                        attribute(collector, "hostGroupName"), null,
                        millis(collector, "metricFetchInterval"), millis(collector, "metricWildcardExpandInterval"),
                        attribute(collector, "metricFetchAt")));
                case "SidecarCollector" -> collectors.add(new CollectorDef(CollectorDef.Type.SIDECAR,
                        attribute(collector, "metricGroupId"), attribute(collector, "processGroupId"),
                        null, attribute(collector, "nodeGroupId"),
                        millis(collector, "metricFetchInterval"), millis(collector, "metricWildcardExpandInterval"),
                        attribute(collector, "metricFetchAt")));
                // was accepted by the schema, but never ran
                case "CloudRemoteCollector" -> { }
                default -> throw new InvalidConfigurationException("Unknown kind of collector: <" + collector.getTagName() + ">");
            }
        }
        return collectors;
    }

    // ---------------------------------------------------------------------------------------------
    // XML
    // ---------------------------------------------------------------------------------------------

    private static Element parse(String xml) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            // a configuration needs no document type; without it no external entity is read
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            factory.setXIncludeAware(false);
            factory.setExpandEntityReferences(false);
            Document document = factory.newDocumentBuilder().parse(new InputSource(new StringReader(xml)));
            return document.getDocumentElement();
        } catch (Exception e) {
            throw new InvalidConfigurationException("The XML is not readable: " + e.getMessage(), e);
        }
    }

    private static String id(Element group) {
        String id = attribute(group, "id");
        if (id == null) {
            throw new InvalidConfigurationException("<" + group.getTagName() + "> has no id.");
        }
        return id;
    }

    private static Duration millis(Element element, String name) {
        String value = attribute(element, name);
        try {
            return value == null ? null : Duration.ofMillis(Long.parseLong(value.strip()));
        } catch (NumberFormatException e) {
            throw new InvalidConfigurationException("<" + element.getTagName() + " " + name + "=\"" + value + "\">: not a number of milliseconds.");
        }
    }

    private static String attribute(Element element, String name) {
        return element.hasAttribute(name) ? element.getAttribute(name) : null;
    }

    private static String blankToNull(String text) {
        return text == null || text.isBlank() ? null : text.strip();
    }

    private static Element child(Element parent, String name) {
        List<Element> children = children(parent, name);
        return children.isEmpty() ? null : children.get(0);
    }

    /**
     * @param name the name of the elements, or {@code null} for all
     */
    private static List<Element> children(Element parent, String name) {
        List<Element> children = new ArrayList<>();
        if (parent == null) {
            return children;
        }
        for (Node node = parent.getFirstChild(); node != null; node = node.getNextSibling()) {
            if (node instanceof Element element && (name == null || name.equals(element.getTagName()))) {
                children.add(element);
            }
        }
        return children;
    }
}

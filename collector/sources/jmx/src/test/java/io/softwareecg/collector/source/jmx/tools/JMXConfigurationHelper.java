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
package io.softwareecg.collector.source.jmx.tools;

import io.softwareecg.collector.common.Console;
import org.apache.commons.lang3.NotImplementedException;

import javax.management.*;
import javax.management.openmbean.CompositeType;
import javax.management.openmbean.OpenMBeanAttributeInfo;
import javax.management.openmbean.OpenMBeanAttributeInfoSupport;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;
import java.util.*;


/**
 * Simple standalone program to list all JMX beans of a JAVA process.
 */
public class JMXConfigurationHelper {

    /**
     * The PID.
     */
    private static int pid;

    /** List of Beans which couldn't received correctly. */
    private static final List<ObjectName> errorBeans = new ArrayList<>();

    /**
     * Our main.
     *
     * @param args not used.
     * @throws Exception if a error during jmx access happens.
     */
    public static void main(String[] args) throws Exception {


        // Create an connector client and
        // connect it to the pid connector server
        if (args.length != 1) {
            Console.getErr().println("Usage: JMXConfigurationHelper [pid] | [host:port]");
            System.exit(-1);
        }
        String urlString = null;
        try {
            // if parameter is a integer we assume that this is the PID
            pid = Integer.parseInt(args[0]);

            throw new NotImplementedException("The tool don't work anymore because sun.management.ConnectorAddressLink is forbidden to use.");

            //urlString = sun.management.ConnectorAddressLink.importFrom(pid);
        } catch (NumberFormatException e) {
            urlString = args[0];
        }

        int indexOfPort = urlString.indexOf(":");
        String hostName = urlString.substring(0, indexOfPort);
        String portNum = urlString.substring(indexOfPort + 1);
        JMXServiceURL url = new JMXServiceURL("service:jmx:rmi:///jndi/rmi://" + hostName + ":" + portNum + "/jmxrmi");
        JMXConnector jmxc = JMXConnectorFactory.connect(url, null);


        // Get an MBeanServerConnection
        //
        MBeanServerConnection mbsc = jmxc.getMBeanServerConnection();
        Console.getOut().println("\nGot MBeanServerConnection to " + urlString);

        // Get domains from MBeanServer
        //
        Console.getOut().println("\nDomains:");
        String[] domains = mbsc.getDomains();
        Arrays.sort(domains);
        for (String domain : domains) {
            Console.getOut().println("\tDomain = " + domain);
        }

        // Get MBeanServer's default domain
        //
        Console.getOut().println("\nMBeanServer default domain = " + mbsc.getDefaultDomain());

        // Get MBean count
        //
        Console.getOut().println("\nMBean count = " + mbsc.getMBeanCount());

        // Query MBean names
        //
        Console.getOut().println("\nQuery possible EKG MetricDefinition:\n");
        Set<ObjectName> names =
                new TreeSet<>(mbsc.queryNames(null, null));
        Console.getOut().println("<MetricGroup id=\"jmx-" + pid + "\" ctype=\"jmx\">");
        for (ObjectName name : names) {
            String prefix = "\t<MetricDefinition>MXBean(" + name.getCanonicalName() + ").";
            ObjectInstance o = mbsc.getObjectInstance(name);
            if (o == null) break;

            try {
                MBeanInfo info = mbsc.getMBeanInfo(o.getObjectName());
                MBeanAttributeInfo[] mbAttrInfo = info.getAttributes();

                // print all attributes
                for (MBeanAttributeInfo mBeanAttributeInfo : mbAttrInfo) {
                    try {
                        printAttribute(prefix, mBeanAttributeInfo);
                    } catch (Exception e) {
                        Console.getOut().println("Error: " + mBeanAttributeInfo);
                    }
                }

            } catch (Exception e) {
                //add failed bean to the list of bad beans
                errorBeans.add(name);
            }


        }
        Console.getOut().println("</MetricGroup>");

        //Show Error-Message if there are beans, which couldn't received via JMX
        if(!errorBeans.isEmpty()){
            Console.getOut().println("ERROR: Following beans couldn't received successfully: ");
            for(ObjectName name: errorBeans){
                Console.getOut().println("\t "+ name.getCanonicalName());
            }
        }
        // Close MBeanServer connection
        //
        jmxc.close();
    }

    /**
     * Print the JMX attribute.
     *
     * @param prefix             the bean name.
     * @param mBeanAttributeInfo the attribute.
     */
    private static void printAttribute(String prefix, MBeanAttributeInfo mBeanAttributeInfo) {
        if (isEKGCompatible(mBeanAttributeInfo)) {
            Console.getOut().println(prefix + mBeanAttributeInfo.getName() + "</MetricDefinition>");
            return;
        }
        if (mBeanAttributeInfo instanceof OpenMBeanAttributeInfo) {
            OpenMBeanAttributeInfoSupport attrInfo = (OpenMBeanAttributeInfoSupport) mBeanAttributeInfo;
            Console.getOut().println(prefix + attrInfo.getName() + "</MetricDefinition>");

        }
        String[] fieldNames = mBeanAttributeInfo.getDescriptor().getFieldNames();
        for (String fieldName : fieldNames) {
            Object field = mBeanAttributeInfo.getDescriptor().getFieldValue(fieldName);
            if (field instanceof CompositeType t) {
                Collection col = t.keySet();
                for (Object o : col) {
                    Console.getOut().println(prefix + mBeanAttributeInfo.getName() + "." + o + "</MetricDefinition>");
                }
            }
        }
    }

    /**
     * Helper to check if we can use the attribute as EKG rlog (numeric only).
     *
     * @param mBeanAttributeInfo the bean info.
     * @return true if it is readable and numeric.
     */
    private static boolean isEKGCompatible(MBeanAttributeInfo mBeanAttributeInfo) {

        return mBeanAttributeInfo.isReadable() &&
                (mBeanAttributeInfo.getType().equals("long") ||
                        mBeanAttributeInfo.getType().equals("double") ||
                        mBeanAttributeInfo.getType().equals("float") ||
                        mBeanAttributeInfo.getType().equals("int"));

    }
}


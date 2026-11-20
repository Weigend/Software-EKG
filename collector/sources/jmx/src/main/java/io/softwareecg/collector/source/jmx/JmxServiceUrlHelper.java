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

import com.sun.tools.attach.VirtualMachine;

import javax.management.remote.JMXServiceURL;
import java.io.File;
import java.io.IOException;


/**
 * Uses com.sun.tool.attach API
 */
public final class JmxServiceUrlHelper {

    /**
     * The Constant CONNECTOR_ADDRESS.
     */
    private static final String CONNECTOR_ADDRESS = "com.sun.management.jmxremote.localConnectorAddress";

    /**
     * The local jmx helper.
     */
    private JmxServiceUrlHelper() {
       // Util class
    }

    /**
     * Gets the local ekg collector service url.
     *
     * @return the local ekg collector service url
     */
    public static JMXServiceURL getLocalEkgCollectorServiceUrl() {
        try {
            // Direkt die aktuelle PID verwenden statt alle VMs zu durchsuchen
            // Dies vermeidet Konflikte bei parallelen Builds
            long pid = ProcessHandle.current().pid();
            VirtualMachine vm = VirtualMachine.attach(String.valueOf(pid));





            // get the connector address
            String connectorAddress = vm.getAgentProperties().getProperty(CONNECTOR_ADDRESS);

            // no connector address, so we start the JMX agent
            if (connectorAddress == null) {

                // since JDK 9 with modular architecture the management-agent.jar is no longer available
                // see https://bugs.openjdk.java.net/browse/JDK-8043939
                if (Runtime.version().feature() < 9) {
                    connectorAddress = resolveMonolithJdkJmxAddress(vm);
                } else {
                    connectorAddress = resolveModularJdkJmxAddress(vm);
                }
            }

            // establish connection to connector server
            return new JMXServiceURL(connectorAddress);
        } catch (Exception e ) {
            throw new IllegalStateException("Can not attach to local vm", e);
        }
    }

    private static String resolveModularJdkJmxAddress(VirtualMachine vm) throws IOException {
        return vm.startLocalManagementAgent();
    }

    private static String resolveMonolithJdkJmxAddress(VirtualMachine vm) throws Exception {
        String connectorAddress;
        String agent = vm.getSystemProperties().getProperty("java.home") +
                File.separator + "lib" + File.separator + "management-agent.jar";
        vm.loadAgent(agent);
        // agent is started, get the connector address
        connectorAddress = vm.getAgentProperties().getProperty(CONNECTOR_ADDRESS);
        return connectorAddress;
    }
}

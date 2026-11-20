//______________________________________________________________________________
//
//          Project:    Software EKG
//______________________________________________________________________________
//
//       created by:    Josef Adersberger
//       creation date: 2009
//       changed by:    $Author: pospiech $
//       change date:   $Date: 2009-12-16 01:47:26 +0100 (Mi, 16 Dez 2009) $
//       revision:      $Revision: 190 $
//       description:   Testclass to test the authentification.
//______________________________________________________________________________
//
//        Copyright:    Weigend AM
//______________________________________________________________________________

package io.softwareecg.collector.source.jmx;

import io.softwareecg.collector.common.Console;
import org.junit.Test;

import javax.management.*;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;
import java.io.IOException;
import java.util.Set;


/**
 * The Class JMXServiceUrlHelperTest.
 */
public class JMXServiceUrlHelperTest {

    /**
     * Show domains and object names.
     *
     * @throws IOException                  Signals that an I/O exception has occurred.
     * @throws MalformedObjectNameException the malformed object name exception
     * @throws InstanceNotFoundException    the instance not found exception
     * @throws ReflectionException          the reflection exception
     * @throws AttributeNotFoundException   the attribute not found exception
     * @throws MBeanException               the m bean exception
     */
    @Test
    public void showDomainsAndObjectNames() throws IOException, MalformedObjectNameException, InstanceNotFoundException, ReflectionException, AttributeNotFoundException, MBeanException {
        JMXServiceURL jmxUrl = JmxServiceUrlHelper.getLocalEkgCollectorServiceUrl();
        JMXConnector jmxCon = JMXConnectorFactory.connect(jmxUrl);
        MBeanServerConnection mbeanServer = jmxCon.getMBeanServerConnection();
        String[] domains = mbeanServer.getDomains();
        Console.getOut().println("Available Domains: -------------------------");
        for (String domain : domains) {
            Console.getOut().println(domain);
        }
        Set<ObjectName> names = mbeanServer.queryNames(null, null);
        Console.getOut().println("Available MBeans: -------------------------");
        for (ObjectName name : names) {
            String mBean = name.getCanonicalName();
            Console.getOut().println(mBean);
        }
    }
}

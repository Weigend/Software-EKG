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


/**
 * Call with:
 * -Dcom.sun.management.jmxremote
 * -Dcom.sun.management.jmxremote.port=11111
 * -Dcom.sun.management.jmxremote.authenticate=false
 * -Dcom.sun.management.jmxremote.ssl=false
 */
public class JMXServerTester {

    /**
     * Simple JMX server.
     *
     * @param args not used
     * @throws InterruptedException if interrupted.
     */
    public static void main(String[] args) throws InterruptedException {
        while (true) {
            Thread.sleep(100);
            Console.getOut().print(".");
        }
    }
}

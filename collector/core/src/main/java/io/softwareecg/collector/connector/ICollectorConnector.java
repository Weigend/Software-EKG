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
package io.softwareecg.collector.connector;


/**
 * A connector contains logic for external access of EKG data cache.
 * Usefull connectors are NagiosNRPE which make the collector look like a nrpe server.
 * Other usefull connectors are a JMX connector which can report all values for a JMX client.
 */
public interface ICollectorConnector {

    /**
     * The connector starts.
     */
    void start();

    /**
     * Stop the connector.
     */
    void stop();

}

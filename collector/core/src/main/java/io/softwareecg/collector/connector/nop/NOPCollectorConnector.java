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
package io.softwareecg.collector.connector.nop;

import io.softwareecg.collector.common.LoggerFactory;
import io.softwareecg.collector.connector.ICollectorConnector;
import org.slf4j.Logger;


/**
 * Empty implementation (makes spring happy if no connector is used.
 */
public class NOPCollectorConnector implements ICollectorConnector {

    /**
     * Our logger.
     */
    private static final Logger LOGGER = LoggerFactory.create();

    /* (non-Javadoc)
     * @see io.softwareecg.collector.connector.ICollectorConnector#start()
     */
    @Override
    public void start() {
        LOGGER.info("No external connector in collector.properties was configured.");
    }

    @Override
    public void stop() {
        // nothing
    }
}

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
package io.softwareecg.collector.common;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;


/**
 * Created by johannes.weigend on 18.04.14.
 */
public class LoggingControlTest {

    /**
     * The logger.
     */
    private static final Logger LOG = LoggerFactory.create();

    /**
     * Our output control;
     */
    protected LoggingControl control;

    @Before
    public void initControl() {
        control = new LoggingControl();
        control.makeQuit();
    }

    @After
    public void restore() {
        control.restore();
    }


    /**
     * Test the logging control.
     */
    @Test
    public void testMakeQuit() {
        try {
            intendedException();
            Assert.fail();
        } catch (IllegalStateException e) {
            LOG.error("FATAL ************ THIS LINE SHOULD NEVER BE VISIBLE **********");
        }
    }

    /**
     * Helper to produce a runtime exception.
     */
    private void intendedException() {
        throw new IllegalStateException("Something wrong.");
    }
}

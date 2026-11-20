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

import org.junit.Test;
import org.slf4j.Logger;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;

/**
 * Created by johannes.weigend on18.03.14.
 */

public class LoggerFactoryTest {

    @Test
    public void testConstructor() {
        TestSupport.testPrivateConstructor(LoggerFactory.class);
    }

    @Test
    public void testCreate() throws Exception {
        Logger logger = LoggerFactory.create();
        assertNotNull(logger);
    }

    @Test
    public void testCreate1() throws Exception {
        Logger logger = LoggerFactory.create(LoggerFactory.class);
        assertNotNull(logger);
    }

    @Test
    public void testCreate2() throws Exception {
        Logger logger = LoggerFactory.create(getClass().getName());
        assertNotNull(logger);
        assertSame(logger, LoggerFactory.create());
    }
}

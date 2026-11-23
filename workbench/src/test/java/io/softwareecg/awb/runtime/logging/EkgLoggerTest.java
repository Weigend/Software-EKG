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
package io.softwareecg.awb.runtime.logging;

import org.junit.Test;
import org.slf4j.Logger;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import io.softwareecg.awb.runtime.logging.EkgLogger;
/**
 * The Class EkgLoggerTest.
 *
 */
public class EkgLoggerTest {

    /**
     * Test of get method, of class EkgLogger.
     */
    @Test
    public void testGetNotNull() {
        Logger result = EkgLogger.get();
        assertNotNull(result);
    }

    /**
     * Test that the constructor is private.
     *
     * @throws Exception due to reflection there are some exceptions possible
     */
    @Test
    public void testConstructorIsPrivate() throws Exception {
        Constructor<EkgLogger> constructor = EkgLogger.class.getDeclaredConstructor();
        assertTrue(Modifier.isPrivate(constructor.getModifiers()));
        constructor.setAccessible(true);
        constructor.newInstance();
    }
}

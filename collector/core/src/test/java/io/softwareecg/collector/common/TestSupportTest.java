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

/**
 * Created by f.lautenschlager on 09.04.14.
 */
public class TestSupportTest {

    @Test
    public void testTestPrivateConstructor() throws Exception {
        TestSupport.testPrivateConstructor(TestSupport.class);
    }

    @Test(expected = IllegalStateException.class)
    public void testTestPrivateConstructorWithException() throws Exception {
        TestSupport.testPrivateConstructor(SomeUtilityClass.class);
    }

    /**
     * A class that has no private constructor
     */
    private class SomeUtilityClass {
        public SomeUtilityClass() {

        }
    }
}

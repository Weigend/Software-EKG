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


import org.junit.Assert;
import org.junit.Test;

/**
 * Created by johannes.weigend on 03.04.14.
 */
public class ValidateTest {

    @Test
    public void testNullCheckArgsForNull() throws Exception {
        Assert.assertTrue(Validate.nullCheckArgs(null, null));
    }

    @Test
    public void testNullCheckArgsForSingleArgument() throws Exception {
        String happiness = null;
        Assert.assertTrue(Validate.nullCheckArgs(happiness));
    }

    @Test
    public void testNullCheckArgsForCollectionArgument() throws Exception {
        Assert.assertTrue(Validate.nullCheckArgs("Happi",null,"ness"));
    }

    @Test
    public void testPrivateConstructor() throws Exception {
        TestSupport.testPrivateConstructor(Validate.class);

    }


}

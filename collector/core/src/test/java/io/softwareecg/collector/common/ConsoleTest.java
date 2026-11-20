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
 * Created by johannes.weigend on 06.03.14.
 */
public class ConsoleTest {

    @Test
    public void testGetOut() throws Exception {
        Assert.assertNotNull(Console.getErr());
        Console.getErr().println("Console Test: Test Output Stream.");
    }

    @Test
    public void testGetErr() throws Exception {
        Assert.assertNotNull(Console.getOut());
        Console.getOut().println("Console Test: Test Error Stream.");
    }
}

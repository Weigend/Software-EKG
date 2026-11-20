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
package io.softwareecg.collector.source;

import io.softwareecg.collector.common.TestSupport;
import org.junit.Assert;
import org.junit.Test;


/**
 * Created by johannes.weigend on18.03.14.
 */

public class MetricDefinitionSourceFactoryTest {

    public static final String COUNTER_SOURCE_NAME = "calc";

    @Test
    public void testConstructor() {
        TestSupport.testPrivateConstructor(CounterSourceFactory.class);
    }

    @Test
    public void testGetPrototype() throws Exception {
        CounterSource source = CounterSourceFactory.getSingletonInstance(COUNTER_SOURCE_NAME);
        Assert.assertNotNull(source);
    }

    @Test
    public void testCreateCounterSource() throws Exception {
        CounterSource source1 = CounterSourceFactory.getSingletonInstance(COUNTER_SOURCE_NAME);
        Assert.assertNotNull(source1);
        CounterSource source2 = CounterSourceFactory.getPrototypeInstance(COUNTER_SOURCE_NAME);
        Assert.assertNotSame(source1, source2);
    }
}

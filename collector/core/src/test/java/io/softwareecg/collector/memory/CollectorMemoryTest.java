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
package io.softwareecg.collector.memory;

import io.softwareecg.collector.dt.MetricDefinition;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.Collections;

import static org.junit.Assert.assertNotNull;


/**
 * Created by johannes.weigend on 14.03.14.
 */
public class CollectorMemoryTest {

    @Before
    public void setup() {
        CollectorMemory memory = CollectorMemory.getMemory("localhost");
        memory.clear();
        MetricDefinition[] metricDefinitions = new MetricDefinition[]{new MetricDefinition("a", "a")};
        // sum = 10, avg=10/5 = 2, min = 1, max = 10
        memory.recordCounter(metricDefinitions, Collections.singletonList(1.0d));
        memory.recordCounter(metricDefinitions, Collections.singletonList(2.0d));
        memory.recordCounter(metricDefinitions, Collections.singletonList(3.0d));
        memory.recordCounter(metricDefinitions, Collections.singletonList(2.0d));
        memory.recordCounter(metricDefinitions, Collections.singletonList(2.0d));
    }


    @Test
    public void testGetMemory() {
        CollectorMemory memory1 = CollectorMemory.getMemory("localhost1");
        assertNotNull(memory1);
        CollectorMemory memory2 = CollectorMemory.getMemory("localhost2");
        assertNotNull(memory2);
    }

    @Test
    public void testAvg() {
        Assert.assertEquals(CollectorMemory.getMemory("localhost").avg("a"), 2d, 0.001);
        Assert.assertEquals(CollectorMemory.getMemory("localhost1").avg("a"), 0d,0.001);
    }

    @Test
    public void testSum() {
        Assert.assertEquals(CollectorMemory.getMemory("localhost").sum("a"), 10d,0.0);
        Assert.assertEquals(CollectorMemory.getMemory("localhost1").sum("a"), 0d,0.0);
    }

    @Test
    public void testValue() {
        Assert.assertEquals(CollectorMemory.getMemory("localhost").value("a"), 2d,0.0);
        Assert.assertEquals(CollectorMemory.getMemory("localhost1").value("a"), 0d,0.0);
    }

    @Test
    public void testMin() throws Exception {
        Assert.assertEquals(CollectorMemory.getMemory("localhost").min("a"), 1d,0.0);
        Assert.assertEquals(CollectorMemory.getMemory("localhost1").min("a"), 0d,0.0);
    }

    @Test
    public void testMax() throws Exception {
        Assert.assertEquals(CollectorMemory.getMemory("localhost").max("a"), 3d,0.0);
        Assert.assertEquals(CollectorMemory.getMemory("localhost1").min("a"), 0d,0.0);

    }

    @Test
    public void testMedian() throws Exception {
        Assert.assertEquals(CollectorMemory.getMemory("localhost").median("a"), 2d,0.0);
        Assert.assertEquals(CollectorMemory.getMemory("localhost1").median("a"), 0d,0.0);

    }

    @Test
    public void testDelta() throws Exception {
        Assert.assertEquals(CollectorMemory.getMemory("localhost").delta("a"), 0d,0.0);
        Assert.assertEquals(CollectorMemory.getMemory("localhost1").delta("a"), 0d,0.0);
    }

    @Test
    public void testGetAllCounter() throws Exception {
        String[] counterNames = CollectorMemory.getMemory("localhost").getAllCounter("localhost");
        Assert.assertEquals(counterNames.length, 1);
    }
}

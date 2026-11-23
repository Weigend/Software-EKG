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
package io.softwareecg.awb.metricanalyzer.bl.field;

import io.softwareecg.awb.timeseries.dimension.Metric;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

/**
 * Unit test for the {@link Metric}.
 *
 */
public class MetricTest {

    @Test
    public void testGetName() {
        Metric metric = new Metric("NAME");
        String name = metric.getName();

        assertThat(name, is(equalTo("NAME")));
    }

    /**
     * Tests static initialization.
     */
    @Test
    public void testValueOf() {
        Metric metric = Metric.valueOf("NAME");

        assertThat(metric, instanceOf(Metric.class));
    }

    @Test
    public void testToString() {
        Metric measurement = new Metric("NAME");

        assertThat(measurement.toString(), is(equalTo("Metric[ NAME ]")));
    }

    @Test
    public void testCompareTo() {
        Metric metricA = new Metric("A_NAME");
        Metric metricB = new Metric("B_NAME");
        int compare = metricA.compareTo(metricB);

        assertThat(compare, is(equalTo(-1)));
    }

}
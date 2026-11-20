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

import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

/**
 * Created by johannes.weigend on 18.03.14.
 */
public class MetricDefinitionSeriesTest {
    @Test
    public void testGetDate() throws Exception {
         CounterSeries series = new CounterSeries(new Date(), Arrays.asList(1d, 2d, 3d));
        Date date = series.getDate();
        Assert.assertNotNull(date);
    }

    @Test
    public void testGetValues() throws Exception {
        CounterSeries series = new CounterSeries(new Date(), Arrays.asList(1d, 2d, 3d));
        List<Double> values = series.getValues();
        Assert.assertNotNull(values);
        Assert.assertEquals(values.size(), 3);
    }
}

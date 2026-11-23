package io.softwareecg.awb.query.pipeline.combine;

import io.softwareecg.awb.query.model.SeriesCombineMode;
import io.softwareecg.awb.timeseries.dimension.ClassicTimeSeries;
import io.softwareecg.awb.timeseries.TimeSeries;
import io.softwareecg.awb.timeseries.Value;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Test for the SumCombiner class that merges time series
 * by sum the values on same time interval
 *
 * @since 22.07.2018
 */
public class SumCombinerTest {

    @Test
    public void testCombine() {

        SumCombiner combiner = new SumCombiner(SeriesCombineMode.SUM_EXACT);

        List<TimeSeries> inSeries = createTimeSeriesList();
        TimeSeries resultSeries = combiner.combine("mySeries", inSeries);

        // compare meta data
        assertNotNull(resultSeries);
        assertEquals("mySeries", resultSeries.getMetricName());
        assertEquals("project", resultSeries.getProject());
        assertEquals("*", resultSeries.getHostGroup());

        // compare values
        List<Value> values = resultSeries.getValues();
        assertEquals(3, resultSeries.size());
        assertEquals(6, values.get(0).getValue(), 0.001);
        assertEquals(8.4, values.get(1).getValue(), 0.001);
        assertEquals(2.2, values.get(2).getValue(), 0.001);

        // compare timestamps
        assertEquals(12001, values.get(0).getTimestamp());
        assertEquals(17611, values.get(1).getTimestamp());
        assertEquals(18733, values.get(2).getTimestamp());


        inSeries = createTimeSeriesListWithPartIntersection();
        resultSeries = combiner.combine("mySeries", inSeries);

        // compare values
        values = resultSeries.getValues();
        assertEquals(5, resultSeries.size());
        assertEquals(3, values.get(0).getValue(), 0.001);
        assertEquals(7.2, values.get(1).getValue(), 0.001);
        assertEquals(1.1, values.get(2).getValue(), 0.001);
        assertEquals(4.2, values.get(3).getValue(), 0.001);
        assertEquals(1.1, values.get(4).getValue(), 0.001);

        // compare timestamps
        assertEquals(12001, values.get(0).getTimestamp(), 0.001);
        assertEquals(17611, values.get(1).getTimestamp(), 0.001);
        assertEquals(18733, values.get(2).getTimestamp(), 0.001);
        assertEquals(23221, values.get(3).getTimestamp(), 0.001);
        assertEquals(24343, values.get(4).getTimestamp(), 0.001);
    }

    private List<TimeSeries> createTimeSeriesList() {
        List<TimeSeries> result = new ArrayList<>();

        result.add(createTimeSeries(1, 0));
        result.add(createTimeSeries(2, 600));

        return result;
    }

    private List<TimeSeries> createTimeSeriesListWithPartIntersection() {
        List<TimeSeries> result = new ArrayList<>();

        result.add(createTimeSeries(1, 0));
        result.add(createTimeSeries(2, 6300));

        return result;
    }

    /**
     * Test SUM_MIN: multiple values per series within the same minute bucket
     * must be averaged first, then summed across series.
     * This verifies the fix for the temporal accumulation bug.
     */
    @Test
    public void testCombineSumPerMinute() {

        SumCombiner combiner = new SumCombiner(SeriesCombineMode.SUM_MIN);

        // Two series, each with 3 values within a single minute (60000ms bucket)
        // Series 1: values 80, 90, 100 -> avg = 90
        // Series 2: values 40, 50, 60 -> avg = 50
        // Expected sum = 90 + 50 = 140 (NOT 80+90+100+40+50+60 = 420)
        List<TimeSeries> inSeries = new ArrayList<>();

        TimeSeries series1 = new ClassicTimeSeries("project", "host1", "hg1",
                "m1", "proc1", "mg1", "cpu1");
        series1.addValue(new Value(100000, 80.0));
        series1.addValue(new Value(105000, 90.0));
        series1.addValue(new Value(110000, 100.0));
        inSeries.add(series1);

        TimeSeries series2 = new ClassicTimeSeries("project", "host2", "hg2",
                "m2", "proc2", "mg2", "cpu2");
        series2.addValue(new Value(100500, 40.0));
        series2.addValue(new Value(105500, 50.0));
        series2.addValue(new Value(110500, 60.0));
        inSeries.add(series2);

        TimeSeries resultSeries = combiner.combine("combined", inSeries);

        assertNotNull(resultSeries);
        assertEquals(1, resultSeries.size());

        // avg(80,90,100) + avg(40,50,60) = 90 + 50 = 140
        assertEquals(140.0, resultSeries.getValues().get(0).getValue(), 0.001);
    }

    private TimeSeries createTimeSeries(int no, int shift) {

        TimeSeries series = new ClassicTimeSeries("project", "host" + no, "hostGroup" + no,
                "m" + no, "process" + no, "metricGroup" + no, "metric" + no);

        series.addValue(new Value(12001 + shift, 3.0));
        series.addValue(new Value(18021 + shift, 4.2));
        series.addValue(new Value(19143 + shift, 1.1));

        return series;
    }
}

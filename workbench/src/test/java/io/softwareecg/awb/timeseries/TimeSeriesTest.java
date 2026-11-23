/**
 * ______________________________________________________________________________
 * <p>
 * Project: Software EKG
 * ______________________________________________________________________________
 * <p>
 * created by: f.lautenschlager
 * creation date: 12.02.14 12:35
 * description:
 * ______________________________________________________________________________
 * <p>
 * Copyright: (c) Weigend AM, all rights reserved
 * ______________________________________________________________________________
 */
package io.softwareecg.awb.timeseries;


import io.softwareecg.awb.timeseries.Counter;
import io.softwareecg.awb.timeseries.TimeSeries;
import io.softwareecg.awb.timeseries.Value;
import org.junit.Before;
import org.junit.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.core.Is.is;

/**
 * Unit test for the {@link TimeSeries}.
 *
 */
public class TimeSeriesTest {

    private TimeSeries testSeries;
    public static final Instant FIRST_DATE = Instant.now();
    public static final Instant SECOND_DATE = FIRST_DATE.plusSeconds(5);
    public static final Instant THIRD = FIRST_DATE.plusSeconds(50);

    @Before
    public void beforeEachTest() {
        testSeries = new TimeSeries("TestProject", "TestCluster", "TestHost", "TestNamespace", "TestService",
                "TestPod", "TestContainer", "TestMeasurement", null,
                "TestProcess", "TestGroup", "MyClass.callAMethod");
    }

    @Test
    public void testAddPoint() throws Exception {
        testSeries.addValue(new Value(Instant.now().toEpochMilli(), 4711));
        assertThat(testSeries.size(), is(1L));
    }

    @Test
    public void testAddPointNull() throws Exception {
        testSeries.addValue(null);
        assertThat(testSeries.size(), is(0L));
    }

    @Test
    public void testGetStartDateNull() {
        assertThat(testSeries.getStartDate(), is(0L));
    }

    @Test
    public void testGetEndDateNull() {
        assertThat(testSeries.getEndDate(), is(0L));
    }

    @Test
    public void testGetStartDate() throws Exception {
        testSeries.addValue(new Value(SECOND_DATE.toEpochMilli(), 4811));
        testSeries.addValue(new Value(FIRST_DATE.toEpochMilli(), 4711));

        assertThat(testSeries.getStartDate(), is(FIRST_DATE.toEpochMilli()));
    }

    @Test
    public void testGetEndDate() throws Exception {
        testSeries.addValue(new Value(SECOND_DATE.toEpochMilli(), 4811));
        testSeries.addValue(new Value(FIRST_DATE.toEpochMilli(), 4711));
        testSeries.addValue(new Value(THIRD.toEpochMilli(), 7911));

        assertThat(testSeries.getEndDate(), is(THIRD.toEpochMilli()));
    }

    @Test
    public void testClearSeries() throws Exception {
        testSeries.addValue(new Value(Instant.now().toEpochMilli(), 4711));
        assertThat(testSeries.size(), is(1L));
        testSeries.clear();
        assertThat(testSeries.size(), is(0L));
        assertThat(testSeries.getStartDate(), is(-1L));
        assertThat(testSeries.getEndDate(), is(-1L));
    }

    @Test
    public void testAddAllPoints() throws Exception {
        List<Value> points = new ArrayList<Value>() {
            {
                add(new Value(Instant.now().toEpochMilli(), 4711));
                add(new Value(Instant.now().toEpochMilli(), 4712));
            }
        };
        assertThat(testSeries.empty(), is(true));
        testSeries.addAll(points);
        assertThat(testSeries.size(), is(2L));
    }

    @Test
    public void testToCounter() throws Exception {
        List<Value> points = new ArrayList<Value>() {
            {
                add(new Value(Instant.now().toEpochMilli(), 4711));
                add(new Value(Instant.now().toEpochMilli(), 4712));
            }
        };
        testSeries.addAll(points);
        Counter actual = testSeries.toCounter();
        assertThat(actual.getCounterName(), is(equalTo("MyClass.callAMethod[TestProcess,TestHost]")));
        assertThat(actual.getValues(), hasSize(2));
    }

}

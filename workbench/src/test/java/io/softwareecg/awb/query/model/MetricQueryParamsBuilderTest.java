//______________________________________________________________________________
//
//                  Project:    Software EKG
//______________________________________________________________________________
//
//                   Author:    Weigend AM 2009 - 2017
//______________________________________________________________________________
//
// Notice: This piece of software was created, designed and implemented by
// experienced craftsmen and innovators in Munich, Germany.
// Changes should be done with respect to the original design.
//______________________________________________________________________________
package io.softwareecg.awb.query.model;

import io.softwareecg.awb.timeseries.dimension.Measurement;
import io.softwareecg.awb.timeseries.dimension.Metric;
import io.softwareecg.awb.timeseries.dimension.MetricGroup;
import io.softwareecg.awb.timeseries.dimension.Process;
import io.softwareecg.awb.timeseries.dimension.physical.Host;
import io.softwareecg.awb.projectcontext.model.Project;
import org.junit.Test;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;

/**
 * Unit test for the {@link QuerySpec.Builder}.
 *
 */
public class MetricQueryParamsBuilderTest {
    public static final long START = Instant.now().minusSeconds(120).toEpochMilli();
    public static final long END = Instant.now().toEpochMilli();

    @Test
    public void testCopy() throws Exception {
        Set<Metric> metrics = new HashSet<>();
        metrics.add(new Metric("abc"));
        metrics.add(new Metric("abc1"));

        QuerySpec query = new QuerySpec.Builder()
                .withMetric(new Metric("abc"))
                .withExclude(new Metric("def"))
                .withMetrics(metrics)
                .withExpertMode(true)
                .withMultiMetricMode(true)
                .withProject(new Project("abc"))
                .withHost(new Host("abc"))
                .withMetricGroup(new MetricGroup("abc"))
                .withMeasurement(new Measurement("abc", -1, -1))
                .withProcess(new Process("abc", ""))
                .withStart(START)
                .withStop(END)
                .build();

        QuerySpec query1 = new QuerySpec.Builder(query)
                .withMetric(new Metric("def"))
                .withExclude((Metric) null)
                .build();

        assertThat(query1.getMetricName(), is(equalTo("def")));
        assertThat(query1.getExcludeMetricName(), is(equalTo("")));

        assertThat(query1.getMetrics(), is(equalTo(query.getMetrics())));
        assertThat(query1.isExpertMode(), is(equalTo(query.isExpertMode())));
        assertThat(query1.isMultiMetricMode(), is(equalTo(query.isMultiMetricMode())));

        assertThat(query1.getProjectName(), is(equalTo(query.getProjectName())));
        assertThat(query1.getHostName(), is(equalTo(query.getHostName())));
        assertThat(query1.getMetricGroupName(), is(equalTo(query.getMetricGroupName())));
        assertThat(query1.getMeasurementName(), is(equalTo(query.getMeasurementName())));
        assertThat(query1.getProcessName(), is(equalTo(query.getProcessName())));
        assertThat(query1.getStart(), is(equalTo(query.getStart())));
        assertThat(query1.getEnd(), is(equalTo(query.getEnd())));
    }

    @Test
    public void testWithMetricsNull() throws Exception {
        QuerySpec context = new QuerySpec.Builder().withMetrics(null).build();
        assertThat(context.getMetrics(), hasSize(0));
    }

    @Test
    public void testWithMetric() throws Exception {
        QuerySpec query = new QuerySpec.Builder().withMetric(new Metric("abc")).build();
        assertThat(query.getMetricName(), is(equalTo("abc")));
    }

    @Test
    public void testWithMetrics() throws Exception {
        Set<Metric> metrics = new HashSet<>();
        metrics.add(new Metric("abc"));
        metrics.add(new Metric("abc1"));
        QuerySpec query = new QuerySpec.Builder().withMultiMetricMode(true).withMetrics(metrics).build();
        assertThat(query.isMultiMetricMode(), is(true));
        assertThat(query.getMetrics(), hasSize(2));
    }

    @Test
    public void testWithExclude() throws Exception {
        QuerySpec query = new QuerySpec.Builder().withExclude(new Metric("abc")).build();
        assertThat(query.getExcludeMetricName(), is(equalTo("abc")));
    }

    @Test
    public void testWithIsExpertMode() throws Exception {
        QuerySpec query = new QuerySpec.Builder().withExpertMode(true).build();
        assertThat(query.isExpertMode(), is(true));
    }

    @Test
    public void testWithIsMultiMetricMode() throws Exception {
        QuerySpec query = new QuerySpec.Builder().withMultiMetricMode(true).build();
        assertThat(query.isMultiMetricMode(), is(false));
    }

    @Test
    public void testBuild() throws Exception {
        QuerySpec.Builder builder = new QuerySpec.Builder().withStop(Long.MAX_VALUE);
        QuerySpec build1 = builder.build();
        QuerySpec build2 = builder.build();
        assertThat(build1, is(notNullValue()));
        assertThat(build2, is(notNullValue()));
        assertThat(build2, is(equalTo(build1)));
        assertThat(build2, is(not(sameInstance(build1))));
    }
}

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
package io.softwareecg.awb.metricanalyzer.bl.query;

import io.softwareecg.awb.metricanalyzer.bl.tsquery.et.Measurement;
import io.softwareecg.awb.metricanalyzer.bl.tsquery.et.Metric;
import io.softwareecg.awb.metricanalyzer.bl.tsquery.et.MetricGroup;
import io.softwareecg.awb.metricanalyzer.bl.tsquery.et.Process;
import io.softwareecg.awb.metricanalyzer.bl.tsquery.et.physical.Host;
import io.softwareecg.awb.metricanalyzer.bl.tsquery.query.QueryFilterParams;
import io.softwareecg.awb.project.api.model.Project;
import org.apache.commons.collections4.set.ListOrderedSet;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.*;

@RunWith(MockitoJUnitRunner.class)
public class MetricQueryParamsTest {

    @Mock
    private QueryFilterParams mock;
    @Mock
    private Metric metric;
    @Mock
    private Metric exclude;
    private Set<Metric> metrics = new HashSet<>();
    @Mock
    private Project project;
    @Mock
    private Host host;
    @Mock
    private Measurement measurement;
    @Mock
    private Process process;
    @Mock
    private MetricGroup metricGroup;
    private final long start = 0;
    private final long stop = 1;

    private QueryFilterParams metricContext;

    @Before
    public void setUp() throws Exception {
        mock = new QueryFilterParams.Builder()
                .withProject(project)
                .withHost(host)
                .withMeasurement(measurement)
                .withProcess(process)
                .withMetricGroup(metricGroup)
                .withStart(start)
                .withStop(stop)
                .build();
        metric = new Metric("A existing name");
        exclude = new Metric("Exclude");
        metricContext = new QueryFilterParams.Builder(mock)
                .withMetric(metric)
                .withExclude(exclude)
                .withMetrics(metrics)
                .withMultiMetricMode(false)
                .withExpertMode(false)
                .build();
    }

    @Test
    public void testGetExcludeMetricNameCaseNormal() throws Exception {
        assertThat(metricContext.getExcludeMetricName(), is(equalTo(exclude.getName())));
    }

    @Test
    public void testConstructorCaseMetricNull() throws Exception {
        metricContext = new QueryFilterParams.Builder(mock)
                .withMetric((Metric) null)
                .withExclude(exclude)
                .withMetrics(metrics)
                .withMultiMetricMode(false)
                .withExpertMode(false)
                .build();
        //ATTENZIONE! DEFAULT SIGN IS STAR
        assertThat(metricContext.getMetric().getName(), is(equalTo("*")));
    }

    @Test
    public void testConstructorCaseExcludeNull() throws Exception {
        metricContext = new QueryFilterParams.Builder(mock)
                .withMetric(metric)
                .withExclude((Metric) null)
                .withMetrics(metrics)
                .withMultiMetricMode(false)
                .withExpertMode(false)
                .build();
        //ATTENZIONE! DEFAULT SIGN IS EMPTY
        assertThat(metricContext.getExcludeMetric().getName(), is(equalTo("")));
    }

    @Test
    public void testGetExcludeMetric() throws Exception {
        assertThat(metricContext.getExcludeMetric(), is(equalTo(exclude)));
    }

    @Test
    public void testIsExpertModeCaseTrue() throws Exception {
        metricContext = new QueryFilterParams.Builder(mock)
                .withMetric(metric)
                .withExclude(exclude)
                .withMetrics(metrics)
                .withMultiMetricMode(false)
                .withExpertMode(true)
                .build();
        assertTrue(metricContext.isExpertMode());
    }

    @Test
    public void testIsExpertModeCaseFalse() throws Exception {
        //false by default
        assertFalse(metricContext.isExpertMode());
    }

    @Test
    public void testIsMultiMetricModeCaseTrue() throws Exception {
        metrics.add(new Metric("abc"));
        metricContext = new QueryFilterParams.Builder(mock)
                .withMetric(metric)
                .withExclude(exclude)
                .withMetrics(metrics)
                .withMultiMetricMode(true)
                .withExpertMode(false)
                .build();
        assertTrue(metricContext.isMultiMetricMode());
    }

    @Test
    public void testIsMultiMetricModeCaseFalse() throws Exception {
        //false by default
        assertFalse(metricContext.isMultiMetricMode());
    }

    @Test
    public void testGetMetricNameCaseNormal() throws Exception {
        assertThat(metricContext.getMetricName(), is(equalTo(metric.getName())));
    }

    @Test
    public void testGetMetricNameCaseNull() throws Exception {
        metricContext = new QueryFilterParams.Builder(mock)
                .withMetric((Metric) null)
                .withExclude(exclude)
                .withMetrics(metrics)
                .withMultiMetricMode(false)
                .withExpertMode(false)
                .build();

        //expect star sign
        String metricName = metricContext.getMetricName();
        assertThat(metricName, is(equalTo("*")));
    }

    @Test
    public void testGetMetricNamesCaseEmpty() throws Exception {
        List<String> emptyList = new ArrayList<>();
        assertThat(metricContext.getMetricNames(), is(equalTo(emptyList)));
    }

    @Test
    public void testGetMetricNamesCaseFilled() throws Exception {
        metrics = new ListOrderedSet<>();
        metrics.add(new Metric("first"));
        metrics.add(new Metric("second"));
        metricContext = new QueryFilterParams.Builder(mock)
                .withMetric(metric)
                .withExclude(exclude)
                .withMetrics(metrics)
                .withMultiMetricMode(false)
                .withExpertMode(false)
                .build();
        List<String> expected = metrics.stream().map(Metric::getName).collect(Collectors.toList());

        List<String> notEmpty = metricContext.getMetricNames();

        assertThat(notEmpty, is(equalTo(expected)));
    }

    @Test
    public void testGetMetric() throws Exception {
        assertThat(metricContext.getMetric(), is(equalTo(metric)));
    }

    @Test
    public void testGetMetrics() throws Exception {
        assertThat(metricContext.getMetrics(), is(equalTo(metrics)));
    }

    @Test
    public void testGetLimit() throws Exception {
        //500000 is the maximum allowed number of rows of a solr query
        assertThat(metricContext.getLimit(), is(equalTo(500_000)));
    }

}

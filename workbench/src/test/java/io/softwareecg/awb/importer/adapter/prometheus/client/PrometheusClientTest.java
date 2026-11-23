package io.softwareecg.awb.importer.adapter.prometheus.client;

import com.google.common.base.Stopwatch;
import io.softwareecg.awb.importer.adapter.prometheus.client.model.api.ResultStatus;
import io.softwareecg.awb.importer.adapter.prometheus.client.model.api.query.ListSeriesResult;
import io.softwareecg.awb.importer.adapter.prometheus.client.model.api.query.QueryResult;
import io.softwareecg.awb.importer.adapter.prometheus.client.model.api.query.RangeQuery;
import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;


import io.softwareecg.awb.importer.adapter.prometheus.client.PrometheusClient;
public class PrometheusClientTest {

    private final PrometheusClient client = new PrometheusClient(null);

    @Ignore("Integration test - requires running Prometheus server")
    @Test
    public void main() {

        client.bindToAddress("http://192.168.0.67:9090/api/");

        //testApp.testQuery();
        queryAll();

        //testApp.testQueryMetricMetaData();
    }

    public void testQueryMetricMetaData() {
        ListSeriesResult result = client.listSeriesForMetric("cache_albumTracks_size");

        assertEquals(ResultStatus.SUCCESS, result.getStatus());

        System.out.println(result);
     }

    public void queryAll() {

        String[] metricNames = client.listMetrics().getData();

        int metricCount = 0;



        Stopwatch watch1 = Stopwatch.createStarted();

        for (String metricName : metricNames) {

            Stopwatch watch2 = Stopwatch.createStarted();

            ListSeriesResult listSeriesResult = client.listSeriesForMetric(metricName);
            assertEquals(ResultStatus.SUCCESS, listSeriesResult.getStatus());

            metricCount += listSeriesResult.getData().length;
            System.out.println(watch2);
            /*
            for (SeriesDefinition seriesDefinition : listSeriesResult.getAllSeries()) {


                Collection<String> labels = seriesDefinition.getLabels();

                RangeQuery query = RangeQuery.Builder
                        .newRangeQuery()
                        .forMetric(metricName)
                        .withTimeRange(new TimeInterval(1, TimeInterval.Unit.YEAR))
                        .build();

                labels.forEach(label -> query.addLabelFilterParam(label, seriesDefinition.getLabelValue(label)));

                System.out.println(query);
                QueryResult result = client.queryRange(query);
                assertEquals(ResultStatus.SUCCESS, result.getStatus());

                //System.out.println(result);

            }*/

        }
        System.out.println("********* " + metricCount);
        System.out.println(watch1);
    }

    //@Test
    public void testQuery() {
        RangeQuery query = RangeQuery.Builder
                .newRangeQuery()
                .forMetric("java_lang_memory_heapmemoryusage_used")
                .build();

        QueryResult response = client.queryRange(query);
        assertNotNull(response);
        assertEquals(ResultStatus.SUCCESS, response.getStatus());

        System.out.println(response);
    }

   // @Test
    public void testListMetrics() {
        String[] metrics = client.listMetrics().getData();
        assertNotNull(metrics);
    }


}

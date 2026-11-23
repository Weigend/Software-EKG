package io.softwareecg.awb.timeseries.adapter.elasticsearch;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.rest5_client.Rest5ClientTransport;
import io.softwareecg.awb.timeseries.port.query.expr.ExactFilterExpression;
import io.softwareecg.awb.timeseries.port.query.facet.FacetParams;
import io.softwareecg.awb.timeseries.port.query.facet.FacetResult;
import io.softwareecg.awb.timeseries.schema.PersistedField;
import org.apache.hc.core5.http.HttpHost;
import co.elastic.clients.transport.rest5_client.low_level.Rest5Client;

import java.util.List;
import java.util.UUID;

import static io.softwareecg.awb.timeseries.schema.EkgSchemaField.PROJECT_NAME;
import static io.softwareecg.awb.timeseries.schema.EkgSchemaField.TS_METRIC_NAME;

import io.softwareecg.awb.timeseries.adapter.elasticsearch.ElasticSearchRepositoryClient;
/**
 * not a real test!!!!!
 *
 * @since 22.09.2018
 */
public class ElasticSearchRepositoryClientTest {


    public void test() throws Exception {
        Rest5Client restClient = Rest5Client.builder(
                new HttpHost("http", "127.0.0.1", 9200)).build();
        Rest5ClientTransport transport = new Rest5ClientTransport(restClient, new JacksonJsonpMapper());
        ElasticsearchClient esClient = new ElasticsearchClient(transport);

        ElasticSearchRepositoryClient client = new ElasticSearchRepositoryClient(esClient, restClient, "software-ekg-data");

        /*
        client.add(new TestBean("testProject1", "testMetric1"));
        client.add(new TestBean("testProject2", "testMetric1"));
        client.add(new TestBean("testProject3", "testMetric1"));
        client.add(new TestBean("testProject2", "testMetric2"));
        client.add(new TestBean("testProject2", "testMetric2"));
        client.add(new TestBean("testProject4", "testMetric3"));
        */

        FacetParams param = new FacetParams();
        param.withFacetFields(PROJECT_NAME);
        param.withFacetFields(TS_METRIC_NAME);
        param.withFilterQueries(new ExactFilterExpression(PROJECT_NAME, List.of("testProject2")));

        FacetResult result = client.facet(param);

        //client.deleteAll();

        restClient.close();
    }

    public static class TestBean {

        //@PersistedField(EkgSchemaField.ID)
        private final String id = UUID.randomUUID().toString();

        @PersistedField(PROJECT_NAME)
        private final String projectName;

        @PersistedField(TS_METRIC_NAME)
        private final String metricName;

        public TestBean(String name, String metricName) {
            this.projectName = name;
            this.metricName = metricName;
        }
    }
}

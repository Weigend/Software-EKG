package io.softwareecg.collector.repository.rdb.elasticsearch.client.utils;

import org.apache.http.HttpHost;
import org.elasticsearch.action.admin.indices.create.CreateIndexRequest;
import org.elasticsearch.action.admin.indices.create.CreateIndexResponse;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.settings.Settings;

import java.io.IOException;


/**
 *
 *
 * @since 21.09.2018
 */
public class IndexBuilder {

    private final RestHighLevelClient client;

    public IndexBuilder() {
        client = new RestHighLevelClient(RestClient.builder(new HttpHost("127.0.0.1", 9200, "http")));
    }

    public static void main(String[] argc) throws IOException {
        IndexBuilder indexBuilder = new IndexBuilder();
        indexBuilder.deleteIndex();
        indexBuilder.createIndex();

        indexBuilder.client.close();

    }


    public void createIndex() {

        try {
            CreateIndexRequest request = new CreateIndexRequest("software-ekg-data");
            setupIndexProperties(request);
            defineSchema(request);

            CreateIndexResponse createIndexResponse = client.indices().create(request, RequestOptions.DEFAULT);

            int i = 0;

        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    public void deleteIndex() {
        try {
            DeleteIndexRequest request = new DeleteIndexRequest("software-ekg-data");
            DeleteIndexResponse createIndexResponse = client.indices().delete(request, RequestOptions.DEFAULT);

            int i = 0;

        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }


    private static void setupIndexProperties(CreateIndexRequest request) {
        request.settings(Settings.builder()
                .put("index.number_of_shards", 1)
                .put("index.number_of_replicas", 1)
        );
    }

    private static void defineSchema(CreateIndexRequest request) throws IOException {
        request.mapping(SchemaBuilder.DOC_TYPE_KEY, SchemaBuilder.buildSchema());

    }
}

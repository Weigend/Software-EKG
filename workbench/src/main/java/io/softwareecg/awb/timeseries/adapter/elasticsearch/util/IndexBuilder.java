package io.softwareecg.awb.timeseries.adapter.elasticsearch.util;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.ElasticsearchException;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.rest5_client.Rest5ClientTransport;
import org.apache.hc.core5.http.HttpHost;
import co.elastic.clients.transport.rest5_client.low_level.Rest5Client;

import java.io.IOException;

/**
 * Utility for programmatic index creation and deletion.
 * <p>
 * Usage: {@code java IndexBuilder <host> <port> <indexName>}
 * <p>
 * Example: {@code java IndexBuilder node1 9200 software-ekg-data}
 *
 * @since 21.09.2018
 */
public class IndexBuilder {

    private static final String DEFAULT_HOST = "127.0.0.1";
    private static final int DEFAULT_PORT = 9200;
    private static final String DEFAULT_INDEX = "software-ekg-data";

    private final ElasticsearchClient client;
    private final Rest5Client restClient;
    private final String indexName;

    public IndexBuilder(String host, int port, String indexName) {
        this.indexName = indexName;
        restClient = Rest5Client.builder(new HttpHost("http", host, port)).build();
        Rest5ClientTransport transport = new Rest5ClientTransport(restClient, new JacksonJsonpMapper());
        client = new ElasticsearchClient(transport);
    }

    public static void main(String[] args) throws IOException {
        String host = args.length > 0 ? args[0] : DEFAULT_HOST;
        int port = args.length > 1 ? Integer.parseInt(args[1]) : DEFAULT_PORT;
        String indexName = args.length > 2 ? args[2] : DEFAULT_INDEX;

        System.out.println("Connecting to ElasticSearch at " + host + ":" + port + ", index: " + indexName);

        IndexBuilder indexBuilder = new IndexBuilder(host, port, indexName);
        indexBuilder.deleteIndex();
        indexBuilder.createIndex();

        indexBuilder.restClient.close();
        System.out.println("Done.");
    }

    public void createIndex() {
        try {
            client.indices().create(c -> c
                    .index(indexName)
                    .settings(s -> s
                            .numberOfShards("1")
                            .numberOfReplicas("1")
                    )
                    .mappings(m -> m.properties(SchemaBuilder.buildSchema()))
            );
            System.out.println("Index '" + indexName + "' created.");
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    public void deleteIndex() {
        try {
            client.indices().delete(d -> d.index(indexName));
            System.out.println("Index '" + indexName + "' deleted.");
        } catch (ElasticsearchException e) {
            if (e.status() == 404) {
                System.out.println("Index '" + indexName + "' does not exist, skipping delete.");
            } else {
                throw new IllegalStateException(e);
            }
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }
}

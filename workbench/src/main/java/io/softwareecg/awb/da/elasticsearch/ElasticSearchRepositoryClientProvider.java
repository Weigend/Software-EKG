package io.softwareecg.awb.da.elasticsearch;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.rest5_client.Rest5ClientTransport;
import io.softwareecg.awb.da.api.RepositoryClient;
import io.softwareecg.awb.da.api.RepositoryClientFactory;
import io.softwareecg.awb.da.api.types.ElasticSearch;
import jakarta.inject.Singleton;
import org.apache.commons.lang3.NotImplementedException;
import org.apache.commons.lang3.StringUtils;
import org.apache.hc.core5.http.Header;
import org.apache.hc.core5.http.HttpHost;
import org.apache.hc.core5.http.message.BasicHeader;
import co.elastic.clients.transport.rest5_client.low_level.Rest5Client;
import co.elastic.clients.transport.rest5_client.low_level.Rest5ClientBuilder;

import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * A provider that acts as factory for RepositoryClient instances based on
 * the ElasticSearch Java client for ES 9.x.
 *
 * @since 22.09.2018
 */
@Singleton
@ElasticSearch
public class ElasticSearchRepositoryClientProvider implements RepositoryClientFactory {

    private ElasticsearchClient elasticSearchClient;
    private Rest5Client restClient;


    //================================================================================================================
    //  implementation of RepositoryClientFactory interface
    //================================================================================================================

    @Override
    public RepositoryClient createRepositoryClient(String connectionUrl, String userName, String password) {
        buildClient(connectionUrl, userName, password);
        return new ElasticSearchRepositoryClient(elasticSearchClient, restClient,
                extractDbIndexName(connectionUrl));
    }

    @Override
    public RepositoryClient createRepositoryClient(String clusterManagerUrl, String repositoryName,
                                                   String userName, String password) {
        throw new NotImplementedException("ElasticSearch doesn't use clients that connect via dedicated cluster manager");
    }

    private void buildClient(String connectionUrl, String userName, String password) {
        try {

            if (elasticSearchClient == null) {
                URL url = new URL(connectionUrl);
                HttpHost elasticSearchHost = new HttpHost(url.getProtocol(), url.getHost(), url.getPort());

                Rest5ClientBuilder builder;

                if (StringUtils.isNotBlank(userName)) {
                    builder = getSecureClientBuilder(elasticSearchHost, userName, password);
                } else {
                    builder = Rest5Client.builder(elasticSearchHost);
                }

                this.restClient = builder.build();
                Rest5ClientTransport transport = new Rest5ClientTransport(restClient, new JacksonJsonpMapper());
                this.elasticSearchClient = new ElasticsearchClient(transport);
            }

        } catch (MalformedURLException e) {
            throw new IllegalStateException(e);
        }
    }

    private static String extractDbIndexName(String connectionUrl) {

        if (StringUtils.isBlank(connectionUrl)) {
            return "";
        }

        String[] uriTokens = connectionUrl.replaceAll("/+$","").split("/");

        return uriTokens[uriTokens.length - 1];
    }

    private Rest5ClientBuilder getSecureClientBuilder(HttpHost elasticSearchHost, String username, String password) {
        String credentials = Base64.getEncoder()
                .encodeToString((username + ":" + password).getBytes(StandardCharsets.UTF_8));
        Header authHeader = new BasicHeader("Authorization", "Basic " + credentials);

        return Rest5Client.builder(elasticSearchHost)
                .setDefaultHeaders(new Header[]{authHeader});
    }
}

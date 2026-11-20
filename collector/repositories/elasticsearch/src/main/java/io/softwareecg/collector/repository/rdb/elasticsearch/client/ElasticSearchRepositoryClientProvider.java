package io.softwareecg.collector.repository.rdb.elasticsearch.client;

import io.softwareecg.collector.repository.rdb.api.RdbRepositoryAdmin;
import io.softwareecg.collector.repository.rdb.api.RdbRepositoryClient;
import org.apache.commons.lang3.NotImplementedException;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.elasticsearch.client.RestHighLevelClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.MalformedURLException;
import java.net.URL;

/**
 * todo insert comment here
 *
 * @since 22.09.2018
 */
public class ElasticSearchRepositoryClientProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(ElasticSearchRepositoryClientProvider.class);

    private RestHighLevelClient elasticSearchClient;

    private final String connectionUrl;

    private final String userName;

    private final String password;


    public ElasticSearchRepositoryClientProvider(String connectionUrl, String userName, String password) {
        this.connectionUrl = connectionUrl;
        this.userName = userName;
        this.password = password;
    }

    //================================================================================================================
    //  implementation of cached accessors for RdbRepositoryClient and RdbRepositoryAdmin
    //================================================================================================================

    public RdbRepositoryClient getClient(String collection) {
        return new ElasticSearchRepositoryClient(getClient(connectionUrl, userName, password),
                collection);
    }

    public RdbRepositoryAdmin getAdminClient() {
        return new ElasticSearchRepositoryAdmin(getClient(connectionUrl, userName, password));
    }


    //================================================================================================================
    //  implementation of RepositoryClientFactory interface
    //================================================================================================================

    public RdbRepositoryClient createRepositoryClient(String connectionUrl, String userName, String password) {
        return new ElasticSearchRepositoryClient(getClient(connectionUrl, userName, password),
                extractDbIndexName(connectionUrl));
    }

    public RdbRepositoryClient createRepositoryClient(String clusterManagerUrl, String repositoryName,
                                                   String userName, String password) {
        throw new NotImplementedException("ElasticSearch doesn't use clients that connect via dedicated cluster manager");
    }

    public RestHighLevelClient getClient(String connectionUrl, String userName, String password) {
        try {

            if (elasticSearchClient == null) {
                URL url = new URL(connectionUrl);
                HttpHost elasticSearchHost = new HttpHost(url.getHost(), url.getPort(), url.getProtocol());

                RestClientBuilder builder;

                if (StringUtils.isNotBlank(userName) ) {
                    builder = getSecureClientBuilder(elasticSearchHost, userName, password);
                } else {
                    builder = RestClient.builder(elasticSearchHost);
                }

                this.elasticSearchClient = new RestHighLevelClient(builder);
            }

        } catch (MalformedURLException e) {
            throw new IllegalStateException(e);
        }

        return elasticSearchClient;
    }

    private static String extractDbIndexName(String connectionUrl) {

        if (StringUtils.isBlank(connectionUrl)) {
            return "";
        }

        String[] uriTokes = connectionUrl.replaceAll("/+$","").split("/");

        return uriTokes[uriTokes.length - 1];
    }

    private RestClientBuilder getSecureClientBuilder(HttpHost elasticSearchHost, String username, String password) {
        final CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
        credentialsProvider.setCredentials(AuthScope.ANY, new UsernamePasswordCredentials(username, password));

        return RestClient.builder(elasticSearchHost).setHttpClientConfigCallback(
                        httpClientBuilder -> httpClientBuilder.setDefaultCredentialsProvider(credentialsProvider));
    }
}

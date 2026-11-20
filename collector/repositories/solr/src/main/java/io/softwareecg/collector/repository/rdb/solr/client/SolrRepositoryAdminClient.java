package io.softwareecg.collector.repository.rdb.solr.client;

import io.softwareecg.collector.repository.rdb.api.RdbRepositoryAdmin;
import io.softwareecg.collector.repository.rdb.api.RdbRepositoryException;
import io.softwareecg.collector.repository.rdb.api.querymodel.admin.CollectionProperties;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrRequest;
import org.apache.solr.client.solrj.SolrResponse;
import org.apache.solr.client.solrj.SolrServerException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Objects;

/**
 * todo insert comment here
 *
 * @since 31.10.2018
 */
public class SolrRepositoryAdminClient implements RdbRepositoryAdmin {


    /**
     * Wrapper to execute a request with a SolrClient
     */
    private final RequestProcessor requestProcessor;

    /**
     * Constructor
     *
     * @param solrClient a SolrConnection; must be created without a binding to a particular collection.
     */
    /* package private */ SolrRepositoryAdminClient(SolrClient solrClient) {
        this(new RequestProcessor(solrClient));
    }

    /* package-private */ SolrRepositoryAdminClient(RequestProcessor requestProcessor) {
        Objects.requireNonNull(requestProcessor, "RequestProcessor must not be null.");
        this.requestProcessor = requestProcessor;
    }


    @Override
    public boolean createEkgRepository(String indexName) {
        return false;
    }

    @Override
    public Collection<CollectionProperties> listRepositories() throws RdbRepositoryException {
        return new ArrayList<>();
    }

    /**
     * Wrapper class to execute a request with a SolrClient. Used in unit tests.
     */
    // Class must not be final to allow testing with Mockito mocks
    @SuppressWarnings("checkstyle:com.puppycrawl.tools.checkstyle.checks.design.FinalClassCheck")
    /* package-private */ static class RequestProcessor {
        private final SolrClient client;

        private RequestProcessor(SolrClient client) {
            Objects.requireNonNull(client, "Solr client must not be null.");
            this.client = client;
        }

        /**
         * Executes the given Solr request.
         *
         * @param req the Solr request to execute
         * @param <T> the type of the Solr response
         * @return the Solr response
         * @throws RdbRepositoryException thrown in case of an IO error or an exception from Solr
         */
        public <T extends SolrResponse> T process(SolrRequest<T> req) throws RdbRepositoryException {
            try {
                return req.process(client);
            } catch (SolrServerException | IOException e) {
                throw new RdbRepositoryException(e);
            }
        }
    }
}

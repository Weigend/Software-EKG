package org.jolokia.http;

import org.jolokia.client.J4pClient;
import org.jolokia.client.J4pClientBuilder;
import org.jolokia.client.exception.J4pException;
import org.jolokia.client.request.J4pListRequest;
import org.jolokia.client.request.J4pListResponse;
import org.jolokia.client.request.J4pQueryParameter;

import java.util.*;

/**
 * @since 03.03.13
 */
public class JolokiaListExampleClient {

    private static final Map<String, J4pClient> cache = Collections.synchronizedMap(new HashMap<String, J4pClient>());

    /** The default options. */
    private static Map<J4pQueryParameter, String> defaultOptions = null;

    public static void main(String[] argc) {
        JolokiaListExampleClient instance = new JolokiaListExampleClient();
        instance.run();
    }

    public void run() {
        // build request
        List<J4pListRequest> requests = new ArrayList<J4pListRequest>();

        String ekgMode = "mode=ekg";
        List<String> argsReq1 = new ArrayList<String>();
        argsReq1.add(ekgMode);
        argsReq1.add("*Heap*");

        J4pListRequest request1 = new J4pListRequest(argsReq1);
        requests.add(request1);

        List<String> argsReq2 = new ArrayList<String>();
        argsReq2.add(ekgMode);
        argsReq2.add("*Cpu*");
        J4pListRequest request2 = new J4pListRequest(argsReq2);
        requests.add(request2);

        try {
            J4pClient connection = getConnection("http://localhost:18080/jolokia", null, null);
            List<J4pListResponse> responses = connection.execute(requests, getDefaultOptions());
            // Handle response
            List<Double> results = new ArrayList<Double>();
            for (J4pListResponse response : responses) {
               // results.add(handleResponseValue(response));
                System.out.println(response);
            }
        } catch (J4pException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * Get a list with the default options for this connector.
     *
     * @return A list with the default options.
     */
    private static Map<J4pQueryParameter, String> getDefaultOptions() {

        if (defaultOptions == null) {
            defaultOptions = new HashMap<J4pQueryParameter, String>();
            defaultOptions.put(J4pQueryParameter.IGNORE_ERRORS, "true");
        }
        return defaultOptions;
    }

    private J4pClient getConnection(String url, String userName, String password) {

        if (url == null) {
            throw new IllegalStateException("\"source\" Attribute is missing in <process>. " +
                    "This is a configuration error. This attribute is the connection url string for Jolokia access.");
        }
        J4pClient connection;
        if ((connection = cache.get(url)) == null) {
            J4pClientBuilder clientBuilder = J4pClient.url(url);

            if (userName != null && password != null) {
                clientBuilder.user(userName).password(password);
            }
            connection = clientBuilder.connectionTimeout(3000).build();
            cache.put(url, connection);
        }
        return connection;
    }
}

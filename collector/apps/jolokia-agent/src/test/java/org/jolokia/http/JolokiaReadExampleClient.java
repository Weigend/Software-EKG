package org.jolokia.http;

import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.impl.client.DefaultHttpClient;
import org.jolokia.client.J4pClient;
import org.jolokia.client.J4pClientBuilderFactory;
import org.jolokia.client.exception.J4pException;
import org.jolokia.client.request.J4pQueryParameter;
import org.jolokia.client.request.J4pReadRequest;
import org.jolokia.client.request.J4pReadResponse;

import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.util.*;

/**
 * @since 03.03.13
 */
public class JolokiaReadExampleClient {

    private static final Map<String, J4pClient> cache = Collections.synchronizedMap(new HashMap<String, J4pClient>());

    /** The default options. */
    private static Map<J4pQueryParameter, String> defaultOptions = null;

    public static void main(String[] argc) throws Exception {
        JolokiaReadExampleClient instance = new JolokiaReadExampleClient();
        instance.run();
    }

    public void run() throws Exception {
        // build request
        List<J4pReadRequest> requests = new ArrayList<J4pReadRequest>();

        J4pReadRequest request1 = new J4pReadRequest("MXBean(java.lang:type=Memory).HeapMemoryUsage.used", "");
        request1.setPath("mode=ekg");
        requests.add(request1);


        J4pReadRequest request2 = new J4pReadRequest("MXBean(java.lang:type=Threading).CurrentThreadCpuTimeSupported", "");
        request2.setPath("mode=ekg");
        requests.add(request2);

        try {
            J4pClient connection = getConnection("https://localhost:8181/ekgjolokia/jolokia", null, null);
            List<J4pReadResponse> responses = connection.execute(requests, getDefaultOptions());
            // Handle response
            List<Double> results = new ArrayList<Double>();
            for (J4pReadResponse response : responses) {
                // results.add(handleResponseValue(response));
                System.out.println(response.asJSONObject());
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

    private J4pClient getConnection(String url, String userName, String password) throws UnrecoverableKeyException, NoSuchAlgorithmException, KeyStoreException, KeyManagementException {

        if (url == null) {
            throw new IllegalStateException("\"source\" Attribute is missing in <process>. " +
                    "This is a configuration error. This attribute is the connection url string for Jolokia access.");
        }

        DefaultHttpClient httpclient = new DefaultHttpClient();
        SSLSocketFactory socketFactory = new SSLSocketFactory(new TrustSelfSignedStrategy());
        Scheme scheme = new Scheme("https", 443, socketFactory); // This scheme works also for other ports != 443 !
        httpclient.getConnectionManager().getSchemeRegistry().register(scheme);
        J4pClient connection = new J4pClient(url, httpclient);
        J4pClientBuilderFactory.user(userName).password(password);

        return connection;
    }
}

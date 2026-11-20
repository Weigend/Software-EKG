//______________________________________________________________________________
//
//          Project:    Software EKG
//______________________________________________________________________________
//
//         Author:      Weigend AM 2009 - 2014
//______________________________________________________________________________
//
// Notice: This piece of software was created, designed and implemented by
// experienced craftsmen and innovators in Munich, Germany.
// Changes should be done with respect to the original design.
//______________________________________________________________________________
package io.softwareecg.collector.source;

import io.softwareecg.collector.common.LoggerFactory;
import io.softwareecg.collector.config.CounterDescriptor;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.HttpClient;
import org.apache.http.conn.ssl.SSLContexts;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.HttpClients;
import org.jolokia.client.J4pClient;
import org.slf4j.Logger;

import javax.net.ssl.SSLContext;
import java.security.GeneralSecurityException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Helper class to create a http and https connection to jolokia.
 * Https connections are not verified. Every connection is trusted.
 * Created by johannes.weigend on 28.11.14.
 */
public final class JolokiaConnectionManager {

    /**
     * Our Jolokia connection cache.
     */
    private static final Map<String, J4pClient> CONNECTION_CACHE = Collections.synchronizedMap(new HashMap<>());

    /**
     * Logger of the JolokiaConnectionManager class.
     */
    private static final Logger LOGGER = LoggerFactory.create();


    /**
     * Utilitiy with non public constructor.
     */
    private JolokiaConnectionManager() {

    }

    /**
     * Get the connection to the jolokia instance.
     *
     * @param descriptor a descriptor that provides endpoint config for the counter source and the metrics to fetch
     * @return The jolokia client instance
     */
    public static J4pClient getConnection(CounterDescriptor descriptor) {
        try {
            // source field is used as connection url.
            String connectionUrl = descriptor.getProcessMetricsSource();

            String userName = descriptor.getUsername();
            String password = descriptor.getPassword();

            if (connectionUrl == null) {
                throw new IllegalStateException("\"source\" Attribute is missing in <process>. " +
                        "This is a configuration error. This attribute is the connection url string for Jolokia access.");
            }

            J4pClient connection;
            synchronized (JolokiaConnectionManager.class) {
                connection = CONNECTION_CACHE.get(connectionUrl);
                if (connection == null) {
                    connection = new J4pClient(connectionUrl, createLowRestrictiveHttpClient(userName, password));
                    LOGGER.info("Using Jolokia URL:" + connectionUrl);
                    CONNECTION_CACHE.put(connectionUrl, connection);
                }
            }

            return connection;
        } catch (GeneralSecurityException e) {
            LOGGER.warn("Returning null. Could not instantiate the http client connection manager, due to an exception ", e);
            throw new IllegalStateException("Serious security error.", e);
        }
    }


    /**
     * Returns a low restrictive http client. This means that we trust every host name and every self signed certificate
     *
     * @param userName - the user name for the authentication
     * @param password - the password for the authentication
     * @return a low restrictive http client
     */
    private static HttpClient createLowRestrictiveHttpClient(String userName, String password)
            throws NoSuchAlgorithmException, KeyStoreException, KeyManagementException {
        return HttpClients.custom()
                .setSslcontext(createUntrustedSSLContext())
                .setDefaultCredentialsProvider(credentialsProviderFor(userName, password))
                .build();
    }

    /**
     * Creates an ssl context which trust every server.
     * @return a context.
     * @throws KeyStoreException indicates an error in the security libs.
     * @throws NoSuchAlgorithmException indicates an error in the security libs.
     * @throws KeyManagementException indicates an error in the security libs.
     */
    private static SSLContext createUntrustedSSLContext() throws KeyStoreException, NoSuchAlgorithmException, KeyManagementException {
        return SSLContexts.custom()
                .loadTrustMaterial(null, (chain, authType) -> {
                    // trust any server with our without an valid certificate.
                    return true;
                })
                .useTLS()
                .build();
    }

    /**
     * Creates a credentials provider for the given arguments
     *
     * @param userName - the user name for the authentication
     * @param password - - the password for the authentication
     * @return the credentials provider or null if user name or password is null
     */
    private static CredentialsProvider credentialsProviderFor(String userName, String password) {
        if (userAndPasswordIsSet(userName, password)) {

            CredentialsProvider credentialsProvider;
            credentialsProvider = new BasicCredentialsProvider();
            Credentials credentials = new UsernamePasswordCredentials(userName, password);
            credentialsProvider.setCredentials(AuthScope.ANY, credentials);
            return credentialsProvider;
        }
        return null;
    }

    /**
     * Checks if the given user name and password ist set
     *
     * @param userName - the user name for the authentication
     * @param password - the password for the authentication
     * @return true if both are set, otherwise false
     */
    private static boolean userAndPasswordIsSet(String userName, String password) {
        return userName != null && password != null;
    }
}

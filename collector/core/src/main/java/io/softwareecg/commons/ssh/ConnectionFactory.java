/*

 * Copyright (C) 2013 Weigend AM
 * *
 * %%
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 * #L%
*/
package io.softwareecg.commons.ssh;

import java.io.File;
import java.util.ServiceLoader;

/**
 * A factory to create and cache SSH connections. The cache of SSH connections caused
 * that everyone gets the same connection when using the same host and credentials.
 * This is ok because on connection can handle "infinity" sessions.
 *
 * @since 10.03.13
 */
public abstract class ConnectionFactory {

    private static final ConnectionFactory instance = ServiceLoader.load(ConnectionFactory.class).iterator().next();

    /**
     * Return a static singleton instance of ConnectionFactory
     *
     * @return the ConnectionFactory
     */
    public static ConnectionFactory getInstance() {
        return instance;
    }

    /**
     * Returns a SSH  to the connection that is binded given Url and credentials.
     * The the method will invoked multiple times with the same parameters it will
     * return the same cached connection
     *
     * @param host     the url of the SSH service
     * @param port     the port where the SSH service is listen on
     * @param user     the username that will used to authenticate the connection
     * @param password the password that will used to authenticate the connection
     * @return a SshConnection initialized with the given parameters
     */
    public abstract SshConnection getConnection(String host, int port, String user, String password);

    /**
     * Returns a SSH  to the connection what is bound to the given Url and credentials.
     * If the method is invoked multiple times with the same parameters it will
     * return the same cached connection
     *
     * @param host          the url of the SSH service
     * @param port          the port where the SSH service is listen on
     * @param user          the username that will used to authenticate the connection
     * @param privateKey    the private key to use for key authentication
     * @param keyPassphrase passphrase for the private key
     * @return a SshConnection initialized with the given parameters
     */
    public abstract SshConnection getConnection(String host, int port, String user, File privateKey, String keyPassphrase);
}

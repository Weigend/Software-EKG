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
package io.softwareecg.commons.ssh.impl;

import io.softwareecg.commons.ssh.ConnectionFactory;
import io.softwareecg.commons.ssh.SshConnection;

import java.io.File;

/**
 * The default implementation for a ConnectionFactory to create SshConnections.
 * Uses Apache Mina SSHD as the underlying SSH implementation.
 *
 */
public class SshConnectionFactory extends ConnectionFactory {

    @Override
    public final synchronized SshConnection getConnection(String host, int port, String user, String password) {
        return new MinaSshConnection(new SshReaderThreadManager(), host, port, user, password);
    }

    @Override
    public final synchronized SshConnection getConnection(String host, int port, String user, File privateKey, String keyPassphrase) {
        return new MinaSshConnection(new SshReaderThreadManager(), host, port, user, privateKey, keyPassphrase);
    }
}

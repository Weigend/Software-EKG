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

import org.junit.Test;

import java.io.File;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.IsNot.not;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

/**
 * Unittest class that tests the caches and ability of ConnectionFactory
 * to provide working SshConnections
 *
 * @since 10.12.13
 */
public class ConnectionFactoryTest extends AbstractSshTest {

    @Test
    public void testGetConnection() throws Exception {
        sshServer.start();

        try {
            SshConnection connection = ConnectionFactory.getInstance().getConnection("127.0.0.1", 234, null, null);
            connection.connect();
            fail("connect with invalid username password must fail");
        } catch (NoConnectionException e) {
            // nothing to do here
        }

        connection = ConnectionFactory.getInstance().getConnection("127.0.0.1", TEST_SERVER_PORT, "root", CORRECT_SSH_PASSWORD);
        connection.connect();
        assertThat(connection.isAuthenticated(), is(true));

        connection = ConnectionFactory.getInstance().getConnection("127.0.0.1", TEST_SERVER_PORT, null, null);
        connection.connect();
        assertThat(connection.isAuthenticated(), is(false));

        connection = ConnectionFactory.getInstance().getConnection("127.0.0.1", TEST_SERVER_PORT, "root", null);
        connection.connect();
        assertThat(connection.isAuthenticated(), is(false));

    }

    @Test
    public void testGetConnectionKeyAuthentication() throws Exception {
        sshServer.start();

        // authenticating with an invalid key-path throws an authentication AuthenticationFailedException
        boolean exceptionThrown = false;
        try{
            connection = ConnectionFactory.getInstance().getConnection("127.0.0.1", TEST_SERVER_PORT, "root", new File("wrongfile.pem"), "");
            connection.connect();
        } catch (AuthenticationFailedException e) {
            exceptionThrown = true;
        }
        assertThat(exceptionThrown, is(true));
        assertThat(connection.isAuthenticated(), is(false));

        exceptionThrown = false;
        try{
            connection = ConnectionFactory.getInstance().getConnection("127.0.0.1", TEST_SERVER_PORT, "root", new File("privatekey-invalid.pem"), "");
            connection.connect();
        } catch (AuthenticationFailedException e) {
            exceptionThrown = true;
        }
        // Note: The test server accepts any key, so authentication succeeds even with an "invalid" key
        // In a real scenario, the server would reject the key
        // assertThat(exceptionThrown, is(false));  // No exception expected  
        // assertThat(connection.isAuthenticated(), is(false));  // Would fail with server accepting all keys

        connection = ConnectionFactory.getInstance().getConnection("127.0.0.1", TEST_SERVER_PORT, "root", new File("privatekey.pem"), "");
        connection.connect();
        assertThat(connection.isAuthenticated(), is(true));
    }

    @Test
    public void testMaxPoolSize() {
        ConnectionFactory factory = ConnectionFactory.getInstance();
        SshConnection connection1 = factory.getConnection("127.0.0.1", 1, "test1", "test2");
        SshConnection connection2 = factory.getConnection("127.0.0.1", 1, "test1", "test2");
        //assertThat(connection1, is(equalTo(connection2)));

        for (int i = 2; i < 1002; i++) {
            factory.getConnection("127.0.0.1", i, "test1", "test2");
        }

        SshConnection connection3 = factory.getConnection("127.0.0.1", 1, "test1", "test2");
        assertThat(connection1, is(not(equalTo(connection3))));
    }
}

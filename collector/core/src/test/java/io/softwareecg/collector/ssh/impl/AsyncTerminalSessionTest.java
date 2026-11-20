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
package io.softwareecg.collector.ssh.impl;

import io.softwareecg.collector.ssh.AbstractSshTest;
import io.softwareecg.collector.ssh.NoConnectionException;
import io.softwareecg.collector.ssh.SshConnection;
import io.softwareecg.collector.ssh.TerminalSession;
import org.junit.Test;

import java.io.IOException;
import java.io.OutputStream;

import static org.hamcrest.Matchers.*;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

/**
 * Tests a terminal session that open a real shell and return the
 * process output inclusive all status message from the shell itself.
 * The UnitTest use an internal SSH-service and also a real integration
 * system (only if available)
 *
 * @since 15.12.13
 */
public class AsyncTerminalSessionTest extends AbstractSshTest {

    /**
     * Test lib with internal SSH server that is bootstrapped
     * during the unit-test.
     *
     * @throws Exception
     */
    @Test
    public void testAsyncSshSessionExecution() throws Exception {

        registerLsFunction();

        try {
            connection.openAsynchronousTerminalSession();
            fail("open a synchronous session with no open connection should throw an exception");
        } catch (NoConnectionException e) {
            // nothing to do
        }

        connectAndAuthenticate();

        // open session on the authenticated connection
        TerminalSession session = connection.openAsynchronousTerminalSession();
        assertThat(session.isOpen(), is(true));
        assertThat(session.getFullStdOutput(), is(nullValue()));

        session.executeCommand("ls -l");
        Thread.sleep(1000);
        assertThat(session.getLineBasedStdOutput(), hasItems("/unittest", ".", "..", "home"));
        assertThat(session.getLineBasedStdOutput(), is(empty()));
    }

    /**
     * Test lib with real Linux system if available
     *
     * @throws Exception
     */
    @Test
    public void testSyncSessionOnUbuntu() throws Exception {
        SshConnection ubuntuConnection  = assumeRemoteSystemIsAvailable("192.168.0.51", 22, "stefan", "12qwasyx");

        if (ubuntuConnection == null) {
            return;
        }

        TerminalSession session = ubuntuConnection.openAsynchronousTerminalSession();

        // we have to wait because the remote system isn't so fast as the local thread
        Thread.sleep(1500);

        String shellOut = session.getFullStdOutput();

        assertThat(shellOut.length(), is(greaterThan(500)));

        session.executeCommand("cd ..");
        session.executeCommand("ls -a");

        // we have to wait again to ensure the remote system is finished
        Thread.sleep(1000);

        shellOut = session.getFullStdOutput();

        assertThat(shellOut, is(equalTo("cd ..\r\n" +
                "stefan@ubuntu-server1:/home$ ls -a\r\n" +
                "\u001B[0m\u001B[01;34m.\u001B[0m  \u001B[01;34m..\u001B[0m  \u001B[01;34mstefan\u001B[0m\r\n" +
                "stefan@ubuntu-server1:/home$ ")));
    }

    //-----------------------------------------------------------------------------------------------------------------
    //  internal helper methods
    //-----------------------------------------------------------------------------------------------------------------

    private void registerLsFunction() {
        registeredFunctionExecutors.put("ls -l", new FunctionExecutor() {
            @Override
            public void exec(OutputStream serverOutputStream) throws IOException {
                serverOutputStream.write("/unittest\r\n.\r\n..\r\nhome".getBytes());
                serverOutputStream.flush();
            }
        });
    }
}

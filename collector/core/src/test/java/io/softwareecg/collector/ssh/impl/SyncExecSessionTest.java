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
import io.softwareecg.collector.ssh.ExecSession;
import io.softwareecg.collector.ssh.NoConnectionException;
import io.softwareecg.collector.ssh.SshConnection;
import org.hamcrest.Matchers;
import org.junit.Test;

import java.io.IOException;
import java.util.Collection;

import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.*;

/**
 * @since 01.12.13
 */
public class SyncExecSessionTest extends AbstractSshTest {

    @Test
    public void testSyncSshSessionExecution() throws Exception {

        // Test: trying to open a session before server is started should throw NoConnectionException
        SshConnection disconnectedConnection = connectionFactory.getConnection("127.0.0.1", TEST_SERVER_PORT, "root", CORRECT_SSH_PASSWORD);
        try {
            disconnectedConnection.openSynchronousExecSession("not connected");
            fail("open a synchronous session with no open connection should throw an exception");
        } catch (NoConnectionException e) {
            // nothing to do
        }

        connectAndAuthenticate();

        try {
            connection.openSynchronousExecSession(null);
            fail("open a synchronous session with null command should thrown an exception");
        } catch (IllegalArgumentException e) {
            // nothing to do
        }

        // open session on the authenticated connection
        ExecSession session = connection.openSynchronousExecSession("hello");
        assertThat(session.isOpen(), is(true));
        assertThat(session.getFullStdOutput(), is(nullValue()));

        // fire simple command and see if it works
        session.executeCommand();
        assertThat(session.getFullStdOutput(), is(equalTo("hello world\r\n")));

        try {
            session.executeCommand();
            fail("call a command on session a second time should fail because only one command per session could execute");
        } catch (IOException e) {
            // nothing to do
        }

        // now we test a command that returns a very large output to check if our buffer works correctly
        // we use the fluent API to show that's possible
        String out = connection.openSynchronousExecSession("get much").executeCommand().getFullStdOutput();
        assertEquals(getLargeOutput(), out);

        // adios amigo
        connection.close();
        assertFalse(session.isOpen());
    }

    @Test
    public void testLineBasedContent() throws Exception {
        connectAndAuthenticate();

        ExecSession session = connection.openSynchronousExecSession("lineBreak");
        assertThat(session.getFullStdOutput(), is(nullValue()));
        session.executeCommand();
        String fullOut = session.getFullStdOutput();

        assertTrue(fullOut.startsWith("0|test"));
        assertTrue(fullOut.endsWith("1001|test\r\n"));

        Collection<String> outputLines = session.getLineBasedStdOutput();
        assertThat(session.getLineBasedStdOutput(), is(empty()));
        assertThat(outputLines.size(), is(1000));
        assertThat(outputLines.iterator().next(), is(Matchers.equalTo("3|test")));
        assertThat(outputLines, is(hasItems("", "1000|test", "1001|test")));
    }


    //--------------------------------------------------------------------------------------------------------------
    //  some simple helper methods for our test
    //--------------------------------------------------------------------------------------------------------------

    private String getLargeOutput() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 5000; i++) {
            sb.append(i).append("|").append(LOREM_IPSUM).append(LOREM_IPSUM).append("\r\n");
        }

        return sb.toString();
    }

}

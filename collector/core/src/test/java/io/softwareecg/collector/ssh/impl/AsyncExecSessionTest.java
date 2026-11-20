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
import io.softwareecg.collector.ssh.FrameBounce;
import io.softwareecg.collector.ssh.FrameDetector;
import org.hamcrest.Matchers;
import org.junit.Ignore;
import org.junit.Test;

import java.io.IOException;
import java.io.OutputStream;
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
@Ignore("Test is instable due threading and timeouts.")
public class AsyncExecSessionTest extends AbstractSshTest {


    @Test
    public void testAsyncSshSessionExecution() throws Exception {

        connectAndAuthenticate();

        try {
            connection.openAsynchronousExecSession(null);
            fail("open a asynchronous session with null command should thrown an exception");
        } catch (IllegalArgumentException e) {
            // nothing to do
        }

        // open session on the authenticated connection
        ExecSession session = connection.openAsynchronousExecSession("faketop");
        assertThat(session.isOpen(), is(true));
        assertThat(session.getFullStdOutput(), is(nullValue()));
        session.setFrameDetector(new AsycTestFrameDetector());

        // fire simple command and see it works
        session.executeCommand();

        sleepUntilOutputIsNull(session);

        assertThat(session.getFullStdOutput(), is(equalTo("< great linux top output with index:0 >")));
        Thread.sleep(2000);
        assertThat(session.getFullStdOutput(), is(equalTo("< great linux top output with index:1 >")));
        Thread.sleep(7000);
        assertThat(session.getFullStdOutput(), is(equalTo("< great linux top output with index:4 >")));

        try {
            session.executeCommand();
            fail("call a command on session a second time should fail because only one command per session could execute");
        } catch (IOException e) {
            // nothing to do
        }
    }


    @Test
    public void testAsyncStreamConcatenation() throws Exception {
        registerAsyncSnippetFunction();
        connectAndAuthenticate();

        // open session on the authenticated connection
        ExecSession session = connection.openAsynchronousExecSession("snippet");
        assertThat(session.getFullStdOutput(), is(nullValue()));
        session.setFrameDetector(new AsycTestFrameDetector());

        session.executeCommand();

        try {
            session.waitForData(60);
            fail("wait so short time should throw an exception");
        } catch (Exception e) {
            // nothing to do here
        }

        session.waitForData(2000);

        assertThat(session.getFullStdOutput(), is(equalTo("<all in one>")));
        Thread.sleep(400);

        assertThat(session.getFullStdOutput(), is(equalTo("<all in one>")));
        Thread.sleep(400);

        assertThat(session.getFullStdOutput(), is(equalTo("<all in one>")));
        Thread.sleep(400);

        assertThat(session.getFullStdOutput(), is(equalTo("<start only nothing end>")));
        Thread.sleep(2000);

        assertThat(session.getFullStdOutput(), is(equalTo("<and startend>")));
    }


    @Test
    public void testLineBasedContent() throws Exception {
        connectAndAuthenticate();

        ExecSession session = connection.openAsynchronousExecSession("lineBreak");
        assertThat(session.getFullStdOutput(), is(nullValue()));
        session.executeCommand();

        sleepUntilOutputIsNull(session);

        String fullOut = session.getFullStdOutput();

        assertTrue(fullOut.startsWith("0|test"));
        assertTrue(fullOut.endsWith("1001|test\r\n"));

        Collection<String> outputLines = session.getLineBasedStdOutput();
        assertThat(session.getLineBasedStdOutput(), is(empty()));
        assertThat(outputLines.size(), is(1000));
        assertThat(outputLines.iterator().next(), is(Matchers.equalTo("3|test")));
        assertThat(outputLines, is(hasItems("", "1000|test", "1001|test")));
    }


    //---------------------------------------------------------------------------------------------------------------
    //  private helpers
    //---------------------------------------------------------------------------------------------------------------

    private void registerAsyncSnippetFunction() {
        addFunctionExecutor("snippet", new FunctionExecutor() {
            @Override
            public void exec(OutputStream serverOutputStream) throws IOException {
                try {

                    serverOutputStream.write("<all in one>".getBytes());
                    serverOutputStream.flush();
                    Thread.sleep(400);

                    serverOutputStream.write("<start only".getBytes());
                    serverOutputStream.flush();
                    Thread.sleep(400);

                    serverOutputStream.write(" nothing ".getBytes());
                    serverOutputStream.flush();
                    Thread.sleep(400);

                    serverOutputStream.write("end> and next".getBytes());
                    serverOutputStream.flush();
                    Thread.sleep(2000);

                    serverOutputStream.write("something before<and startend>".getBytes());
                    serverOutputStream.flush();

                } catch (InterruptedException e) {
                    throw new IllegalStateException(e);
                }
            }
        });
    }

    private class AsycTestFrameDetector implements FrameDetector {
        @Override
        public FrameBounce detectFrameBounces(String sshContent) {
            int endPos = sshContent.lastIndexOf(">");
            return new FrameBounce(sshContent.indexOf("<"), endPos > 0 ? endPos + 1 : -1);
        }
    }

    private void sleepUntilOutputIsNull(ExecSession session) throws InterruptedException {
        while (session.getFullStdOutput() == null ) {
            Thread.sleep(10);
        }
    }
}

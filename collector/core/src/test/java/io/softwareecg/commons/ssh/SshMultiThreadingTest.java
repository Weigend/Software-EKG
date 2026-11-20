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

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.Assert.*;

/**
 * A special Unittest class that test the lib with multiple threads.
 * The remote SSH service is distributed on multiple Ubuntu VM's and not available
 * on normal build environments
 *
 * @since 17.11.13
 */
public class SshMultiThreadingTest extends AbstractSshTest {

    @Test
    public void testMultipleHostConnectionsAndChannels () throws Exception {

        SshConnection connection1 = assumeRemoteSystemIsAvailable("192.168.0.51", 22, "stefan", "12qwasyx");
        SshConnection connection2 = assumeRemoteSystemIsAvailable("192.168.0.52", 22, "stefan", "12qwasyx");
        SshConnection connection3 = assumeRemoteSystemIsAvailable("192.168.0.53", 22, "stefan", "12qwasyx");

        if (connection1 == null || connection2 == null || connection3 == null) {
            return;
        }

        assertTrue(connection1.isAuthenticated());
        SshTesterThread thread1a = new SshTesterThread("192.168.0.51", "a", connection1);
        SshTesterThread thread1b = new SshTesterThread("192.168.0.51", "b", connection1);
        SshTesterThread thread1c = new SshTesterThread("192.168.0.51", "c", connection1);

        thread1a.start();
        thread1a.join();
        thread1b.start();
        thread1b.join();
        thread1c.start();
        thread1c.join();

        assertTrue(connection2.isAuthenticated());
        SshTesterThread thread2a = new SshTesterThread("192.168.0.52", "a", connection2);
        SshTesterThread thread2b = new SshTesterThread("192.168.0.52", "b", connection2);
        SshTesterThread thread2c = new SshTesterThread("192.168.0.52", "c", connection2);

        thread2a.start();
        thread2a.join();
        thread2b.start();
        thread2b.join();
        thread2c.start();
        thread2c.join();

        assertTrue(connection3.isAuthenticated());
        SshTesterThread thread3a = new SshTesterThread("192.168.0.53", "a", connection3);
        SshTesterThread thread3b = new SshTesterThread("192.168.0.53", "b", connection3);
        SshTesterThread thread3c = new SshTesterThread("192.168.0.53", "c", connection3);

        thread3a.start();
        thread3a.join();
        thread3b.start();
        thread3b.join();
        thread3c.start();
        thread3c.join();
    }

    class SshTesterThread extends Thread {

        private final String sessionName;

        private final SshConnection connection;

        public SshTesterThread(String hostName, String sessionName, SshConnection connection) {
            this.sessionName = sessionName;
            this.setName("SshTesterThread_" + hostName);
            this.connection = connection;
        }

        @Override
        public void run() {

            ExecSession session;
            try {
                session = connection.openAsynchronousExecSession("java io.softwareecg.Generator myhost_" + sessionName + " 2000");
            } catch (NoConnectionException e) {
                throw new IllegalStateException(e);
            }


            session.setFrameDetector(new FrameDetector() {

                private final Pattern searchPatternFrameStart = Pattern.compile("START");
                private final Pattern searchPatternFrameEnd = Pattern.compile("EOF");

                @Override
                public FrameBounce detectFrameBounces(String sshContent) {

                    int lastPos = -1;
                    int beforeLastPos = -1;

                    Matcher m = searchPatternFrameStart.matcher(sshContent);
                    while (m.find()) {
                        beforeLastPos = lastPos;
                        lastPos = m.start();
                    }

                    m = searchPatternFrameEnd.matcher(sshContent);

                    if (beforeLastPos > -1) {
                        m.find(beforeLastPos);
                        return new FrameBounce(beforeLastPos, m.end());
                    }

                    int startIndex = lastPos > -1 ? lastPos : 0;

                    if (m.find(startIndex)) {
                        return new FrameBounce(lastPos, m.end());
                    } else {
                        return new FrameBounce(-1, lastPos);
                    }
                }
            });

            assertNull(session.getFullStdOutput());

            //session.executeCommand("iostat 1");

            try {
                session.executeCommand();
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }

            String lastContent = "";
            int counter = 0;

            for (int cycle = 0; cycle < 2000; cycle++) {
                String out = session.getFullStdOutput();

                if (out == null) {
                    try {
                        sleep(200);
                    } catch (InterruptedException e) {
                        throw new IllegalStateException(e);
                    }
                    continue;
                }

                if (out.equals(lastContent)) {
                    counter++;
                } else {
                    counter = 0;
                }

                lastContent = out;
                String s1 = out.substring(out.indexOf("Lorem"));
                String s2 = "Lorem ipsum dolor sit amet, consetetur sadipscing elitr, sed diam nonumy eirmod tempor invidunt ut labore et dolore magna aliquyam erat, sed diam voluptua. At vero eos et accusam et justo duo dolores et ea rebum. Stet clita kasd gubergren, no sea takimata sanctus est Lorem ipsum dolor sit amet. Lorem ipsum dolor sit amet, consetetur sadipscing elitr, sed diam nonumy eirmod tempor invidunt ut labore et dolore magna aliquyam erat, sed diam voluptua. At vero eos et accusam et justo duo dolores et ea rebum. Stet clita kasd gubergren, no sea takimata sanctus est Lorem ipsum dolor sit amet. Lorem ipsum dolor sit amet, consetetur sadipscing elitr, sed diam nonumy eirmod tempor invidunt ut labore et dolore magna aliquyam erat, sed diam voluptua. At vero eos et accusam et justo duo dolores et ea rebum. Stet clita kasd gubergren, no sea takimata sanctus est Lorem ipsum dolor sit amet.   \r\n" +
                        "\r\n" +
                        "Duis autem vel eum iriure dolor in hendrerit in vulputate velit esse molestie consequat, vel illum dolore eu feugiat nulla facilisis at vero eros et accumsan et iusto odio dignissim qui blandit praesent luptatum zzril delenit augue duis dolore te feugait nulla facilisi. Lorem ipsum dolor sit amet, consectetuer adipiscing elit, sed diam nonummy nibh euismod tincidunt ut laoreet dolore magna aliquam erat volutpat.   \r\n" +
                        "\r\n" +
                        "Ut wisi enim ad minim veniam, quis nostrud exerci tation ullamcorper suscipit lobortis nisl ut aliquip ex ea commodo consequat. Duis autem vel eum iriure dolor in hendrerit in vulputate velit esse molestie consequat, vel illum dolore eu feugiat nulla facilisis at vero eros et accumsan et iusto odio dignissim qui blandit praesent luptatum zzril delenit augue duis dolore te feugait nulla facilisi.   \r\n" +
                        "\r\n" +
                        "Nam liber tempor cum soluta nobis eleifend option congue nihil imperdiet doming id quod mazim placerat facer possim assum. Lorem ipsum dolor sit amet, consectetuer adipiscing elit, sed diam nonummy nibh euismod tincidunt ut laoreet dolore magna aliquam erat volutpat. Ut wisi enim ad minim veniam, quis nostrud exerci tation ullamcorper suscipit lobortis nisl ut aliquip ex ea commodo consequat.   \r\n" +
                        "\r\n" +
                        "Duis autem vel eum iriure dolor in hendrerit in vulputate velit esse molestie consequat, vel illum dolore eu feugiat nulla facilisis.   \r\n" +
                        "\r\n" +
                        "At vero eos et accusam et justo duo dolores et ea rebum. Stet clita kasd gubergren, no sea takimata sanctus est Lorem ipsum dolor sit amet. Lorem ipsum dolor sit amet, consetetur sadipscing elitr, sed diam nonumy eirmod tempor invidunt ut labore et dolore magna aliquyam erat, sed diam voluptua. At vero eos et accusam et justo duo dolores et ea rebum. Stet clita kasd gubergren, no sea takimata sanctus est Lorem ipsum dolor sit amet. Lorem ipsum dolor sit amet, consetetur sadipscing elitr, At accusam aliquyam diam diam dolore dolores duo eirmod eos erat, et nonumy sed tempor et et invidunt justo labore Stet clita ea et gubergren, kasd magna no rebum. sanctus sea sed takimata ut vero voluptua. est Lorem ipsum dolor sit amet. Lorem ipsum dolor sit amet, consetetur sadipscing elitr, sed diam nonumy eirmod tempor invidunt ut labore et dolore magna aliquyam erat.   \r\n" +
                        "\r\n" +
                        "Consetetur sadipscing elitr, sed diam nonumy eirmod tempor invidunt ut labore et dolore magna aliquyam erat, sed diam voluptua. At vero eos et accusam et justo duo dolores et ea rebum. Stet clita kasd gubergren, no sea takimata sanctus est Lorem ipsum dolor sit amet. Lorem ipsum dolor sit amet, consetetur sadipscing elitr, sed diam nonumy eirmod tempor invidunt ut labore et dolore magna aliquyam erat, sed diam voluptua. At vero eos et accusam et justo duo dolores et ea rebum. Stet clita kasd gubergren, no sea takimata sanctus est Lorem ipsum dolor sit amet. Lorem ipsum dolor sit amet, consetetur sadipscing elitr, sed diam nonumy eirmod tempor invidunt ut labore et dolore magna aliquyam erat, sed diam voluptua. At vero eos et accusam et justo duo dolores et ea rebum. Stet clita kasd gubergren, no sea takimata sanctus.   \r\n" +
                        "\r\n" +
                        "Lorem ipsum dolor sit amet, consetetur sadipscing elitr, sed diam nonumy eirmod tempor invidunt ut labore et dolore magna aliquyam erat, sed diam voluptua. At vero eos et accusam et justo duo dolores et ea rebum. Stet clita kasd gubergren, no sea takimata sanctus est Lorem ipsum dolor sit amet. Lorem ipsum dolor sit amet, consetetur sadipscing elitr, sed diam nonumy eirmod tempor invidunt ut labore et dolore magna aliquyam erat, sed diam voluptua. At vero eos et accusam et justo duo dolores et ea rebum. Stet clita kasd gubergren, no sea takimata sanctus est Lorem ipsum dolor sit amet. Lorem ipsum dolor sit amet, consetetur sadipscing elitr, sed diam nonumy eirmod tempor invidunt ut labore et dolore magna aliquyam erat, sed diam voluptua. At vero eos et accusam et justo duo dolores et ea rebum. Stet clita kasd gubergren, no sea takimata sanctus est Lorem ipsum dolor sit amet.   \r\n" +
                        "\r\n" +
                        "Duis autem vel eum iriure dolor in hendrerit in vulputate velit esse molestie consequat, vel illum dolore eu feugiat nulla facilisis at vero eros et accumsan et iusto odio dignissim qui blandit praesent luptatum zzril delenit augue duis dolore te feugait nulla facilisi. Lorem ipsum dolor sit amet, consectetuer adipiscing elit, sed diam nonummy nibh euismod tincidunt ut laoreet dolore magna aliquam erat volutpat.   \r\n" +
                        "\r\n" +
                        "Ut wisi enim ad minim veniam, quis nostrud exerci tation ullamcorper suscipit lobortis nisl ut aliquip ex ea commodo consequat. Duis autem vel eum iriure dolor in hendrerit in vulputate velit esse molestie consequat, vel illum dolore eu feugiat nulla facilisis at vero eros et accumsan et iusto odio dignissim qui blandit praesent luptatum zzril delenit augue duis dolore te feugait nulla facilisi.   \r\n" +
                        "\r\n" +
                        "Nam liber tempor cum soluta nobis eleifend option congue nihil imperdiet doming id quod mazim placerat facer possim assum. Lorem ipsum dolor sit amet, consectetuer adipiscing elit, sed diam nonummy nibh euismod tincidunt ut laoreet dolore magna aliquam erat volutpat. Ut wisi enim ad minim veniam, quis nostrud exerci tation ullamcorper suscipit lobortis nisl ut aliquip ex ea commodoe magna aliquyam erat, sed diam voluptua. At vero eos et accusam et justo duo dolores et ea rebum. | EOF";


                if (counter < 100 && out.startsWith("START") && s1.equals(s2)) {
                    //System.out.println("ok" + cycle);
                } else {
                    if(counter >= 100) {
                        System.out.println("### counter is highter than 100");
                    }

                    if(!out.startsWith("START")) {
                        System.out.println("### content does not start with START");
                    }

                    if (!s1.equals(s2)) {
                        FileOutputStream fos1 = null;
                        try {
                            fos1 = new FileOutputStream("G:/__logs/s1_"+getName()+".txt");

                            FileOutputStream fos2 = new FileOutputStream("G:/__logs/s2_"+getName()+".txt");

                            fos1.write(s1.getBytes());
                            fos2.write(s2.getBytes());

                            fos1.close();
                            fos2.close();
                        } catch (Exception e) {
                            throw new IllegalStateException(e);
                        }
                    }

                    System.out.println("nooop" + cycle);
                    throw new IllegalStateException();
                }

                try {
                    sleep(200);
                } catch (InterruptedException e) {
                    throw new IllegalStateException(e);
                }
            }

            session.close();
        }
    }
}

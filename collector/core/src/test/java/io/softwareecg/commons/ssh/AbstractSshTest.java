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

import org.apache.sshd.scp.server.ScpCommandFactory;
import org.apache.sshd.server.Environment;
import org.apache.sshd.server.ExitCallback;
import org.apache.sshd.server.SshServer;
import org.apache.sshd.server.channel.ChannelSession;
import org.apache.sshd.server.command.Command;
import org.apache.sshd.server.keyprovider.SimpleGeneratorHostKeyProvider;
import org.junit.After;
import org.junit.Before;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

/**
 * Base test class providing an embedded SSH server for testing.
 *
 * @since 01.12.13
 */
public class AbstractSshTest {

    protected static final String CORRECT_SSH_PASSWORD = "test";

    protected static final int TEST_SERVER_PORT = 22222;

    protected static final String LOREM_IPSUM = "Lorem ipsum dolor sit amet, consetetur sadipscing elitr, sed diam nonumy " +
            "eirmod tempor invidunt ut labore et dolore magna aliquyam erat, sed diam voluptua. At vero eos et accusam et " +
            "justo duo dolores et ea rebum. Stet clita kasd gubergren, no sea takimata sanctus est Lorem ipsum dolor sit " +
            "amet. Lorem ipsum dolor sit amet, consetetur sadipscing elitr, sed diam nonumy eirmod tempor invidunt ut labore" +
            " et dolore magna aliquyam erat, sed diam voluptua. At vero eos et accusam et justo duo dolores et ea rebum. Stet" +
            " clita kasd gubergren, no sea takimata sanctus est Lorem ipsum dolor sit amet.";

    protected SshServer sshServer;

    protected ConnectionFactory connectionFactory = ConnectionFactory.getInstance();

    protected SshConnection connection = connectionFactory.getConnection("127.0.0.1", TEST_SERVER_PORT, "root", CORRECT_SSH_PASSWORD);

    protected Map<String, List<TestingShell>> activeTestingShells = new HashMap<>();

    protected Map<String, FunctionExecutor> registeredFunctionExecutors = new HashMap<>();

    /**
     * Creates an instance of AbstractSshTest with a configured SSH-Server.
     */
    public AbstractSshTest() {
        sshServer = SshServer.setUpDefaultServer();
        sshServer.setPort(TEST_SERVER_PORT);
        sshServer.setKeyPairProvider(new SimpleGeneratorHostKeyProvider(Paths.get("hostkey.ser")));
        
        // Only accept password authentication with correct password
        sshServer.setPasswordAuthenticator((username, password, session) -> {
            if (password == null || password.isEmpty()) {
                return false;
            }
            return password.equals(CORRECT_SSH_PASSWORD);
        });

        sshServer.setPublickeyAuthenticator((username, key, session) -> true);

        sshServer.setShellFactory(channel -> new TestingShell());

        sshServer.setCommandFactory((channel, command) -> {
            if (command != null && command.startsWith("scp ")) {
                return new ScpCommandFactory().createCommand(channel, command);
            } else {
                return new TestingCommand(command);
            }
        });
    }

    @Before
    public void setup() throws Exception {
        registerHelloWorldFunction();
        registerLargeOutFunction();
        registerFakeTopFunction();
        registerLineBreakFunction();
    }

    @After
    public void tearDown() throws Exception {
        if (sshServer != null) {
            sshServer.stop(true);
        }
    }

    protected void addFunctionExecutor(String functionName, FunctionExecutor executor) {
        registeredFunctionExecutors.put(functionName, executor);
    }

    protected void connectAndAuthenticate() throws IOException, InterruptedException {
        sshServer.start();
        Thread.sleep(1000);
        connection.connect();
        assertThat(connection.isAuthenticated(), is(true));
    }

    protected SshConnection assumeRemoteSystemIsAvailable(String host, int port, String user, String password) {
        try {
            InetAddress inetAddress = InetAddress.getByName(host);
            boolean reachable = inetAddress.isReachable(2500);
            if (!reachable) {
                return null;
            }

            SshConnection con = ConnectionFactory.getInstance().getConnection(host, port, user, password);
            con.connect();
            return con;
        } catch (NoConnectionException | AuthenticationFailedException | UnknownHostException e) {
            return null;
        } catch (IOException e) {
            return null;
        }
    }

    private void registerHelloWorldFunction() {
        addFunctionExecutor("hello", serverOutputStream -> {
            serverOutputStream.write("hello world\r\n".getBytes());
            serverOutputStream.flush();
        });
    }

    private void registerLargeOutFunction() {
        addFunctionExecutor("get much", serverOutputStream -> {
            for (int i = 0; i < 5000; i++) {
                serverOutputStream.write((i + "|" + LOREM_IPSUM + LOREM_IPSUM + "\r\n").getBytes());
            }
            serverOutputStream.flush();
        });
    }

    private void registerFakeTopFunction() {
        addFunctionExecutor("faketop", serverOutputStream -> {
            for (int i = 0; i < 5; i++) {
                serverOutputStream.write(("< great linux top output with index:" + i + " >").getBytes());
                serverOutputStream.flush();
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    throw new IllegalStateException(e);
                }
            }
            serverOutputStream.flush();
        });
    }

    private void registerLineBreakFunction() {
        addFunctionExecutor("lineBreak", serverOutputStream -> {
            for (int i = 0; i < 1000; i++) {
                serverOutputStream.write((i + "|test\n").getBytes());
            }
            serverOutputStream.write(("\r\n").getBytes());
            serverOutputStream.write(("1000|test\r\n").getBytes());
            serverOutputStream.write(("1001|test\r\n").getBytes());
            serverOutputStream.flush();
        });
    }

    protected interface FunctionExecutor {
        void exec(OutputStream serverOutputStream) throws IOException;
    }

    protected class TestingCommand implements Command {

        private final String command;
        private InputStream in;
        private OutputStream out;
        private OutputStream err;
        private ExitCallback callback;
        private Thread thread;

        public TestingCommand(String command) {
            this.command = command;
        }

        @Override
        public void setInputStream(InputStream in) {
            this.in = in;
        }

        @Override
        public void setOutputStream(OutputStream out) {
            this.out = out;
        }

        @Override
        public void setErrorStream(OutputStream err) {
            this.err = err;
        }

        @Override
        public void setExitCallback(ExitCallback callback) {
            this.callback = callback;
        }

        @Override
        public void start(ChannelSession channel, Environment env) throws IOException {
            thread = new Thread(() -> {
                try {
                    if (registeredFunctionExecutors.containsKey(command)) {
                        registeredFunctionExecutors.get(command).exec(out);
                    }
                } catch (IOException e) {
                    throw new IllegalStateException(e);
                } finally {
                    callback.onExit(0);
                }
            });
            thread.start();
        }

        @Override
        public void destroy(ChannelSession channel) {
            if (thread != null) {
                thread.interrupt();
            }
        }
    }

    protected class TestingShell implements Command {

        private InputStream in;
        private OutputStream out;
        private OutputStream err;
        private ExitCallback callback;
        private Thread thread;
        private String userName = "";

        @Override
        public void setInputStream(InputStream in) {
            this.in = in;
        }

        @Override
        public void setOutputStream(OutputStream out) {
            this.out = out;
        }

        @Override
        public void setErrorStream(OutputStream err) {
            this.err = err;
        }

        @Override
        public void setExitCallback(ExitCallback callback) {
            this.callback = callback;
        }

        @Override
        public void start(ChannelSession channel, Environment env) throws IOException {
            userName = env.getEnv().get("USER");
            if (userName == null) userName = "unknown";
            
            if (!activeTestingShells.containsKey(userName)) {
                activeTestingShells.put(userName, new ArrayList<>());
            }
            activeTestingShells.get(userName).add(this);

            thread = new Thread(() -> {
                try {
                    while (in == null || out == null) {
                        Thread.sleep(100);
                    }

                    InputStreamReader clientInputReader = new InputStreamReader(in, StandardCharsets.UTF_8);
                    StringBuilder lastCommand = new StringBuilder();

                    while (true) {
                        int value = clientInputReader.read();
                        if (value < 0) break;

                        char s = (char) value;

                        if (s != '\n' && s != '\r') {
                            out.write(s);
                            out.flush();
                            lastCommand.append(s);
                        } else {
                            String cmd = lastCommand.toString();
                            if ("exit".equals(cmd)) {
                                return;
                            }

                            out.write("\r\n".getBytes());
                            out.flush();

                            if (registeredFunctionExecutors.containsKey(cmd)) {
                                registeredFunctionExecutors.get(cmd).exec(out);
                            }

                            lastCommand = new StringBuilder();
                        }
                    }
                } catch (Exception e) {
                    // Ignore
                } finally {
                    callback.onExit(0);
                }
            });
            thread.start();
        }

        @Override
        public void destroy(ChannelSession channel) {
            List<TestingShell> testingShells = activeTestingShells.get(userName);
            if (testingShells != null) {
                testingShells.remove(this);
            }
            if (thread != null) {
                thread.interrupt();
            }
        }
    }
}

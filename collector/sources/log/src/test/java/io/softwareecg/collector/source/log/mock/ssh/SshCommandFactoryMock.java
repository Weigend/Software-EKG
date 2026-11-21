package io.softwareecg.collector.source.log.mock.ssh;

import org.apache.sshd.server.Environment;
import org.apache.sshd.server.ExitCallback;
import org.apache.sshd.server.channel.ChannelSession;
import org.apache.sshd.server.command.Command;
import org.apache.sshd.server.command.CommandFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

/**
 * Answers every command a client sends with what the receiver makes of it, and ends it.
 *
 * @since 12.05.2014
 */
public class SshCommandFactoryMock implements CommandFactory {

    private final ISshCommandReceiver commandReceiver;

    public SshCommandFactoryMock(ISshCommandReceiver commandReceiver) {
        this.commandReceiver = commandReceiver;
    }

    @Override
    public Command createCommand(ChannelSession channel, final String command) {
        return new Command() {

            private OutputStream out;

            private ExitCallback exitCallback;

            @Override
            public void setInputStream(InputStream in) {
                // the commands read nothing
            }

            @Override
            public void setOutputStream(OutputStream out) {
                this.out = out;
            }

            @Override
            public void setErrorStream(OutputStream err) {
                // errors are part of the answer
            }

            @Override
            public void setExitCallback(ExitCallback callback) {
                this.exitCallback = callback;
            }

            @Override
            public void start(ChannelSession channel, Environment env) {
                // must not block the thread of the server
                Thread thread = new Thread(() -> {
                    try {
                        out.write(commandReceiver.execute(command).getBytes(StandardCharsets.UTF_8));
                        out.flush();
                        exitCallback.onExit(0);
                    } catch (IOException | RuntimeException e) {
                        exitCallback.onExit(1, String.valueOf(e.getMessage()));
                    }
                }, "ssh-command-mock");
                thread.setDaemon(true);
                thread.start();
            }

            @Override
            public void destroy(ChannelSession channel) {
                // nothing is kept
            }
        };
    }
}

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
package io.softwareecg.collector.source.log.mock.ssh;

import org.apache.sshd.server.Environment;
import org.apache.sshd.server.ExitCallback;
import org.apache.sshd.server.channel.ChannelSession;
import org.apache.sshd.server.command.Command;
import org.apache.sshd.server.command.CommandFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Created by martin.binder on 10.05.2014.
 */
public class SshCommandFactoryMock implements CommandFactory {
    private final ISshCommandReceiver commandReceiver;

    public SshCommandFactoryMock(ISshCommandReceiver commandReceiver) {
        this.commandReceiver = commandReceiver;
    }

    @Override
    public Command createCommand(ChannelSession channel, final String command) {

        return new Command() {
            private boolean shutdown = false;
            private InputStream in;
            private OutputStream out;
            private OutputStream err;
            private ExitCallback exitCallback;

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
                this.exitCallback = callback;
            }

            @Override
            public void start(ChannelSession channel, Environment env) throws IOException {
                while(!shutdown) {
                    out.write(commandReceiver.execute(command).getBytes());
                    out.flush();
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                exitCallback.onExit(0);
            }

            @Override
            public void destroy(ChannelSession channel) {
                shutdown = true;

                try {
                    out.close();
                } catch (IOException e) {
                    // do nothing
                }
            }
        };
    }
}

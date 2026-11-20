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

import org.apache.sshd.server.SshServer;
import org.apache.sshd.server.keyprovider.SimpleGeneratorHostKeyProvider;

import java.io.File;
import java.io.IOException;

/**
 * Created by martin.binder on 12.05.2014.
 */
public class SshServerMock {

    private final SshServer sshd;

    public SshServerMock(int port, ISshCommandReceiver commandReceiver, IUserAuthenticator userAuthenticator) {
        sshd = SshServer.setUpDefaultServer();
        sshd.setPort(port);

        //for ssl handshake - not for user auth!
        sshd.setKeyPairProvider(new SimpleGeneratorHostKeyProvider(new File("hostkey.ser").toPath()));

        sshd.setCommandFactory(new SshCommandFactoryMock(commandReceiver));

        // Use the built-in password authenticator from SSHD 2.14.0
        sshd.setPasswordAuthenticator((username, password, session) ->
                userAuthenticator.authenticate(username, password));
    }

    public void start() throws IOException {
        sshd.start();
    }

    public void stop() throws InterruptedException, IOException {
        sshd.stop(true);
        Thread.sleep(50); // avoid deadlock see apache unit tests
    }

    public int getActiveSessionCount() {
        return sshd.getActiveSessions().size();
    }
}

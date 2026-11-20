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

import org.apache.sshd.common.util.buffer.Buffer;
import org.apache.sshd.server.auth.AbstractUserAuth;
import org.apache.sshd.server.auth.UserAuth;
import org.apache.sshd.server.auth.UserAuthFactory;
import org.apache.sshd.server.session.ServerSession;

/**
 * Connects the IUserAuthenticator with the apache sshd authentication mechanism.
 */
public class SshUserAuth extends AbstractUserAuth {

    private final IUserAuthenticator userAuthenticator;

    public SshUserAuth(IUserAuthenticator userAuthenticator) {
        super("password");
        this.userAuthenticator = userAuthenticator;
    }

    @Override
    protected Boolean doAuth(Buffer buffer, boolean b) {
        return userAuthenticator.authenticate(getSession().getUsername(), buffer.getString());
    }



    public static class Factory implements UserAuthFactory {
        private final IUserAuthenticator userAuthenticator;

        public Factory(IUserAuthenticator userAuthenticator) {
            this.userAuthenticator = userAuthenticator;
        }

        @Override
        public String getName() {
            return "password";
        }

        @Override
        public UserAuth createUserAuth(ServerSession session) {
            return new SshUserAuth(userAuthenticator);
        }
    }
}

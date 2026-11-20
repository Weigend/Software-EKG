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
package io.softwareecg.collector.source.unix.executor.ssh;

import io.softwareecg.commons.ssh.ExecSession;
import io.softwareecg.collector.source.unix.UnixSourceConfig;
import io.softwareecg.collector.source.unix.executor.AsyncCommandExecutor;

import java.io.IOException;

/**
 * Asynchronous implementation of the ssh-command-executor.
 * <p>
 * Holds open a connection to the remote system for continuing output.
 */
public class AsyncSshExecutor extends AbstractSshExecutor implements AsyncCommandExecutor {

    /**
     * the maximum amount of milliseconds the CounterSource waits for
     * the first data of the terminal output
     */
    private static final int INITIAL_TIMEOUT = 10_000;

    /**
     * SSH-Network session.
     */
    private ExecSession session;

    /**
     * We overwrite the standard 'executeRemoteSynchronous' method because we need async session
     * that permanent proceed the iostat output without interrupt the stream or execute
     * the unix command (iostat) multiple times
     *
     * @param terminalCommand the iostat command with arguments
     * @param sourceConfig a config that provides settings and credentials that will used to fetch the Unix metrics.
     * @return the terminal output, maybe blank on first call
     * @throws java.io.IOException if the connection fails or the data cannot send correctly
     */
    @Override
    public String executeCommand(String terminalCommand, UnixSourceConfig sourceConfig) throws IOException {
        if (sshConnection == null) {
            createConnection(sourceConfig);
        }

        if (session == null || !session.isOpen()) {
            session = sshConnection.openAsynchronousExecSession(terminalCommand);
            session = session.executeCommand();
            session.waitForData(INITIAL_TIMEOUT);
        }

        if (session.hasErrorOutput()) {
            logger.error("received an error message from SSH terminal:\n" + session.getErrorOutput());
        }

        return session.getFullStdOutput();
    }

    /**
     * Explicitly close the connection.
     */
    @Override
    public void close() {

        if (session != null && session.isOpen()) {
            session.close();
            session = null;
        }

        if (sshConnection != null) {
            sshConnection.close();
            sshConnection = null;
        }
    }

}

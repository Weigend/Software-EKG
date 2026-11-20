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

import io.softwareecg.collector.ssh.ExecSession;
import io.softwareecg.collector.source.unix.UnixSourceConfig;
import io.softwareecg.collector.source.unix.executor.CommandExecutor;

import java.io.IOException;

/**
 * Synchronous implementation of the ssh-executor.
 */
public class SyncSshExecutor extends AbstractSshExecutor implements CommandExecutor {

    @Override
    public String executeCommand(String terminalCommand, UnixSourceConfig sourceConfig) throws IOException{
        if (sshConnection == null) {
            createConnection(sourceConfig);
        }

        try {
            ExecSession execSession = sshConnection.openSynchronousExecSession(terminalCommand);
            String output = execSession.executeCommand().getFullStdOutput();

            // we closed the terminal session because each
            // session can only execute only one command
            execSession.close();
            return output;
        } catch (Exception e) {
            // Connection is stale or channels exhausted — close and reconnect on next call
            logger.warn("SSH execution failed on {}, will reconnect: {}", sourceConfig.getHost(), e.getMessage());
            try {
                sshConnection.close();
            } catch (Exception closeEx) {
                logger.debug("Error closing stale SSH connection", closeEx);
            }
            sshConnection = null;
            throw new IOException("SSH command failed on " + sourceConfig.getHost(), e);
        }
    }
}

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

import io.softwareecg.commons.ssh.ConnectionFactory;
import io.softwareecg.commons.ssh.SshConnection;
import io.softwareecg.collector.common.LoggerFactory;
import io.softwareecg.collector.source.unix.UnixSourceConfig;
import org.slf4j.Logger;

/**
 * Abstract class for all derived SSH executors that provides
 * the common functionality of SSH connection handling.
 *
 * @since 03.05.2014
 */
public class AbstractSshExecutor {

    /**
     * The logger used by all executors
     */
    protected final Logger logger = LoggerFactory.create();

    /**
     * SSH Connection to be used for connecting the remote host.
     */
    protected SshConnection sshConnection = null;

    /**
     * Creates a new SSH connection that can used for further action like
     * command line calls at the remote shell.
     *
     * @param sourceConfig a config that provides settings and credentials that will used to fetch the Unix metrics.
     */
    protected void createConnection(UnixSourceConfig sourceConfig) {

        ConnectionFactory factory = ConnectionFactory.getInstance();

        sshConnection = factory.getConnection(sourceConfig.getHost(), sourceConfig.getPort(),
                sourceConfig.getUsername(), sourceConfig.getPassword());
    }
}

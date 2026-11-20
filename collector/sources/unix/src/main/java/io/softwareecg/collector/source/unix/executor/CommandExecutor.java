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
package io.softwareecg.collector.source.unix.executor;

import io.softwareecg.collector.source.unix.UnixSourceConfig;

import java.io.IOException;

/**
 * A command executor is used to execute a shell command either on the local system or on a remote system.
 *
 * @since 03.05.2014
 */
public interface CommandExecutor {

    /**
     * Execute the given command.
     *
     * @param terminalCommand the command to be executed.
     * @param sourceConfig a config that provides settings and credentials that will used to fetch the Unix metrics.
     * @return output of the command.
     * @throws IOException on failure during communication.
     */
    String executeCommand(String terminalCommand, UnixSourceConfig sourceConfig) throws IOException;
}
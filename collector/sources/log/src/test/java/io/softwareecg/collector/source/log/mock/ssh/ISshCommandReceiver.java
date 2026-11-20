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

/**
 * Interface for executing received commands.
 *
 * This is the ssh server-side of the execution!
 *
 */
public interface ISshCommandReceiver {

    /**
     * Execute a received command.
     * @param command the command.
     * @return output.
     */
    String execute(String command);
}

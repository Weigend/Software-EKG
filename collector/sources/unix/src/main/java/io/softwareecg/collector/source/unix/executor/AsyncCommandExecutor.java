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

/**
 * Interface for {@link CommandExecutor}s that asynchronous.
 * The connection is kept open until {@link #close} is called.
 *
 * Created by l.steens on 24.07.15.
 */
public interface AsyncCommandExecutor extends CommandExecutor {

    /**
     * Closes the connection.
     */
    void close();
}

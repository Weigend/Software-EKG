//______________________________________________________________________________
//
//          Project:    Software EKG
//______________________________________________________________________________
//
//       created by:    Johannes Weigend
//       creation date: 2011
//       changed by:    $Author: weigend $
//       change date:   $Date: 2010-03-04 17:28:30 +0100 (Do, 04 Mrz 2010) $
//       revision:      $Revision: 421 $
//       description:   Access performance counter.
//______________________________________________________________________________
//
//        Copyright:    Weigend AM
//______________________________________________________________________________
package io.softwareecg.collector.source.unix;

import io.softwareecg.collector.source.unix.analyzer.ITerminalOutAnalyzer;
import io.softwareecg.collector.source.unix.analyzer.TopAnalyzer;
import io.softwareecg.collector.source.unix.executor.CommandExecutor;
import io.softwareecg.collector.source.unix.executor.local.SyncLocalExecutor;
import io.softwareecg.collector.source.unix.executor.ssh.SyncSshExecutor;

/**
 * Unix counter per SSH.
 * Currently no wildcard support.
 * For usage see JUnit-Test.
 */

@SuppressWarnings("unused") // constructors used via reflection and class path scan
public class UnixTopCounterSource extends AbstractUnixCounterSource {

    /** The Constant TOP_COMMAND. */
    /**
     * Without a terminal top cuts every line at 80 characters, which leaves little of a command
     * line to find a process by. The width is set through the environment, which every version
     * of top understands.
     */
    private static final String TOP_COMMAND = "env COLUMNS=512 top -c -n 1 -b";
    /**
     * The string buffer to build the terminal command
     */
    private final StringBuffer terminalCommand = new StringBuffer();


    /**
     * Simple constructor.
     * Uses the sync-ssh executor as default.
     */
    public UnixTopCounterSource() {
        this(new SyncSshExecutor());
        fileNamePrefix = "unix-top";
    }

    /**
     * Constructor with custom executor.
     *
     * @param remoteExecutor remote executor to be used for command-execution on the remote host.
     */
    public UnixTopCounterSource(CommandExecutor remoteExecutor) {
        this.remoteExecutor = remoteExecutor;
        this.localExecutor = new SyncLocalExecutor();
    }

    @Override
    protected ITerminalOutAnalyzer createTerminalAnalyzer() {
        return new TopAnalyzer(null, listeners);
    }

    @Override
    protected String getTerminalCommand(String additionalArguments) {
        if(terminalCommand.length() == 0){
            terminalCommand.append(TOP_COMMAND).append(' ').append(additionalArguments);
        }
        return terminalCommand.toString();

    }

    @Override
    public String getId() {
        return "unix";
    }
}

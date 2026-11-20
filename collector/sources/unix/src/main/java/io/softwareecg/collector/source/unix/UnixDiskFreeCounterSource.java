/*
 * ______________________________________________________________________________
 *
 * Project: Design for Diagnosability
 * ______________________________________________________________________________
 *
 * Copyright: (c) Weigend AM, all rights reserved
 * ______________________________________________________________________________
 */
package io.softwareecg.collector.source.unix;

import io.softwareecg.collector.source.unix.analyzer.DiskFreeAnalyzer;
import io.softwareecg.collector.source.unix.analyzer.ITerminalOutAnalyzer;
import io.softwareecg.collector.source.unix.executor.CommandExecutor;
import io.softwareecg.collector.source.unix.executor.local.SyncLocalExecutor;
import io.softwareecg.collector.source.unix.executor.ssh.SyncSshExecutor;

/**
 */
public class UnixDiskFreeCounterSource extends AbstractUnixCounterSource {

    /**
     * The Constant DF_COMMAND.
     */
    private static final String DF_COMMAND = "df -m";
    /**
     * The string buffer to build the terminal command
     */
    private final StringBuffer terminalCommand = new StringBuffer();

    /**
     * Simple constructor.
     * Uses the sync-ssh executor as default.
     */
    public UnixDiskFreeCounterSource() {
        this(new SyncSshExecutor());
        fileNamePrefix = "unix-diskfree";
    }

    /**
     * Constructor of the unix disk free counter for a given excutor
     *
     * @param remoteExecutor - remote executor to be used for command-execution on the remote host
     */
    public UnixDiskFreeCounterSource(CommandExecutor remoteExecutor) {
        this.remoteExecutor = remoteExecutor;
        this.localExecutor = new SyncLocalExecutor();
    }

    @Override
    protected ITerminalOutAnalyzer createTerminalAnalyzer() {
        return new DiskFreeAnalyzer();
    }

    @Override
    protected String getTerminalCommand(String additionalArguments) {
        if (terminalCommand.length() == 0) {
            terminalCommand.append(DF_COMMAND).append(' ').append(additionalArguments);
        }
        return terminalCommand.toString();
    }

    @Override
    public String getId() {
        return "unixdf";
    }
}

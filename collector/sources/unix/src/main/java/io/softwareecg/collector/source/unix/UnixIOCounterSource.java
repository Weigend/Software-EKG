package io.softwareecg.collector.source.unix;

import io.softwareecg.collector.config.CounterDescriptor;
import io.softwareecg.collector.source.unix.analyzer.IOStatAnalyzer;
import io.softwareecg.collector.source.unix.analyzer.ITerminalOutAnalyzer;
import io.softwareecg.collector.source.unix.executor.AsyncCommandExecutor;
import io.softwareecg.collector.source.unix.executor.CommandExecutor;
import io.softwareecg.collector.source.unix.executor.local.AsyncLocalExecutor;
import io.softwareecg.collector.source.unix.executor.ssh.AsyncSshExecutor;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;

/**
 * Collector to monitor the output of the iostat unix process.
 *
 * @since 06.03.13
 */
public class UnixIOCounterSource extends AbstractUnixCounterSource {

    private static final String IOSTAT_COMMAND = "iostat";
    private static final String UPDATE_INTERVAL = "2";
    private static final String ARGS_WITH_NFS = "-xn";
    private static final String ARGS_NO_NFS = "-x";

    /**
     * Flag if NFS is supported by network filesystem (NFS) is supported by the operating system.
     */
    private boolean nfsSupported = true;

    /**
     * Flag if the support for NFS reports was checked or not
     */
    private Boolean nfsStatSupportChecked = false;

    /**
     * Default constructor that overrides the output name-appendix
     */
    @SuppressWarnings("unused") // default constructor is needed by java services mechanism
    public UnixIOCounterSource() {
        this(new AsyncSshExecutor());
        fileNamePrefix = "unix-io";
    }

    /**
     * Constructor with custom executor.
     *
     * @param remoteExecutor remote executor to be used for command-execution on the remote host.
     */
    public UnixIOCounterSource(CommandExecutor remoteExecutor) {
        this.remoteExecutor = remoteExecutor;
        this.localExecutor = new AsyncLocalExecutor();
    }

    @Override
    public String getId() {
        return "unixio";
    }

    @Override
    protected ITerminalOutAnalyzer createTerminalAnalyzer() {
        return new IOStatAnalyzer(listeners);
    }

    @Override
    protected String getTerminalOutput(CounterDescriptor descriptor) throws IOException {
        String result = super.getTerminalOutput(descriptor);

        // Check if NFS is supported after the first execution:
        if (!nfsStatSupportChecked) {
            nfsStatSupportChecked = true;

            // if non of the metrics header available the IOStat with NFS isn't supported (a usage screen will be shown)
            if (!result.contains("avgrq-sz")) {
                this.nfsSupported = false;

                // Close all async executors: We need a new connection because we use a different command
                if (this.remoteExecutor instanceof AsyncCommandExecutor) {
                    ((AsyncCommandExecutor) remoteExecutor).close();
                }
                if (this.localExecutor instanceof AsyncCommandExecutor) {
                    ((AsyncCommandExecutor) localExecutor).close();
                }

                // Execute command again without NFS flag:
                result = super.getTerminalOutput(descriptor);
            }
        }

        return result;
    }

    @Override
    protected String getTerminalCommand(String additionalArguments) {
        StringBuilder cmd = new StringBuilder();
        cmd.append(IOSTAT_COMMAND).append(' ').append((this.nfsSupported ? ARGS_WITH_NFS : ARGS_NO_NFS));
        if (StringUtils.isNotBlank(additionalArguments)) {
            cmd.append(' ').append(additionalArguments);
        }
        cmd.append(' ').append(UPDATE_INTERVAL);
        return cmd.toString();
    }
}

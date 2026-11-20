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
package io.softwareecg.collector.source.log;

import io.softwareecg.collector.ssh.ConnectionFactory;
import io.softwareecg.collector.ssh.ExecSession;
import io.softwareecg.collector.ssh.SshConnection;
import io.softwareecg.collector.common.LoggerFactory;
import io.softwareecg.collector.common.Registry;
import io.softwareecg.collector.config.CounterDescriptor;
import io.softwareecg.collector.dt.MetricDefinition;
import io.softwareecg.collector.repository.RepositoryFactory;
import io.softwareecg.collector.repository.StaticTimeSeriesRepository;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.NotImplementedException;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Map;

/**
 * Helper to collect all data from a ssh connection.
 *
 * @since 12.05.2014
 */
public class SshTailLogger extends Thread {

    /**
     * Our logger
     */
    private static final Logger LOGGER = LoggerFactory.create();

    /**
     * Wait until retry in ms.
     */
    private static final int RETRY_WAIT_IN_MS = 1000 * 60 * 2;

    /**
     * SSH server port.
     */
    private static final int DEFAULT_SSH_PORT = 22;

    /**
     * Timeout in ms to wait for initial data from the async exec session.
     */
    private static final int INITIAL_DATA_TIMEOUT_MS = 10_000;

    /**
     * Poll interval in ms for reading output from the async exec session.
     */
    private static final int POLL_INTERVAL_MS = 1000;

    /**
     * The magic tail makes the job to get all the logfile data even the file was rotated and a new one is created.
     */
    private static final String TAIL_COMMAND = "tail --retry --follow=name";

    /**
     * a descriptor that provides endpoint config for the counter source and the metrics to fetch
     */
    private final CounterDescriptor descriptor;

    /**
     * The repository to write our logdata.
     */
    private final StaticTimeSeriesRepository repository;

    /**
     * The original logfile name.
     */
    private final String logfileName;

    /**
     * The amount of loglines which are read and written.
     */
    private final long logLinesWritten = 0;

    /**
     * The ssh-port to be used.
     */
    private static int sshPort = DEFAULT_SSH_PORT;

    private volatile boolean shutdown = false;

    /**
     * Error flag.
     */
    private transient boolean error = false;

    private SshConnection sshConnection = null;
    private ExecSession execSession = null;
    private InputStream localIn = null;
    private Process proc = null;

    private final Thread shutdownHook = new ShutdownHook();

    /**
     * Create a tail logger.
     *
     * @param descriptor holds host/counter/process/source.
     */
    public SshTailLogger(CounterDescriptor descriptor) {
        setDaemon(true);
        this.descriptor = descriptor;
        this.logfileName = descriptor.getProcessMetricsSource().replace("${HOSTNAME}", descriptor.getNodeName());

        RepositoryFactory repositoryFactory = Registry.getDefault().lookup(RepositoryFactory.class);
        this.repository = repositoryFactory.createStaticRepository(descriptor);

        //Add shutdown hook, to ensure that the session will close
        Runtime.getRuntime().addShutdownHook(shutdownHook);
    }

    /**
     * Shut down the source.
     */
    public void shutdown() {
        LOGGER.info("Shutdown Hook");
        // shutdownHook.start();
    }

    /**
     * ShutdownHook thread class
     */
    public class ShutdownHook extends Thread {
        @Override
        public void run() {
            LOGGER.info("Shutdown Hook detected. Closing stream to: " + logfileName);

            //close remote connections - if they are existing
            if (execSession != null) {
                execSession.close();
            }
            if (sshConnection != null) {
                sshConnection.close();
            }
            shutdown = true;
            LOGGER.error(captureThreadDump());
        }

        /**
         * Fetches the stack traces of all current threads and produces a printable string.
         * @return String with thread stack traces, never null.
         */
        public String captureThreadDump() {
            StringBuilder builder = new StringBuilder();
            for (Map.Entry<Thread, StackTraceElement[]> entry : Thread.getAllStackTraces().entrySet()) {
                builder.append(entry.getKey());
                builder.append("\r\n");

                for (StackTraceElement traceElement : entry.getValue()) {
                    builder.append(' ');
                    builder.append(traceElement);
                    builder.append("\r\n");
                }

            }

            return builder.toString();

        }
    }




    /**
     * Override the ssh-port.
     *
     * @param sshPort the ssh-port.
     */
    public static void setSshPort(int sshPort) {
        SshTailLogger.sshPort = sshPort;
    }

    /**
     * Executes a command as a local process.
     * The method returns after the process has started.
     *
     * @param command command to execute
     * @throws java.io.IOException if something goes wrong
     */
    protected void executeLocal(String command) throws IOException {
        ProcessBuilder pb = new ProcessBuilder(command.split(" ")).redirectErrorStream(true);
        proc = pb.start();
        //read from process stdout
        localIn = proc.getInputStream();
    }

    /**
     * Returns the error state of the logger.
     *
     * @return the error-state.
     */
    public boolean isError() {
        return error;
    }

    /**
     * Get the total amount of written lines.
     *
     * @return amount of written lines.
     */
    public long getLogLinesWritten() {
        return logLinesWritten;
    }

    private void executeRemote() throws Exception {
        ConnectionFactory factory = ConnectionFactory.getInstance();
        sshConnection = factory.getConnection(
                descriptor.getNodeAddress(), sshPort,
                descriptor.getUsername(), descriptor.getPassword());

        String command = TAIL_COMMAND + " " + logfileName;
        execSession = sshConnection.openAsynchronousExecSession(command);
        execSession.executeCommand();

        // Wait for initial data to confirm the connection works
        execSession.waitForData(INITIAL_DATA_TIMEOUT_MS);

        if (execSession.hasErrorOutput()) {
            LOGGER.error(execSession.getErrorOutput());
            return;
        }

        LOGGER.info("SSH connection to '" + descriptor.getNodeAddress() + "' established.");
        error = false;

        // Continuously poll for new lines
        while (!isInterrupted() && !shutdown && execSession.isOpen()) {
            Collection<String> lines = execSession.getLineBasedStdOutput();
            for (String line : lines) {
                parseLine(line);
            }

            if (execSession.hasErrorOutput()) {
                LOGGER.error("received an error message from SSH terminal:\n" + execSession.getErrorOutput());
            }

            Thread.sleep(POLL_INTERVAL_MS);
        }
    }


    /**
     * The main loop tries to connect or reconnect on any error. Only a Thread.interrupt() stops this loop.
     */
    public void run() {
        LOGGER.info("Main loop starting... Thread " + getId());
        while (!isInterrupted() && !shutdown) {
            try {
                if (HostNameUtil.isLocalhost(descriptor.getNodeAddress())) {
                    String command = TAIL_COMMAND + " " + logfileName;
                    executeLocal(command);

                    org.apache.commons.io.LineIterator lines = IOUtils.lineIterator(localIn, "ISO-8859-1");
                    while (lines.hasNext()) {
                        parseLine(lines.nextLine());
                    }
                } else {
                    executeRemote();
                }
            } catch (Exception e) {
                LOGGER.error("can't execute command", e);
                error = true;
                if (shutdown) {
                    break;
                }
                try {
                    LOGGER.info("Retrying to connect to host: '" + descriptor.getNodeAddress() + "' in " + RETRY_WAIT_IN_MS + " milliseconds.");
                    Thread.sleep(RETRY_WAIT_IN_MS);
                    // todo: if we do a reconnect make sure we do not tail the same data twice...

                } catch (InterruptedException e1) {
                    shutdown = true;
                    LOGGER.info("caught InterruptedException, exiting now. Thread " + getId());
                    break; // stop
                }
            } finally {
                IOUtils.closeQuietly(localIn);
                if (execSession != null) {
                    execSession.close();
                    execSession = null;
                }
                if (sshConnection != null) {
                    sshConnection.close();
                    sshConnection = null;
                }
            }
        }
        LOGGER.info("Main loop is exiting... - Thread " + getId());
    }

    /**
     * Set the shutdown flag of the logger.
     */
    public void initiateShutdown() {
        shutdown = true;
        try {
            if (localIn != null) {
                localIn.close();
            }
            if (proc != null) {
                proc.destroy();
            }
            if (execSession != null) {
                execSession.close();
            }
            if (sshConnection != null) {
                sshConnection.close();
            }
        } catch (Exception e) {
            LOGGER.info("Could not close resources during shutdown");
        }
    }

    private void parseLine(String line) {
        for (MetricDefinition metricDefinition : descriptor.getMetricDefinitions()) {
            if (line.matches(metricDefinition.getDefinition())) {
                LOGGER.debug("tail line matches: " + line);
                byte[] writeLine = (line + IOUtils.LINE_SEPARATOR).getBytes();

                // todo - implement this
                throw new NotImplementedException("This part isn't implemented at the moment");

//                if (repository instanceof LogStorage) {
//                    ((LogStorage)repository).writeRawLogdata(writeLine, writeLine.length);
//                }

                //logLinesWritten++;
                //return;
            }
        }
    }
}


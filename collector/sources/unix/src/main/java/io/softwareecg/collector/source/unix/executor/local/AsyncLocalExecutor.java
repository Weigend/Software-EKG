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
package io.softwareecg.collector.source.unix.executor.local;

import io.softwareecg.collector.common.LoggerFactory;
import io.softwareecg.collector.source.unix.UnixSourceConfig;
import io.softwareecg.collector.source.unix.executor.AsyncCommandExecutor;
import org.slf4j.Logger;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Executes a command as a local process. Use this class for commands that do not terminate themselves but continuously
 * write to stdout.
 * <p>
 * Created by l.steens on 22.07.15.
 */
public class AsyncLocalExecutor implements AsyncCommandExecutor {

    private static final Logger LOGGER = LoggerFactory.create();

    /**
     * the maximum amount of milliseconds to wait for the first data of the terminal output.
     */
    private static final int INITIAL_TIMEOUT = 1_000;

    private Process process = null;
    private String stdOutputLast = "";
    private StdOutReader outputReaderThread = null;

    @Override
    public String executeCommand(String terminalCommand, UnixSourceConfig sourceConfig) throws IOException {
        if (this.process == null) {
            LOGGER.debug("Starting process \"{}\" on local machine.", terminalCommand);
            ProcessBuilder pb = new ProcessBuilder(terminalCommand.split(" "));
            this.process = pb.start();

            this.outputReaderThread = new StdOutReader(this.process, terminalCommand);
            this.outputReaderThread.start();

            this.waitForData(INITIAL_TIMEOUT);
        }

        return this.getOutput();
    }

    @Override
    public void close() {
        if (this.process != null) {
            this.process.destroy();
            this.process = null;
        }
        if (this.outputReaderThread != null) {
            this.outputReaderThread = null;
        }
    }

    private String getOutput() throws IOException {
        String output = this.outputReaderThread.getData();
        if (!output.isEmpty()) {
            this.stdOutputLast = output;
        }
        return this.stdOutputLast;
    }

    private void waitForData(final int timeoutInMillis) {
        int maxAmountOfCycles = timeoutInMillis / 10;
        for (int cycle = 0; cycle < maxAmountOfCycles; cycle++) {
            if (this.outputReaderThread == null || this.outputReaderThread.hasData()) {
                return;
            }

            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                throw new IllegalStateException(e);
            }
        }
    }

    private static class StdOutReader extends Thread {
        private final Process process;
        private final String processLabel;
        private final ByteArrayOutputStream outputBuffer = new ByteArrayOutputStream();

        /**
         * Creates a new StdOutReader.
         *
         * @param process      the process
         * @param processLabel a label for the process. used for log messages.
         */
        public StdOutReader(Process process, String processLabel) {
            this.process = process;
            this.processLabel = processLabel;
        }

        /**
         * Returns the output of the process.
         *
         * @return a string containing  the output to stdout
         */
        public String getData() {
            synchronized (outputBuffer) {
                String outStr = outputBuffer.toString();
                outputBuffer.reset();
                return outStr;
            }
        }

        /**
         * Returns if new data is available since the last call to {@link #getData()}.
         *
         * @return true if new data is available, otherwise false.
         */
        public boolean hasData() {
            return this.outputBuffer.size() > 0;
        }

        @Override
        public void run() {
            //read from process stdout
            InputStream stdin = this.process.getInputStream();
            byte[] buffer = new byte[8192];
            int read;

            try {
                while ((read = stdin.read(buffer)) > 0) {
                    LOGGER.trace("{} bytes read from process {}.", read, processLabel);
                    synchronized (outputBuffer) {
                        outputBuffer.write(buffer, 0, read);
                    }
                }
            } catch (IOException e) {
                LOGGER.error(String.format("Error reading from process %s.", processLabel), e);
            }
        }

    }
}

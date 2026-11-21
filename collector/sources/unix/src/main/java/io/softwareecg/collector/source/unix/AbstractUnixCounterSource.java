package io.softwareecg.collector.source.unix;


import io.softwareecg.collector.common.LoggerFactory;
import io.softwareecg.collector.config.CounterDescriptor;
import io.softwareecg.collector.dt.MetricDefinition;
import io.softwareecg.collector.source.CounterWildcardSupport;
import io.softwareecg.collector.source.ExpandWildcardResponse;
import io.softwareecg.collector.source.ExpandWildcardResultType;
import io.softwareecg.collector.source.AbstractCounterSource;
import io.softwareecg.collector.source.unix.analyzer.AnalyzerStringUtils;
import io.softwareecg.collector.source.unix.analyzer.ITerminalOutAnalyzer;
import io.softwareecg.collector.source.unix.executor.CommandExecutor;
import io.softwareecg.collector.source.unix.executor.local.SyncLocalExecutor;
import io.softwareecg.collector.source.unix.executor.ssh.SyncSshExecutor;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;

import java.io.*;
import java.util.*;

import static org.apache.commons.lang3.StringUtils.defaultString;

/**
 * Base class of all collector that are based on unix terminal outputs
 *
 * @since 06.03.2013
 */
abstract class AbstractUnixCounterSource extends AbstractCounterSource implements CounterWildcardSupport {

    /**
     * Our logger.
     */
    protected final Logger logger = LoggerFactory.create();

    /**
     * The last update of the ssh result.
     */
    protected Map<String, Date> source2LastUpdate = new HashMap<>();

    /**
     * The Class which parses the top output.
     */
    protected ITerminalOutAnalyzer analyzer = createTerminalAnalyzer();

    /**
     * Executor used for execute remote commands.
     */
    protected CommandExecutor remoteExecutor;

    /**
     * Executor used to execute commands on the local machine.
     */
    protected CommandExecutor localExecutor;

    /**
     * The filename prefix that will be used at debug output files with the terminal output content.
     * It could overwritten by deriving classes to get a more readable name
     */
    protected String fileNamePrefix = getClass().getSimpleName();


    @Override
    public double querySingleCounter(CounterDescriptor descriptor, MetricDefinition metricDefinition) {
        try {
            updateAnalyzerSourceIfRequired(descriptor);
            return analyzer.getFilteredCounter(metricDefinition.getDefinition(), metricDefinition.getRegexPostFilterExp());
        } catch (Exception e) {
            logger.error(String.format("Could not query metricDefinition %s.", metricDefinition), e);
            return -1.0;
        }
    }

    /* (non-Javadoc)
     * @see io.softwareecg.collector.source.CounterWildcardSupport#replaceWildcardCounter(CounterDescriptor)
     */
    @Override
    public ExpandWildcardResponse replaceWildcardCounter(CounterDescriptor descriptor) {

        //ssh libraries are not thread safe
        synchronized (AbstractUnixCounterSource.class) {
            try {
                updateAnalyzerSourceIfRequired(descriptor);

                Set<MetricDefinition> expandedMetricDefinition = new HashSet<>();

                MetricDefinition[] originalMetricDefinition = descriptor.getMetricDefinitions();
                for (MetricDefinition metricDefinition : originalMetricDefinition) {

                    if (metricDefinition.useWildcard()) {
                        expandedMetricDefinition.addAll(analyzer.expandCounter(metricDefinition));
                    } else {
                        expandedMetricDefinition.add(metricDefinition);
                    }
                }

                if (expandedMetricDefinition.isEmpty()) {
                    return new ExpandWildcardResponse(ExpandWildcardResultType.METRIC_NOT_FOUND);
                }

                // The new group with a expanded counter list
                return new ExpandWildcardResponse(ExpandWildcardResultType.SUCCESSFUL_EXPANDED,
                        expandedMetricDefinition.toArray(new MetricDefinition[0]));

            } catch (IOException e) {
                // the host did not answer or refused the login: the job says so and keeps trying
                logger.warn("Unable to read the counters of {}: {}", descriptor.getNodeAddress(), rootMessage(e));
                return new ExpandWildcardResponse(ExpandWildcardResultType.METRIC_SOURCE_NOT_AVAILABLE,
                        "Host " + descriptor.getNodeAddress() + " cannot be read: " + rootMessage(e));
            } catch (Exception e) {
                logger.error("unable to expand wildcard counter", e);

                return new ExpandWildcardResponse(ExpandWildcardResultType.ERROR);
            }
        }
    }

    private static String rootMessage(Throwable e) {
        Throwable root = e;
        while (root.getCause() != null && root.getCause() != root) {
            root = root.getCause();
        }
        return root.getMessage() != null ? root.getMessage() : root.getClass().getSimpleName();
    }

    /**
     * Returns a (new) instance of ITerminalOutAnalyzer that belongs
     * to the specific type of UnixCounterSource and extract data from
     * the Terminal output
     *
     * @return a ITerminalOutAnalyzer instance
     */
    protected abstract ITerminalOutAnalyzer createTerminalAnalyzer();

    /**
     * Returns the terminal (shell) command that will be execute
     * to get the requested output
     *
     * @param optionalParams optional parameters that will be passed to the source.
     * @return the command that will be send to the terminal
     */
    protected abstract String getTerminalCommand(String optionalParams);

    /*
     * Executes a command as a local process
     * The method returns after the process has finished.
     *
     * @param command command to execute
     * @return the combined stdOut and errorOut of the process
     * @throws IOException if something goes wrong

    protected String executeLocal(String command) throws IOException {
        ProcessBuilder pb = new ProcessBuilder(command.split(" "));
        //Set the width for the virtual console.
        pb.environment().put("COLUMNS","9999");
        Process proc = pb.start();

        StringBuilder sb = new StringBuilder();

        //read from process stdout
        InputStream stdin = proc.getInputStream();
        byte[] buffer = new byte[8192];
        int read;
        while ((read = stdin.read(buffer)) > 0) {
            sb.append(new String(buffer, 0, read));
        }

        return sb.toString();
    }*/

    /**
     * Update top analyzer source if required.
     *
     * @param descriptor a descriptor that provides endpoint config for the counter source and the metrics to fetch
     * @throws java.io.IOException Signals that an I/O exception has occurred.
     */
    protected void updateAnalyzerSourceIfRequired(CounterDescriptor descriptor) throws IOException {

        Date lastUpdate = source2LastUpdate.get(descriptor.getProcessMetricsSource());
        if (lastUpdate == null || (lastUpdate.getTime() + descriptor.getMetricFetchInterval()) < new Date().getTime()) {
            String result = getTerminalOutput(descriptor);

            if (isDebugOutputEnabled()) {
                storeFile(descriptor.getNodeAddress().replaceAll("[^0-9a-zA-Z_\\\\.]", ""), result);
            }
            String[] lines = AnalyzerStringUtils.extractLines(result);
            analyzer.setTerminalOutput(lines);
            source2LastUpdate.put(descriptor.getProcessMetricsSource(), new Date()); // now
        }
    }

    protected String getTerminalOutput(CounterDescriptor descriptor) throws IOException {
        UnixSourceConfig sourceConfig = UnixSourceConfig.fromDescriptor(descriptor);

        CommandExecutor commandExecutor = getCommandExecutor(sourceConfig);
        String shellCommand = getTerminalCommand(sourceConfig);

        logger.debug("Executing command \"{}\" on host {} using executor {}.",
                shellCommand, sourceConfig.getHost(), commandExecutor.getClass().getSimpleName());

        String result = commandExecutor.executeCommand(shellCommand, sourceConfig);

        logger.debug("Done Executing command \"{}\" on host {}. Result (first 200 characters)={}",
                shellCommand, sourceConfig.getHost(), StringUtils.abbreviate(result, 200));

        return result;
    }

    private CommandExecutor getCommandExecutor(UnixSourceConfig sourceConfig) {
        // 127.0.0.1 is used for tunneling - do not execute local. Only localhost is executed local.
        boolean executeLocal = StringUtils.startsWith(sourceConfig.getHost(), "localhost");

        // If data is read from a file we need a sync executor:
        if (StringUtils.isNotBlank(sourceConfig.getLogfilePath())) {
            return executeLocal ? new SyncLocalExecutor() : new SyncSshExecutor();
        }

        // Use either async or sync executor - the sublass decides:
        return executeLocal ? this.localExecutor : this.remoteExecutor;
    }

    private String getTerminalCommand(UnixSourceConfig sourceConfig) {
        return StringUtils.isBlank(sourceConfig.getLogfilePath())
                        ? getTerminalCommand(defaultString(sourceConfig.getAdditionalCommandArguments()))
                        : "cat " + sourceConfig.getLogfilePath();
    }

    private static final String DEBUG_OUTPUT_PROPERTY =
            "io.softwareecg.collector.source.unix.debugOutputEnabled";

    private boolean isDebugOutputEnabled() {
        return Boolean.parseBoolean(System.getProperty(DEBUG_OUTPUT_PROPERTY, "false"));
    }

    /**
     * Stores the result in a debug file under /tmp/.
     *
     * @param host   - the host.
     * @param result the result.
     */
    protected void storeFile(String host, String result) {
        File outputFile = new File("/tmp", fileNamePrefix + "-" + host + ".txt");

        try (BufferedReader reader = new BufferedReader(new StringReader(result));
             FileWriter fileWriter = new FileWriter(outputFile);
             PrintWriter writer = new PrintWriter(fileWriter)) {

            String line;
            while ((line = reader.readLine()) != null) {
                writer.println(line);
            }
            writer.flush();

        } catch (IOException e) {
            logger.warn("Can not store debug output to file: " + e.getMessage());
        }
    }

}

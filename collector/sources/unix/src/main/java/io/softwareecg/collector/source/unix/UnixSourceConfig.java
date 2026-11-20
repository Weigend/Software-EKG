package io.softwareecg.collector.source.unix;

import io.softwareecg.collector.config.CounterDescriptor;
import org.apache.commons.lang3.StringUtils;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Just a simple POJO to store config values that will used by the
 * SSH executors to fetch the Unix metrics.
 *
 * The config values are exploded, and with default data enriched
 * values that parsed from the main collector config.
 *
 * @since 16.08.2018
 */
public class UnixSourceConfig {

    private static final Pattern SIMPLE_HOST_ADDRESS_PATTERN =
            Pattern.compile("^([a-z19][a-z0-9\\\\.-_]+):([1-9][\\d]+)$", Pattern.CASE_INSENSITIVE);

    /**
     * The default SSH port that can used if the concrete port is missing
     */
    public static final int DEFAULT_SSH_PORT = 22;

    /**
     * The host that should used for the connection without port
     */
    private String host;

    /**
     * The TCP port that should used for the connection
     */
    private Integer port;

    /**
     * The username used to established the connection to the Unix-System
     */
    private String username;

    /**
     * The username used to established the connection to the Unix-System
     */
    private String password;

    /**
     * The file path used to store the output of the shell command (i.e. top).
     * for example: /usr/ekg_logs/top.txt
     */
    private String logfilePath;

    /**
     * Additional/optional call arguments that can/will used by the counter sources to fetch the data if specified.
     * For this the arguments will append or replace the process arguments the counter source uses per default.
     * This attributed will typically used to specify the user parameter on Unix commands to filter processes
     * that are not in scope.
     *
     * Example:  -u appuser
     */
    private String additionalCommandArguments;

    /**
     * Converts the given {@link CounterDescriptor} to an UnixSourceConfig instance
     * that is initialized with the relevant data of the descriptor and filled with
     * default data if necessary.
     *
     * @param counterDescriptor the CounterDescriptor that provides the config data
     * @return an UnixSourceConfig that hold all required information to get SSH executors to work.
     */
    public static UnixSourceConfig fromDescriptor(CounterDescriptor counterDescriptor) {
        UnixSourceConfig sourceConfig = new UnixSourceConfig();

        boolean sourceAvailable = StringUtils.isNoneBlank(counterDescriptor.getProcessMetricsSource());
        String source = counterDescriptor.getProcessMetricsSource();
        sourceConfig.setHost(source);

        // fallback to configured node address if no process source specified
        if (!sourceAvailable) {
            sourceConfig.setHost(counterDescriptor.getNodeAddress());
        }

        // sources that start with slash will interpreted as path, also if file:/ used
        if (sourceAvailable && source.startsWith("/")) {
            sourceConfig.setLogfilePath(source);
            sourceConfig.setHost(counterDescriptor.getNodeAddress());

        } else if (sourceAvailable && source.startsWith("file:")) {
            sourceConfig.setLogfilePath(source.substring("file:".length()));
            sourceConfig.setHost(counterDescriptor.getNodeAddress());
        }

        sourceConfig.setAdditionalCommandArguments(counterDescriptor.getAdditionalArguments());
        sourceConfig.setUsername(counterDescriptor.getUsername());
        sourceConfig.setPassword(counterDescriptor.getPassword());

        // try to parse host address to URL to resolve port
        try {
            URL url = new URL(sourceConfig.getHost());
            sourceConfig.setHost(url.getHost());
            sourceConfig.setPort(url.getPort() > 0 ? url.getPort() : DEFAULT_SSH_PORT);

        } catch (MalformedURLException e) {
            Matcher matcher = SIMPLE_HOST_ADDRESS_PATTERN.matcher(sourceConfig.getHost());
            if (matcher.matches()) {
                sourceConfig.setHost(matcher.group(1));
                sourceConfig.setPort(Integer.valueOf(matcher.group(2)));
            } else {
                sourceConfig.setPort(DEFAULT_SSH_PORT);
            }
        }

        return sourceConfig;
    }

    /**
     * @return the hostname
     */
    public String getHost() {
        return host;
    }

    /**
     * Sets the hostname.
     *
     * @param host a string that contains hostname.
     */
    public void setHost(String host) {
        this.host = host;
    }

    /**
     * Returns the SSH port
     *
     * @return the port
     */
    public Integer getPort() {
        return port;
    }

    /**
     * Sets the SSH port.
     *
     * @param port the port
     */
    public void setPort(Integer port) {
        this.port = port;
    }

    /**
     * Returns the path to the file that contains the output of the shell command.
     *
     * @return the path
     */
    public String getLogfilePath() {
        return logfilePath;
    }

    /**
     * Sets the path to the file that contains the output of the shell command. If the path is set the actual shell
     * command (i.e. top, diskfree, iostat, ...) is not executed. Instead the content of the file is read.
     *
     * @param logfilePath the path to the file
     */
    public void setLogfilePath(String logfilePath) {
        this.logfilePath = logfilePath;
    }

    /**
     * Returns additional arguments for the shell command that is executed.
     *
     * @return a String that contains the additional arguments
     */
    public String getAdditionalCommandArguments() {
        return additionalCommandArguments;
    }

    /**
     * Sets additional arguments for the shell command that is executed.
     *
     * @param additionalCommandArguments a String that contains the arguments
     */
    public void setAdditionalCommandArguments(String additionalCommandArguments) {
        this.additionalCommandArguments = additionalCommandArguments;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }
}

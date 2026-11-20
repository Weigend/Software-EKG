//______________________________________________________________________________
//
//          Project:    Software EKG
//______________________________________________________________________________
//
//         Author:      johannes.weigend
//           Date:      22.10.13 17:03
//        Copyright:    Weigend AM
//______________________________________________________________________________
//
// Notice: This piece of software was created, designed and implemented by
// experienced craftsmen and innovators in Munich, Germany.
// Changes should be done with respect to the original design.
//____________________________________________________________________________
package io.softwareecg.collector.source.log;

import io.softwareecg.collector.config.CounterDescriptor;
import io.softwareecg.collector.source.CounterSeries;
import io.softwareecg.collector.source.CounterSource;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;

import java.util.Arrays;

/**
 * This source gets a server side logfile per tail over SSH. It is mostly generic.
 *
 * @since 12.05.2015
 */
public class LogCounterSource implements CounterSource {

    /**
     * The unique id for the xml scheme.
     */
    public static final String COUNTER_SOURCE_ID = "log";

    /**
     * Our runner.
     */
    private SshTailLogger runner = null;

    @Override
    public CounterSeries queryCounter(CounterDescriptor descriptor) {

        Validate.isTrue(StringUtils.isNotEmpty(descriptor.getProcessMetricsSource()), "counter needs a source specified!");
        Validate.isTrue(StringUtils.isNotEmpty(descriptor.getNodeName()), "counter needs a host name specified!");

        String hostAddress = descriptor.getNodeAddress();
        String logfileName = descriptor.getProcessMetricsSource().replace("${HOSTNAME}", descriptor.getNodeName());
        String password = descriptor.getPassword();
        String userName = descriptor.getUsername();

        if (!HostNameUtil.isLocalhost(hostAddress)) {
            checkPrecondition(hostAddress, logfileName, userName, password);
        }

        // every source has one runner
        if (runner == null) {
            runner = new SshTailLogger(descriptor);
            runner.start();
        }
        Double[] result = new Double[descriptor.getMetricDefinitions().length];
        for (int i = 0; i < result.length; i++) {
            if (!runner.isError()) {
                result[i] = (double) runner.getLogLinesWritten();
            } else {
                result[i] = -1.0;
            }
        }
        return new CounterSeries(Arrays.asList(result));
    }

    /**
     * Helper to validate our input arguments.
     *
     * @param hostAddress a host address should not be null or empty.
     * @param logFileName a log file name should not be null or empty.
     * @param userName    a user name should not be null or empty.
     * @param password    a password should not be null or empty.
     */
    private void checkPrecondition(String hostAddress, String logFileName, String userName, String password) {
        if (userName == null || userName.isEmpty()) {
            throw new IllegalStateException("LogCounterSource needs a user in the host section for SSH access.");
        }
        if (password == null || password.isEmpty()) {
            throw new IllegalStateException("LogCounterSource needs a password in the host section for SSH access.");
        }
        if (logFileName == null || logFileName.isEmpty()) {
            throw new IllegalStateException("LogCounterSource needs logfile name as source parameter for SSH access.");
        }
        if (hostAddress == null || hostAddress.isEmpty()) {
            throw new IllegalStateException("Host address should never be null or empty.");
        }
    }

    @Override
    public String getId() {
        return COUNTER_SOURCE_ID;
    }

    /**
     * Shut down the source.
     */
    public void stop() {
        if (runner != null) {
            runner.initiateShutdown();
            runner.interrupt();
        }
    }

    /**
     * Shut down the source.
     */
    public void shutdown() {
        if (runner != null) {
            runner.shutdown();
        }
    }

}
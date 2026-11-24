package io.softwareecg.collector.control;

/**
 * A bean that defines possible states of job collector and hold
 * the a single state per instance with a couple of convenience methods
 * to set and query the states.
 *
 * @since 10.08.2018
 */
public class CollectorJobState {

    /**
     * The underlying CollectorJobStateEnum that defines the current
     * state of the job this instance belongs to.
     * Each new instance of this class will start with BOOTSTRAP state.
     */
    private volatile CollectorJobStateEnum jobState = CollectorJobStateEnum.BOOTSTRAP;

    //=========================================================================================================
    // state update interface methods
    //=========================================================================================================

    /**
     * Switches the job state to "BOOTSTRAP" that indicates the job
     * is in the startup or setup phase
     */
    public void switchToInitState() {
        jobState = CollectorJobStateEnum.BOOTSTRAP;
    }

    /**
     * Switches the job state to "ENV_ERROR" that indicates the job
     * isn't able to reach the configured endpoints in the environment.
     */
    public void switchToEnvErrorErrorState() {
        jobState = CollectorJobStateEnum.ENV_ERROR;
    }

    /**
     * Switches the job state to "WILDCARD_EXPANSION" that indicates the job
     * is currently trying to resolve all metrics names it has to capture
     */
    public void switchToWildcardExpansionState() {
        jobState = CollectorJobStateEnum.WILDCARD_EXPANSION;
    }

    /**
     * Switches the job state to "WAIT_FOR_DATA" that indicates the job
     * waits for metric data from the endpoints/sources.
     */
    public void switchToWaitForDataState() {
        jobState = CollectorJobStateEnum.WAIT_FOR_DATA;
    }

    /**
     * Switches the job state to "NO_DATA_ERROR" that indicates the job
     * can reach the metric endpoints but didn't get any data from it.
     */
    public void switchToNoDataErrorState() {
        jobState = CollectorJobStateEnum.NO_DATA_ERROR;
    }

    /**
     * Switches the job state to "RUN_OK" that indicates the job
     * runs fine with all metrics resolved and  valid metric data
     * from endpoints.
     */
    public void switchToRunOkState() {
        jobState = CollectorJobStateEnum.RUN_OK;
    }

    public void switchToStorageErrorState() {
        jobState = CollectorJobStateEnum.STORAGE_ERROR;
    }

    public void switchToFailedState() {
        jobState = CollectorJobStateEnum.FAILED;
    }

    public void switchToStoppedState() {
        jobState = CollectorJobStateEnum.STOPPED;
    }

    //=========================================================================================================
    // query accessor interface methods
    //=========================================================================================================

    /**
     * Returns a boolean flag if the job runs fine with all metrics resolved and
     * valid metric data from endpoints or not.
     *
     * @return true if the job runs fine in capture mode, otherwise false
     */
    public boolean isInRunOkState() {
        return this.jobState == CollectorJobStateEnum.RUN_OK;
    }

    /**
     * Returns a boolean flag if the job currently is unable to retrieve any
     * metric data from the endpoints/metric sources to use.
     *
     * @return true if the job tries but doesn't retrieve any metric data, otherwise false
     */
    public boolean isInNoDataErrorState() {
        return this.jobState == CollectorJobStateEnum.NO_DATA_ERROR;
    }

    /**
     * Returns a boolean flag if the job currently is currently in any
     * kind of error state or not
     *
     * @return true if the job has an error, otherwise false
     */
    public boolean isInErrorState() {
        return isInNoDataErrorState() || this.jobState == CollectorJobStateEnum.ENV_ERROR
                || this.jobState == CollectorJobStateEnum.STORAGE_ERROR
                || this.jobState == CollectorJobStateEnum.FAILED;
    }

    /**
     * Return a readable String representation of the current state
     *
     * @return a string representation of the state
     */
    /**
     * @return the current state, for callers that need more than the printable text
     */
    public CollectorJobStateEnum getState() {
        return jobState;
    }

    public String getPrintableState() {
        return jobState.getPrintableAlias();
    }

    //=========================================================================================================
    // anonymous class that defines the possible job states
    //=========================================================================================================

    /**
     * An enumeration that defines the possible job states
     * an CollectorJob can take on.
     */
    public enum CollectorJobStateEnum {

        // job is new and in the initializing phase
        BOOTSTRAP("initializing"),

        // job isn't able to reach the configured endpoints in the environment
        // to resolve the cluster nodes or the metric values
        ENV_ERROR("endpoints unreachable"),

        // job use wildcard expansion to resolve all  metrics it has to capture
        WILDCARD_EXPANSION("wildcard expansion"),

        // the endpoints and the collector waits for the first set of metric data
        WAIT_FOR_DATA("wait for data"),

        // the endpoints are reachable but the job didn't get any data from it
        NO_DATA_ERROR("no data available"),

        // the jobs run and capture the metric what are configured
        RUN_OK("ok"),

        // the source delivers, but the collected values cannot be written; the job retries
        STORAGE_ERROR("cannot write"),

        // the job ended with an unexpected error and does not collect anymore
        FAILED("failed"),

        // the job was stopped on request
        STOPPED("stopped");

        private final String printableAlias;

        /**
         * Constructs an CollectorJobStateEnum instance with
         * the specified printable alias.
         *
         * @param alias the alias to use
         */
        CollectorJobStateEnum(String alias) {
            this.printableAlias = alias;
        }

        public String getPrintableAlias() {
            return printableAlias;
        }
    }
}

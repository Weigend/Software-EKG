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
package io.softwareecg.collector.dt;

/**
 * This class contains all information about the processes
 * the EKG collector should capture metrics from.
 *
 * @since 13.03.2014
 */
public class ProcessInfo {

    /**
     * The process name / process matching expression
     */
    private String processName;

    /**
     * An optional label for the process
     */
    private String processLabel;

    /**
     * The endpoint used to fetch the process metrics
     */
    private String source;

    /**
     * Additional/optional call arguments that can/will used by the counter sources to fetch the data if specified.
     * For this the arguments will append or replace the process arguments the counter source uses per default.
     */
    private final String additionalArguments;

    /**
     * Constructs a new ProcessInfo instance that will pre-initialized
     * with the given arguments
     *
     * @param processName the name of the process that will provide the metrics
     * @param processLabel an human readable label that will use instead the process name at the AWB output
     * @param source  the endpoint used to fetch the process metrics
     * @param additionalArguments optional call args that will used to parametrize the processes
     */
    public ProcessInfo(String processName, String processLabel, String source, String additionalArguments) {
        this.processName = processName;
        this.processLabel = processLabel;
        this.source = source != null ? source.trim() : null;
        this.additionalArguments = additionalArguments;
    }

    /**
     * Getter for processName.
     * @return the processName name.
     */
    public String getProcessName() {
        return processName;
    }

    public void setProcessName(String name) {
        this.processName = name;
    }

    /**
     * Return Additional/optional call arguments that can/will used by the
     * counter sources to parametrize the processes at capturing the metric data.
     *
     * @return additional call args if available otherwise null
     */
    public String getAdditionalArguments() {
        return additionalArguments;
    }


    /**
     * Getter for the label.
     * @return the processName label.
     */
    public String getProcessLabel() {
        return processLabel;
    }

    public void setProcessLabel(String label) {
        this.processLabel = label;
    }

    /**
     * Returns the source endpoint that provided the process
     * metrics. This can be an URL or just a config token and
     * heavily depends on that the counter source type requires.
     *
     * @return the source endpoint
     */
    public String getSource() {
        return source;
    }


    /**
     * Setter for the source.
     * @param source the source.
     */
    public void setSourceEndpoint(String source) {
        this.source = source;
    }

    public ProcessInfo copy() {
        return new ProcessInfo(processName, processLabel, source, additionalArguments);
    }
}
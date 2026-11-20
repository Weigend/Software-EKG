package io.softwareecg.collector.config.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.time.Duration;

/**
 * One line of work: the metrics of a metric group, of the processes of a process group, on
 * the machines of a host group - one job for every host and process.
 *
 * @param type           how the machines are reached, see {@link Type}
 * @param metricGroup    the id of the metric group
 * @param processGroup   the id of the process group
 * @param hostGroup      for a remote collector: the id of the host group
 * @param nodeGroup      for a sidecar collector: the id of a host group, a pod group or a pod;
 *                       {@code null} for the machine the collector runs on
 * @param interval       how often the values are read
 * @param expandInterval how often wildcards are resolved again, to find metrics that are new;
 *                       {@code null} for once at the start
 * @param at             a fixed time to read the values at instead of an interval, or {@code null}
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({"type", "metricGroup", "processGroup", "hostGroup", "nodeGroup", "interval", "expandInterval", "at"})
public record CollectorDef(Type type,
                           String metricGroup,
                           String processGroup,
                           String hostGroup,
                           String nodeGroup,
                           Duration interval,
                           Duration expandInterval,
                           String at) {

    /**
     * How a collector reaches the machines it reads.
     */
    public enum Type {

        /**
         * The collector reads other machines, the hosts of a host group. The default.
         */
        @JsonProperty("remote")
        REMOTE,

        /**
         * The collector runs next to what it reads. Without a node group it reads the machine
         * it runs on.
         */
        @JsonProperty("sidecar")
        SIDECAR
    }

    public CollectorDef {
        type = type == null ? Type.REMOTE : type;
    }

    /**
     * A collector that reads the hosts of a host group.
     */
    public static CollectorDef remote(String metricGroup, String processGroup, String hostGroup,
                                      Duration interval, Duration expandInterval) {
        return new CollectorDef(Type.REMOTE, metricGroup, processGroup, hostGroup, null, interval, expandInterval, null);
    }

    /**
     * A collector that runs next to what it reads.
     *
     * @param nodeGroup a host group, a pod group or a pod; {@code null} for the own machine
     */
    public static CollectorDef sidecar(String metricGroup, String processGroup, String nodeGroup,
                                       Duration interval, Duration expandInterval) {
        return new CollectorDef(Type.SIDECAR, metricGroup, processGroup, null, nodeGroup, interval, expandInterval, null);
    }
}

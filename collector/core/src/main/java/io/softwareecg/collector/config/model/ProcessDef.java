package io.softwareecg.collector.config.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

/**
 * A process whose metrics are collected, on every host of the collector.
 *
 * @param name   the name of the process as the source knows it: {@code java}, part of a command
 *               line, an alias for a service
 * @param label  the name shown instead, if the real one is hard to read; or {@code null}
 * @param source where the metrics of the process are read: an address, a port, a file - what
 *               the source of the metric group needs. Usually refers to the host:
 *               <code>${REF:hostAddress}:22</code>. Or {@code null}.
 * @param args   further arguments for the source, or {@code null}
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({"name", "label", "source", "args"})
public record ProcessDef(String name, String label, String source, String args) {
}

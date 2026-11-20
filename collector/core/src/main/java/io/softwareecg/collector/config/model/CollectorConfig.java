package io.softwareecg.collector.config.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import io.softwareecg.collector.config.InvalidConfigurationException;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A collector configuration as it is written down: what is measured ({@link #metricGroups()}),
 * of which processes ({@link #processGroups()}), on which machines ({@link #hostGroups()},
 * {@link #platform()}), and the {@link #collectors()} that combine the three.
 * <p>
 * This is the content of a {@code config.yaml}. The groups are maps; their keys are the ids the
 * collectors refer to. A collector yields one job for every host and every process of its groups.
 *
 * @param project       the EKG project the collected series belong to, or {@code null}
 * @param metricGroups  the sets of metrics, by id
 * @param processGroups the sets of processes, by id
 * @param hostGroups    the sets of hosts, by id
 * @param platform      the pods of a cluster, or {@code null}
 * @param collectors    what is collected where
 */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
@JsonPropertyOrder({"project", "metricGroups", "processGroups", "hostGroups", "platform", "collectors"})
public record CollectorConfig(String project,
                              Map<String, MetricGroup> metricGroups,
                              Map<String, List<ProcessDef>> processGroups,
                              Map<String, HostGroup> hostGroups,
                              Platform platform,
                              List<CollectorDef> collectors) {

    public CollectorConfig {
        metricGroups = metricGroups == null ? Map.of() : new LinkedHashMap<>(metricGroups);
        processGroups = processGroups == null ? Map.of() : new LinkedHashMap<>(processGroups);
        hostGroups = hostGroups == null ? Map.of() : new LinkedHashMap<>(hostGroups);
        collectors = collectors == null ? List.of() : new ArrayList<>(collectors);
    }

    /**
     * Checks that the configuration is complete and that every collector refers to groups that
     * exist. The message of the exception names the place and what is missing there.
     *
     * @throws InvalidConfigurationException if it is not
     */
    public void validate() {
        if (metricGroups.isEmpty()) {
            throw invalid("'metricGroups' is missing or empty: there is nothing to measure.");
        }
        metricGroups.forEach((id, group) -> {
            if (group == null || isBlank(group.type())) {
                throw invalid("metricGroups." + id + ": 'type' is missing - the source of the metrics, for example unix or prometheus.");
            }
            if (group.metrics().isEmpty()) {
                throw invalid("metricGroups." + id + ": 'metrics' is missing or empty.");
            }
            for (Metric metric : group.metrics()) {
                if (metric == null || isBlank(metric.metric())) {
                    throw invalid("metricGroups." + id + ": a metric is empty.");
                }
            }
        });

        if (processGroups.isEmpty()) {
            throw invalid("'processGroups' is missing or empty.");
        }
        processGroups.forEach((id, processes) -> {
            if (processes == null || processes.isEmpty()) {
                throw invalid("processGroups." + id + ": the group has no processes.");
            }
            for (ProcessDef process : processes) {
                if (process == null || isBlank(process.name())) {
                    throw invalid("processGroups." + id + ": a process has no 'name'.");
                }
            }
        });

        hostGroups.forEach((id, group) -> {
            if (group == null || group.hosts().isEmpty()) {
                throw invalid("hostGroups." + id + ": 'hosts' is missing or empty.");
            }
            for (Host host : group.hosts()) {
                if (host == null || isBlank(host.name()) || isBlank(host.address())) {
                    throw invalid("hostGroups." + id + ": a host needs a 'name' and an 'address'.");
                }
            }
        });

        if (platform != null) {
            if (isBlank(platform.name())) {
                throw invalid("platform: 'name' is missing - the namespace or project of the cluster.");
            }
            platform.podGroups().forEach((id, group) -> {
                if (group == null) {
                    throw invalid("platform.podGroups." + id + " is empty.");
                }
            });
        }

        if (collectors.isEmpty()) {
            throw invalid("'collectors' is missing or empty: nothing would be collected.");
        }
        for (int i = 0; i < collectors.size(); i++) {
            validate("collectors[" + i + "]", collectors.get(i));
        }
    }

    private void validate(String where, CollectorDef collector) {
        if (collector == null) {
            throw invalid(where + " is empty.");
        }
        if (isBlank(collector.metricGroup())) {
            throw invalid(where + ": 'metricGroup' is missing.");
        }
        if (!metricGroups.containsKey(collector.metricGroup())) {
            throw invalid(where + ": there is no metric group '" + collector.metricGroup() + "'. Known: " + metricGroups.keySet());
        }
        if (isBlank(collector.processGroup())) {
            throw invalid(where + ": 'processGroup' is missing.");
        }
        if (!processGroups.containsKey(collector.processGroup())) {
            throw invalid(where + ": there is no process group '" + collector.processGroup() + "'. Known: " + processGroups.keySet());
        }
        if (collector.interval() == null && isBlank(collector.at())) {
            throw invalid(where + ": 'interval' is missing - how often is collected, for example 15s.");
        }
        if (collector.interval() != null && collector.interval().isNegative()) {
            throw invalid(where + ": 'interval' is negative.");
        }

        if (collector.type() == CollectorDef.Type.REMOTE) {
            if (!isBlank(collector.nodeGroup())) {
                throw invalid(where + ": 'nodeGroup' belongs to a collector of 'type: sidecar'. A remote collector names its 'hostGroup'.");
            }
            if (isBlank(collector.hostGroup())) {
                throw invalid(where + ": 'hostGroup' is missing. (A collector that reads its own machine is of 'type: sidecar'.)");
            }
            if (!hostGroups.containsKey(collector.hostGroup())) {
                throw invalid(where + ": there is no host group '" + collector.hostGroup() + "'. Known: " + hostGroups.keySet());
            }
        } else {
            if (!isBlank(collector.hostGroup())) {
                throw invalid(where + ": a collector of 'type: sidecar' names its machines as 'nodeGroup', not 'hostGroup'.");
            }
            if (!isBlank(collector.nodeGroup()) && !hostGroups.containsKey(collector.nodeGroup())
                    && (platform == null || !platform.knows(collector.nodeGroup()))) {
                throw invalid(where + ": there is no host group, pod group or pod '" + collector.nodeGroup() + "'.");
            }
        }
    }

    private static InvalidConfigurationException invalid(String message) {
        return new InvalidConfigurationException(message);
    }

    static boolean isBlank(String text) {
        return text == null || text.isBlank();
    }
}

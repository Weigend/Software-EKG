package io.softwareecg.collector.config.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The part of a cluster (Kubernetes, OpenShift) that is collected: a namespace and its pods.
 * A collector of type sidecar refers to a pod group or to a single pod as its node group.
 *
 * @param name            the namespace (Kubernetes) or project (OpenShift)
 * @param serviceAddress  the address of the service of the platform that lists the pods, for
 *                        example {@code https://host:port/api/v1/pods}; or {@code null}
 * @param serviceUser     the login for that service, or {@code null}
 * @param servicePassword its password, or {@code null}
 * @param refreshInterval how often the state of the pods is read again, or {@code null}
 * @param podGroups       the pods, grouped, by id of the group
 */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
@JsonPropertyOrder({"name", "serviceAddress", "serviceUser", "servicePassword", "refreshInterval", "podGroups"})
public record Platform(String name,
                       String serviceAddress,
                       String serviceUser,
                       String servicePassword,
                       Duration refreshInterval,
                       Map<String, PodGroup> podGroups) {

    public Platform {
        podGroups = podGroups == null ? Map.of() : new LinkedHashMap<>(podGroups);
    }

    /**
     * @return {@code true} if the id is the one of a pod group or of a pod
     */
    boolean knows(String id) {
        return podGroups.containsKey(id)
                || podGroups.values().stream().anyMatch(group -> group != null
                && group.pods().stream().anyMatch(pod -> pod != null && id.equals(pod.id())));
    }

    /**
     * Pods that are collected alike, typically the pods of one service.
     *
     * @param service   the name of the service, or {@code null}
     * @param namespace the namespace of the pods if it is not the one of the platform, or {@code null}
     * @param username  the login for all pods that name none of their own, or {@code null}
     * @param password  its password, or {@code null}
     * @param pods      the pods
     */
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    @JsonPropertyOrder({"service", "namespace", "username", "password", "pods"})
    public record PodGroup(String service, String namespace, String username, String password, List<Pod> pods) {

        public PodGroup {
            pods = pods == null ? List.of() : new ArrayList<>(pods);
        }
    }

    /**
     * A pod: one unit of deployment, with the containers that run in it.
     *
     * @param id         the id a collector can refer to this pod alone with, or {@code null}
     * @param name       the name the pod is shown with
     * @param address    its address
     * @param username   the login, or {@code null} for the one of the group
     * @param password   its password, or {@code null}
     * @param containers the containers of the pod
     */
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    @JsonPropertyOrder({"id", "name", "address", "username", "password", "containers"})
    public record Pod(String id, String name, String address, String username, String password,
                      List<Container> containers) {

        public Pod {
            containers = containers == null ? List.of() : new ArrayList<>(containers);
        }
    }

    /**
     * A container of a pod. Containers of one pod differ in their port only.
     *
     * @param id       the id of the container
     * @param name     the name it is shown with, or {@code null} for its id
     * @param address  its address, or {@code null} for the one of the pod
     * @param username the login, or {@code null} for the one of the pod
     * @param password its password, or {@code null}
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonPropertyOrder({"id", "name", "address", "username", "password"})
    public record Container(String id, String name, String address, String username, String password) {
    }
}

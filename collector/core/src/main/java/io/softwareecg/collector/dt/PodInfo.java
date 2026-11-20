package io.softwareecg.collector.dt;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.StringJoiner;

/**
 * todo insert comment here
 *
 * @since 09.08.2018
 */
public class PodInfo extends NodeInfo {

    private String namespace;

    private String serviceName;

    private final List<ContainerInfo> containers = new ArrayList<>();

    /**
     *
     * @param podName the name/id of the Pod
     * @param podAddress the address of the pod inside the cloud platform
     * @param podGroupName the name of the pod group this pod belongs to (this is may the service name)
     * @param namespace
     * @param serviceName
     * @param username the username required to connect to the pod or it's services
     * @param password the password required to connect to the pod or it's services
     */
    public PodInfo(String podName, String podAddress, String podGroupName, String namespace, String serviceName,
                   String username, String password) {

        super(podName, podAddress, podGroupName, username, password);

        this.namespace = namespace;
        this.serviceName = serviceName;
    }

    public String getNamespace() {
        return namespace == null ? "*" : namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }


    public String getServiceName() {
        return serviceName == null ? "*" : serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    @Override
    public PodInfo copy() {
        return new PodInfo(getNodeName(), getNodeAddress(), getNodeGroupId(), namespace,
                serviceName, getUsername(), getPassword());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PodInfo podInfo = (PodInfo) o;
        return Objects.equals(namespace, podInfo.namespace) &&
                Objects.equals(serviceName, podInfo.serviceName) &&
                Objects.equals(containers, podInfo.containers);
    }

    @Override
    public int hashCode() {
        return Objects.hash(namespace, serviceName, containers);
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", PodInfo.class.getSimpleName() + "[", "]")
                .add("podName='" + getNodeName() + "'")
                .add("podAddress='" + getNodeAddress() + "'")
                .add("namespace='" + namespace + "'")
                .add("serviceName='" + serviceName + "'")
                .add("containers=" + containers)
                .toString();
    }


}
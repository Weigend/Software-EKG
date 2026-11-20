package io.softwareecg.collector.dt;

import java.util.Objects;

/**
 * todo insert comment here
 *
 * @since 09.08.2018
 */
public class PodGroupInfo {

    private String namespace;

    private String serviceName;

    private String podGroupId;

    public PodGroupInfo(String namespace, String serviceName, String podGroupId) {
        this.namespace = namespace;
        this.serviceName = serviceName;
        this.podGroupId = podGroupId;
    }

    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public String getPodGroupId() {
        return podGroupId;
    }

    public void setPodGroupId(String podGroupId) {
        this.podGroupId = podGroupId;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PodGroupInfo that = (PodGroupInfo) o;
        return Objects.equals(namespace, that.namespace) &&
                Objects.equals(serviceName, that.serviceName) &&
                Objects.equals(podGroupId, that.podGroupId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(namespace, serviceName, podGroupId);
    }
}

package io.softwareecg.collector.dt;

/**
 * todo insert comment here
 *
 * @since 11.08.2018
 */
public class ContainerInfo extends NodeInfo {


    public ContainerInfo(String containerId, String containerAddress,
                         String podName, String username, String password) {
        super(containerId, containerAddress, podName, username, password);
    }

    public ContainerInfo copy() {
        return (ContainerInfo) super.copy();
    }
}

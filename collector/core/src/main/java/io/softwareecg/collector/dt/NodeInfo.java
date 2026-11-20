package io.softwareecg.collector.dt;

/**
 * todo insert comment here
 *
 * @since 11.08.2018
 */
public class NodeInfo {

    /**
     * The alias name of the host
     */
    private String nodeName;

    /**
     * The IP address or absolute URL both with
     * or without port number
     */
    private String nodeAddress;

    /**
     * The unique id of the host group that will used to identify the specific host group
     * than referencing it in the Collector config section
     */
    private String nodeGroupId;

    /**
     * The username required to connect to the host
     */
    private String username;

    /**
     * The password required to connect to the host
     */
    private String password;


    public NodeInfo(String nodeName, String nodeAddress, String nodeGroupId, String username, String password) {
        this.nodeName = nodeName;
        this.nodeAddress = nodeAddress;
        this.nodeGroupId = nodeGroupId;
        this.username = username;
        this.password = password;
    }

    public String getNodeName() {
        return nodeName;
    }

    public String getNodeAddress() {
        return nodeAddress;
    }

    public String getNodeGroupId() {
        return nodeGroupId;
    }

    public void setNodeName(String nodeName) {
        this.nodeName = nodeName;
    }

    public void setNodeAddress(String nodeAddress) {
        this.nodeAddress = nodeAddress;
    }

    public void setNodeGroupId(String nodeGroupId) {
        this.nodeGroupId = nodeGroupId;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public NodeInfo copy() {
        return new NodeInfo(nodeName, nodeAddress, nodeGroupId, username, password);
    }
}

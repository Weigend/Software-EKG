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
 * This class contains all information for a host.
 */
public class HostInfo extends NodeInfo {


    /**
     * The Information to access a host.
     *
     * @param hostName  the name of the host.
     * @param hostAddress the ip of the host.
     * @param hostGroupName the hostgroup.
     * @param password  the password (optional)
     * @param username  the userName (optional)
     */
    public HostInfo(String hostName, String hostAddress, String hostGroupName, String username, String password) {
        super(hostName, hostAddress, hostGroupName, username, password);
    }

    @Override
    public HostInfo copy() {
        return new HostInfo(getNodeName(), getNodeAddress(), getNodeGroupId(), getUsername(), getPassword());
    }
}
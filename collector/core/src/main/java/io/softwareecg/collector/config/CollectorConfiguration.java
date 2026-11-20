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
package io.softwareecg.collector.config;

import io.softwareecg.collector.dt.ClassicCollectorJobConfiguration;
import io.softwareecg.collector.dt.CloudCollectorJobConfiguration;
import io.softwareecg.collector.dt.SidecarCollectorJobConfiguration;

import java.util.List;


/**
 * Provides access to collector configuration.
 */
public interface CollectorConfiguration {

    List<ClassicCollectorJobConfiguration> getClassicCollectorJobConfiguration();

    List<CloudCollectorJobConfiguration> getCloudCollectorJobConfiguration();

    List<SidecarCollectorJobConfiguration> getSidecarCollectorConfiguration();

}

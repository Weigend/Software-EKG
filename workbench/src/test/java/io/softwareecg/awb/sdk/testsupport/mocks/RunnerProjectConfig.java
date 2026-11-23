package io.softwareecg.awb.sdk.testsupport.mocks;

import io.softwareecg.awb.projectcontext.model.TargetProjectConfig;

/**
 * A extended TargetProjectConfig that just provides an
 * parameterized constructor
 *
 * @since 24.03.2020
 */
public class RunnerProjectConfig extends TargetProjectConfig {

    /**
     * Constructs a new instance of RunnerProjectConfig
     * pre-configured with the specified project name.
     *
     * @param projectName the project name
     */
    public RunnerProjectConfig(String projectName) {
        this.setProjectName(projectName);
        this.setOverwriteExistingData(false);
    }
}

package io.softwareecg.collector.dt;

/**
 * todo insert comment here
 *
 * @since 09.08.2018
 */
public class PlatformProjectInfo {

    private final String projectName;

    private final String platformServiceAddress;

    private final String platformServiceUser;

    private final String platformServicePassword;

    private final long platformRefreshStateInterval;

    public PlatformProjectInfo(String projectName, String platformServiceAddress,
                              String platformServiceUser, String platformServicePassword, long platformRefreshState) {

        this.projectName = projectName;
        this.platformServiceAddress = platformServiceAddress;
        this.platformServiceUser = platformServiceUser;
        this.platformServicePassword = platformServicePassword;
        this.platformRefreshStateInterval = platformRefreshState;
    }

    public String getProjectName() {
        return projectName;
    }

    public String getPlatformServiceAddress() {
        return platformServiceAddress;
    }

    public String getPlatformServiceUser() {
        return platformServiceUser;
    }

    public String getPlatformServicePassword() {
        return platformServicePassword;
    }

    public long getPlatformRefreshStateInterval() {
        return platformRefreshStateInterval;
    }

    public PlatformProjectInfo copy() {

        return new PlatformProjectInfo(
                projectName,
                platformServiceAddress,
                platformServiceUser,
                platformServicePassword,
                platformRefreshStateInterval
        );
    }
}

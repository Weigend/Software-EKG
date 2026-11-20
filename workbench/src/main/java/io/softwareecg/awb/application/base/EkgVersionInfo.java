//______________________________________________________________________________
//
//                  ProjectConfiguration:    Software EKG
//______________________________________________________________________________
//
//                   Author:    Weigend AM 2009 - 2017
//______________________________________________________________________________
//
// Notice: This piece of software was created, designed and implemented by
// experienced craftsmen and innovators in Munich, Germany.
// Changes should be done with respect to the original design.
//______________________________________________________________________________
package io.softwareecg.awb.application.base;

import io.softwareecg.awb.commons.about.VersionInfo;
import io.softwareecg.awb.sdk.core.log.EkgLogger;
import jakarta.inject.Singleton;
import org.slf4j.Logger;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.time.Instant;
import java.util.Date;
import java.util.Properties;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;

/**
 * Get the Version Info for the Software-EKG
 *
 */
@Singleton
public class EkgVersionInfo implements VersionInfo {
    private static final Logger LOGGER = EkgLogger.get();

    private String buildRevision;
    private final Instant buildTime;
    private String versionString;

    /**
     * Read and cache the version properties.
     */
    public EkgVersionInfo() {
        Properties buildNumber = getBuildNumberProperties();
        // the version is written by the build and nowhere else, see build.properties
        versionString = written(buildNumber, "version");
        buildRevision = written(buildNumber, "revision");

        String timestamp = buildNumber.getProperty("timestamp");
        if (timestamp == null) {

            Date date = getClassBuildTime();

            if (date != null) {
                buildTime = Instant.ofEpochMilli(date.getTime());
            } else {
                buildTime = Instant.ofEpochMilli(new Date().getTime());
            }
        } else {
            buildTime = Instant.ofEpochMilli(Long.parseLong(buildNumber.getProperty("timestamp")));
        }
    }

    /**
     * @return what the build wrote for the key, or "unknown" if it wrote nothing - as in a
     *         start from classes that Maven did not process
     */
    private static String written(Properties properties, String key) {
        String value = properties.getProperty(key);
        return value == null || value.isBlank() || value.startsWith("${") ? "unknown" : value.strip();
    }

    @Override
    public String getBuildRevision() {
        return buildRevision;
    }

    @Override
    public Instant getBuildTime() {
        return buildTime;
    }

    @Override
    public String getVersionString() {
        return versionString;
    }

    /**
     * Get the build number properties.
     *
     * @return the build number properties.
     */
    private static Properties getBuildNumberProperties() {
        Properties buildNumber = new Properties();
        try {
            try (InputStream propertiesFile = EkgVersionInfo.class.getResourceAsStream("/build.properties")) {
                if (propertiesFile != null) {
                    buildNumber.load(propertiesFile);
                } else {
                    LOGGER.info("No Build Number found");
                }
            }
        } catch (IOException e) {
            LOGGER.info("No Build Number found");
        }
        return buildNumber;
    }


    private static Date getClassBuildTime() {
        Date d = null;
        Class<?> currentClass = new Object() {}.getClass().getEnclosingClass();
        URL resource = currentClass.getResource(currentClass.getSimpleName() + ".class");
        if (resource != null) {
            if (resource.getProtocol().equals("file")) {
                try {
                    d = new Date(new File(resource.toURI()).lastModified());
                } catch (URISyntaxException ignored) { }
            } else if (resource.getProtocol().equals("jar")) {
                String path = resource.getPath();
                d = new Date( new File(path.substring(5, path.indexOf("!"))).lastModified() );
            } else if (resource.getProtocol().equals("zip")) {
                String path = resource.getPath();
                File jarFileOnDisk = new File(path.substring(0, path.indexOf("!")));
                //long jfodLastModifiedLong = jarFileOnDisk.lastModified ();
                //Date jfodLasModifiedDate = new Date(jfodLastModifiedLong);
                try(JarFile jf = new JarFile(jarFileOnDisk)) {
                    ZipEntry ze = jf.getEntry (path.substring(path.indexOf("!") + 2));//Skip the ! and the /
                    long zeTimeLong = ze.getTime ();
                    Date zeTimeDate = new Date(zeTimeLong);
                    d = zeTimeDate;
                } catch (IOException|RuntimeException ignored) { }
            }
        }
        return d;
    }
}

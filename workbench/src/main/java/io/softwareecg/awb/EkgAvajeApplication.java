//______________________________________________________________________________
//
//          ProjectConfiguration:    Software EKG
//______________________________________________________________________________
package io.softwareecg.awb;

import io.softwareecg.awb.uitoolkit.events.FinishEvent;
import io.softwareecg.awb.application.about.VersionInfo;
import io.softwareecg.wfx.platform.api.EventBus;
import io.softwareecg.wfx.lookup.api.Lookup;
import io.softwareecg.wfx.main.Main;
import io.softwareecg.wfx.platform.api.exceptions.PlatformException;
import io.softwareecg.wfx.windowmanager.api.ApplicationWindow;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.stage.Stage;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.PrintStream;

/**
 * Avaje-Inject-backed startup class for Software-EKG.
 */
public class EkgAvajeApplication extends Main {

    private static final Logger LOGGER = LoggerFactory.getLogger(EkgAvajeApplication.class);

    @Override
    public void init() {
        super.init();
        System.setErr(new PrintStream(System.err) {
            public void print(String s) {
                if (StringUtils.isNotBlank(s)) {
                    // JVM "WARNING:" lines and Lucene/Jersey JUL output are not errors —
                    // log them at WARN so they stop polluting the error log.
                    LOGGER.warn("Captured from stderr: {}", s);
                }
            }
        });
    }

    @Override
    public void start(Stage stage) throws PlatformException {
        try {
            Lookup.lookup(EventBus.class).subscribe(FinishEvent.class, event -> {
                try {
                    LOGGER.info("FinishEvent received, initiating application shutdown...");
                    stop();
                    LOGGER.info("Application stop() completed, calling Platform.exit()");
                    Platform.exit();
                    System.exit(0);
                    return true;
                } catch (Exception e) {
                    LOGGER.error("Error occurred during application shutdown", e);
                }
                return false;
            });

            super.start(stage);

            VersionInfo versionInfo = Lookup.lookup(VersionInfo.class);
            LOGGER.info("Started Software-EKG {} Revision {} (Avaje runtime)",
                    versionInfo.getVersionString(), versionInfo.getBuildRevision());
            ApplicationWindow appWindow = Lookup.lookup(ApplicationWindow.class);
            // a build of a developer has no revision worth showing
            boolean released = !"local".equals(versionInfo.getBuildRevision()) && !"unknown".equals(versionInfo.getBuildRevision());
            appWindow.setTitle("Software EKG " + versionInfo.getVersionString()
                    + (released ? " rev:" + versionInfo.getBuildRevision() : ""));
        } catch (Exception e) {
            LOGGER.warn("Can not startup EGK-Application", e);
            throw new PlatformException("caught exception during initialization of Main", e);
        }
    }

    public static void main(String[] args) {
        Application.launch(args);
    }
}

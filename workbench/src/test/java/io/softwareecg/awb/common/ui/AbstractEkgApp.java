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
package io.softwareecg.awb.common.ui;

import io.softwareecg.awb.common.ui.events.FinishEvent;
import io.softwareecg.wfx.platform.api.EventBus;
import io.softwareecg.wfx.platform.core.eventbus.AnnotationProcessor;
import io.softwareecg.wfx.platform.core.eventbus.SimpleEventBus;
import io.softwareecg.wfx.lookup.api.Lookup;
import io.softwareecg.wfx.lookup.serviceloader.ServiceLoaderLookupStrategy;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URL;

/**
 * Abstract Class to encapsulate all FX-Application test usage.
 *
 */
public abstract class AbstractEkgApp extends Application {
    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractEkgApp.class);
    /* The URL to the fxml which will be shown */
    private URL fxmlResource;

    /* The title which will be used */
    private String title;

    protected void setTitle(String title) {
        this.title = title;
    }

    protected void setFxmlResource(URL fxmlResource) {
        this.fxmlResource = fxmlResource;
    }

    @Override
    public void init() {
        ServiceLoaderLookupStrategy lookupStrategy = new ServiceLoaderLookupStrategy();
        lookupStrategy.init(EventBus.class, new SimpleEventBus());
        lookupStrategy.init(FXMLLoader.class, (ServiceLoaderLookupStrategy.Producer<FXMLLoader>) FXMLLoader::new);
        Lookup.init(lookupStrategy);

        // Registers the ApplicationFinisher on the bus
        AnnotationProcessor.process(new ApplicationFinisher());
    }

    @Override
    public void start(Stage stage) {
        try {
            Parent root = FXMLLoader.load(fxmlResource);
            Scene scene = new Scene(root);
            stage.setScene(scene);
            stage.setTitle(title);
            stage.show();
        } catch (IOException e) {
            LOGGER.error("Can not start application", e);
        }
    }

    @Override
    public void stop() {
        // Tells the ApplicationFinisher to Exit
        Lookup.lookup(EventBus.class).publish(new FinishEvent(0, this));
    }
}

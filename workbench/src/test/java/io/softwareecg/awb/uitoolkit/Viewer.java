//______________________________________________________________________________
//
//                  Project:    Software EKG
//______________________________________________________________________________
//
//                   Author:    Weigend AM 2009 - 2017
//______________________________________________________________________________
//
// Notice: This piece of software was created, designed and implemented by
// experienced craftsmen and innovators in Munich, Germany.
// Changes should be done with respect to the original design.
//______________________________________________________________________________
package io.softwareecg.awb.uitoolkit;

import io.softwareecg.wfx.lookup.api.Lookup;
import io.softwareecg.wfx.lookup.serviceloader.ServiceLoaderLookupStrategy;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

/**
 * This is the viewer fxml test application.
 * <p>
 * It allows you to start a single fxml file inclusive the controller.
 * You can start every fxml file from classpath. The first command line
 * argument will be interpreted as the fxml file to start.
 * <p>
 * The started application has the same classpath elements as the full
 * application start and also uses the WFX lookup mechanism.
 *
 */
public class Viewer extends Application {

    /**
     * Main entry point for starting the viewer fxml application.
     * <p>
     * This application require exact one argument. This argument must be the
     * path to a fxml file that should be initially loaded and set as scene root.
     *
     * @param args The first is required and used as the fxml to start.
     */
    public static void main(String[] args) {
        ServiceLoaderLookupStrategy lookupStrategy = new ServiceLoaderLookupStrategy();
        lookupStrategy.init(FXMLLoader.class, (ServiceLoaderLookupStrategy.Producer<FXMLLoader>) FXMLLoader::new);
        Lookup.init(lookupStrategy);
        launch(Viewer.class, args);
    }

    @Override
    public void start(Stage primaryStage) throws IOException {
        String path = getParameters().getRaw().get(0);
        FXMLLoader loader = Lookup.lookup(FXMLLoader.class);
        loader.setLocation(getClass().getResource(path));
        Parent node = loader.load();
        primaryStage.setScene(new Scene(node));
        primaryStage.show();
        // primaryStage.setOnHidden(e -> System.exit(0));
    }
}

package io.softwareecg.awb.sdk.core.resourceloader;

import io.softwareecg.awb.sdk.core.log.EkgLogger;
import io.softwareecg.wfx.lookup.api.Lookup;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import org.slf4j.Logger;

import java.net.URL;
import io.softwareecg.awb.sdk.core.di.DiInjection;

/**
 * Helper class that prevent unnecessary glue code by providing functionality
 * to load FXML view with CDI support and exception handling.
 *
 * @since 27.07.2018
 */
@SuppressWarnings("unused") // methods are parts of the SDK and called from other projects.
public class CdiFxmlLoader {

    private static final Logger LOGGER = EkgLogger.get();

    /**
     * Instantiate a new UI component as object hierarchy from a FXML document.
     * The location from which the document will be loaded must have been reachable
     * by the specified root class.
     *
     * @param fxmlView the location of the FXML view as relative or absolute resource path
     * @param root the root the root of the object hierarchy.
     *             The value passed to this method is used as the value of the {@code <fx:root>} tag.
     * @param controller the controller that should use by the view component
     * @return a {@link ViewLoadResult} instance that includes the JavaFX component and according controller
     */
    public static ViewLoadResult loadView(String fxmlView, Parent root, Object controller) {
        Class<?> callerClass = StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE).getCallerClass();
        return loadView(fxmlView, root, controller, callerClass);
    }

    /**
     * Instantiate a new UI component as object hierarchy from a FXML document.
     * The location from which the document will be loaded must have been reachable
     * by the specified root class.
     *
     * @param fxmlView the location of the FXML view as relative or absolute resource path
     * @param root the root the root of the object hierarchy.
     *             The value passed to this method is used as the value of the {@code <fx:root>} tag.
     * @return a {@link ViewLoadResult} instance that includes the JavaFX component and according controller
     */
    public static ViewLoadResult loadView(String fxmlView, Parent root) {
        Class<?> callerClass = StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE).getCallerClass();
        return loadView(fxmlView, root, null, callerClass);
    }

    /**
     * Instantiate a new UI component as object hierarchy from a FXML document.
     * The location from which the document will be loaded must have been reachable (in classpath)
     * of the caller class.
     *
     * @param fxmlView the class path location of the FXML document that defines the object hierarchy of the component
     * @return a ViewLoadResult instance that includes the JFX component and the controller (if defined in the FXML)
     */
    public static ViewLoadResult loadView(String fxmlView) {
        Class<?> callerClass = StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE).getCallerClass();
        return loadView(fxmlView, null, null, callerClass);
    }


    private static ViewLoadResult loadView(String fxmlView, Parent root, Object controller, Class<?> callerClass) {
        FXMLLoader fxmlLoader = Lookup.lookup(FXMLLoader.class);

        URL fxmlResourceURL = callerClass.getResource(fxmlView);
        if (fxmlResourceURL == null && root != null) {
            fxmlResourceURL = root.getClass().getResource(fxmlView);
        }

        fxmlLoader.setLocation(fxmlResourceURL);

        if (controller != null) {
            fxmlLoader.setController(controller);
        }

        if (root != null) {
            fxmlLoader.setRoot(root);
        }

        ViewLoadResult result = new ViewLoadResult();

        DiInjection.injectFields(root);

        // attach FXML to this control instance
        try {
            result.setComponent(fxmlLoader.load());
            result.setController(fxmlLoader.getController());
        } catch (Exception exception) {
            LOGGER.error("Unable to load the underlying .fxml file.", exception);
            LOGGER.error("Root Cause: ", exception.getCause());
            throw new RuntimeException(exception);
        }

        return result;
    }
}

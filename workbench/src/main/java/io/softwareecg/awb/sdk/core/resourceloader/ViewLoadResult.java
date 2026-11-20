package io.softwareecg.awb.sdk.core.resourceloader;

/**
 * A simple POJO container that stores a tuple of
 * a JavaFX view component and the according controller.
 *
 * It will used as result object for view resolvers like
 * the CdiFxmlLoader
 *
 * @since 03.08.2018
 */
@SuppressWarnings("unchecked")
public class ViewLoadResult {

    /**
     * an EKG view component like a panel or
     * other type of dialog.
     */
    private Object component;

    /**
     * the controller that manages the behaviour
     * of the view component above.
     */
    private Object controller;

    /**
     * Returns the controller that manages the
     * behaviour of the view component
     *
     * @param <T> the class type of the controller
     * @return the controller instance
     */
    public <T> T getController() {
        return (T) controller;
    }

    /**
     * Returns the view component that transported
     * by this result container.
     *
     * @param <T> the class type of the component
     * @return the wrapped component
     */
    public <T> T getComponent() {
        return (T) component;
    }

    /**
     * Sets the view component that will transported
     * by this result container.
     *
     * @param component the ui component
     */
    public void setComponent(Object component) {
        this.component = component;
    }

    /**
     * Sets the view controller that belongs to the
     * stored view component.
     *
     * @param controller the view controller that belongs to the view component
     */
    public void setController(Object controller) {
        this.controller = controller;
    }
}

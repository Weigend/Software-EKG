package io.softwareecg.awb.application;

import io.avaje.inject.Bean;
import io.avaje.inject.Factory;
import io.avaje.inject.Lazy;
import io.softwareecg.awb.uitoolkit.view.AppWindowProvider;
import jakarta.inject.Inject;

/**
 * Avaje-only producer for {@link AppWindowProvider}. Lives in its own
 * class because the provider depends on runtime main-window state.
 *
 * <p><b>Lazy</b>: AppWindowProvider needs the FXML scene which is not bound
 * yet during BeanScope.build(). Match that with Avaje's {@link Lazy}.
 */
@Factory
public class AppWindowProviderFactory {

    private final EkgApplicationController appController;

    @Inject
    public AppWindowProviderFactory(EkgApplicationController appController) {
        this.appController = appController;
    }

    @Bean
    @Lazy
    public AppWindowProvider produceAppWindow() {
        return appController.getDefaultWindow();
    }
}

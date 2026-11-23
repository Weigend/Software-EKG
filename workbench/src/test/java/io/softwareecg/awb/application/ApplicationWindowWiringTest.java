package io.softwareecg.awb.application;

import io.avaje.inject.spi.AvajeModule;
import io.avaje.inject.spi.InjectExtension;
import io.softwareecg.wfx.lookup.api.Priorities;
import io.softwareecg.wfx.windowmanager.api.ApplicationWindow;
import io.softwareecg.wfx.windowmanager.windows.DefaultApplicationWindow;
import org.junit.jupiter.api.Test;

import java.util.ServiceLoader;

import static org.assertj.core.api.Assertions.assertThat;

class ApplicationWindowWiringTest {

    @Test
    void awbWindowIsProvidedByAnEarlierRuntimeModule() {
        AvajeModule awbModule = loadModule("io.softwareecg.awb.AwbModule");
        AvajeModule windowModule = loadModule(
                "io.softwareecg.awb.application.ApplicationBootstrapModule");

        assertThat(windowModule).isInstanceOf(AvajeModule.Custom.class);
        assertThat(windowModule.autoProvides())
                .contains(ApplicationWindow.class, EkgApplicationController.class);
        assertThat(awbModule.autoProvides()).doesNotContain(EkgApplicationController.class);
        assertThat(awbModule.requires()).contains(EkgApplicationController.class);
    }

    /**
     * WFX shows the first {@link ApplicationWindow} in the order of {@link Priorities}. With
     * equal priorities that order is the order of the jars on the classpath, and on Windows
     * the plain WFX window came first.
     */
    @Test
    void awbWindowTakesPrecedenceOverThePlainWfxWindow() {
        assertThat(Priorities.of(EkgApplicationController.class))
                .isLessThan(Priorities.of(DefaultApplicationWindow.class));
    }

    private static AvajeModule loadModule(String className) {
        return ServiceLoader.load(InjectExtension.class).stream()
                .filter(provider -> provider.type().getName().equals(className))
                .map(ServiceLoader.Provider::get)
                .map(AvajeModule.class::cast)
                .findFirst()
                .orElseThrow(() -> new AssertionError("Missing Avaje module: " + className));
    }
}

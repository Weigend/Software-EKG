package io.softwareecg.awb.application.base;

import io.avaje.inject.spi.AvajeModule;
import io.avaje.inject.spi.InjectExtension;
import io.softwareecg.awb.application.base.impl.EkgApplicationController;
import io.softwareecg.wfx.windowmanager.api.ApplicationWindow;
import org.junit.jupiter.api.Test;

import java.util.ServiceLoader;

import static org.assertj.core.api.Assertions.assertThat;

class ApplicationWindowWiringTest {

    @Test
    void awbWindowIsProvidedByAnEarlierRuntimeModule() {
        AvajeModule awbModule = loadModule("io.softwareecg.awb.AwbModule");
        AvajeModule windowModule = loadModule(
                "io.softwareecg.awb.application.base.impl.ApplicationBootstrapModule");

        assertThat(windowModule).isInstanceOf(AvajeModule.Custom.class);
        assertThat(windowModule.autoProvides())
                .contains(ApplicationWindow.class, EkgApplicationController.class);
        assertThat(awbModule.autoProvides()).doesNotContain(EkgApplicationController.class);
        assertThat(awbModule.requires()).contains(EkgApplicationController.class);
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

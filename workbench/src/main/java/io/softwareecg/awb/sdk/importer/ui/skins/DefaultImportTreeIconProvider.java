package io.softwareecg.awb.sdk.importer.ui.skins;

import io.softwareecg.awb.sdk.awbapi.explorer.ImporterTreeIconProvider;
import io.softwareecg.awb.sdk.core.resourceloader.CachingSvgLoader;
import javafx.scene.Node;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

/**
 * todo insert comment here
 *
 * @since 04.02.2020
 */
public class DefaultImportTreeIconProvider implements ImporterTreeIconProvider {

    protected CachingSvgLoader cachingSvgLoader = new CachingSvgLoader().setDefaultScale(0.05);

    protected Node createPngIcon(String path) {
        ImageView icon = new ImageView(new Image(getClass().getResourceAsStream(path)));
        icon.setPreserveRatio(true);
        icon.setFitWidth(18);

        return icon;
    }

    protected Node createSvgIcon(String iconResourcePath) {
        return cachingSvgLoader.getSvgImage(iconResourcePath);
    }
}

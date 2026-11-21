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
package io.softwareecg.awb.explorer.ui.items;

import afester.javafx.svg.SvgLoader;
import io.softwareecg.awb.common.ui.explorer.api.AbstractItem;
import io.softwareecg.awb.repository.api.EkgRepository;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import org.apache.commons.lang3.StringUtils;

/**
 * Abstract base for explorerView items that required to contains a {@link EkgRepository}.
 *
 */
public abstract class RepositoryBaseItem extends AbstractItem<String> {

    private final EkgRepository repository;

    /**
     * Initialize the item with a label, a context and a types.
     *
     * @param value      The visible label.
     * @param repository The types.
     */
    public RepositoryBaseItem(String value, EkgRepository repository) {
        super(value);
        this.repository = repository;
    }

    public void setItemIcon(Image icon) {
        ImageView imageView = new ImageView(icon);
        imageView.setFitHeight(22);
        imageView.setFitWidth(22);
        imageView.setSmooth(true);
        imageView.setPreserveRatio(true);

        super.setGraphic(imageView);
    }

    public String getRepositoryId() {
        if (StringUtils.isBlank(getId())) {
            return null;
        }

        return StringUtils.split(getId(), '_')[0];
    }

    public String getProjectId() {
        if (StringUtils.isBlank(getId())) {
            return null;
        }

        String[] idTokens = StringUtils.split(getId(), '_');

        if (idTokens.length > 1) {
            return StringUtils.split(getId(), '_')[1];
        }

        return null;
    }

    public EkgRepository getRepository() {
        return repository;
    }


    public Node createIcon(String iconResourcePath) {

        Group iconImage = new SvgLoader().loadSvg(getClass().getResourceAsStream(iconResourcePath));
        iconImage.setScaleX(0.05);
        iconImage.setScaleY(0.05);

        return new Group(iconImage);
    }
}

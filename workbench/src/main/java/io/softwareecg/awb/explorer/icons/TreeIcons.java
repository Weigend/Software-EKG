package io.softwareecg.awb.explorer.icons;

import io.softwareecg.awb.projectcontext.model.CloudPlatformType;
import io.softwareecg.awb.runtime.resources.CachingSvgLoader;
import javafx.scene.Node;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

/**
 * The icons of the explorer's tree.
 *
 * @since 20.09.2018
 */
public final class TreeIcons {

    public static final TreeIcons DEFAULT = new TreeIcons();

    private final CachingSvgLoader cachingSvgLoader = new CachingSvgLoader().setDefaultScale(0.05);

    private TreeIcons() {
    }

    public Node getProjectItemIcon() {
        return createSvgIcon("/io/softwareecg/awb/explorer/metric/icons/project-icon3.svg");
    }

    public Node getReproBookmarksItemIcon() {
        return createSvgIcon("/io/softwareecg/awb/explorer/metric/icons/bookmark/bookmark-group-icon.svg");
    }

    public Node getBookmarkItemIcon() {
        return createSvgIcon("/io/softwareecg/awb/explorer/metric/icons/bookmark/bookmark-icon-2.svg");
    }

    public Node getBookmarkGroupItemIcon() {
        return createSvgIcon("/io/softwareecg/awb/explorer/metric/icons/bookmark/bookmark-group-icon3.svg");
    }

    public Node getMetricItemIcon() {
        return createSvgIcon("/io/softwareecg/awb/explorer/metric/icons/series/logical/metric-icon.svg");
    }

    public Node getMetricGroupItemIcon() {
        return createSvgIcon("/io/softwareecg/awb/explorer/metric/icons/series/logical/metric-icon.svg");
    }

    public Node getCounterItemIcon() {
        return createSvgIcon("/io/softwareecg/awb/explorer/metric/icons/series/logical/metric-icon.svg");
    }

    public Node getMeasurementItemIcon() {
        return createSvgIcon("/io/softwareecg/awb/explorer/metric/icons/series/measurements-icon.svg");
    }

    public Node getMeasurementGroupItemIcon() {
        return createSvgIcon("/io/softwareecg/awb/explorer/metric/icons/series/measurements-icon.svg");
    }

    public Node getHostGroupItemIcon() {
        return createSvgIcon("/io/softwareecg/awb/explorer/metric/icons/series/physical/hosts-icon2.svg");
    }

    public Node getHostItemIcon() {
        return createSvgIcon("/io/softwareecg/awb/explorer/metric/icons/series/physical/hosts-icon2.svg");
    }

    public Node getProcessGroupItemIcon() {
        return createSvgIcon("/io/softwareecg/awb/explorer/metric/icons/series/process-icon2.svg");
    }

    public Node getProcessItemIcon() {
        return createSvgIcon("/io/softwareecg/awb/explorer/metric/icons/series/process-icon2.svg");
    }

    public Node getContainerItemIcon() {
        return createSvgIcon("/io/softwareecg/awb/explorer/metric/icons/series/logical/container-icon.svg");
    }

    public Node getNamespaceGroupItemIcon(CloudPlatformType platformType) {
        if (platformType == CloudPlatformType.OPEN_SHIFT) {
            return createSvgIcon("/io/softwareecg/awb/explorer/metric/icons/series/logical/openshift-icon.svg");

        } else if (platformType == CloudPlatformType.KUBERNETES) {
            return createSvgIcon("/io/softwareecg/awb/explorer/metric/icons/series/logical/kubernetes-icon.svg");
        }

        return createSvgIcon("/io/softwareecg/awb/explorer/metric/icons/series/logical/namespace-icon.svg");
    }

    public Node getNamespaceItemIcon(CloudPlatformType platformType) {
        if (platformType == CloudPlatformType.OPEN_SHIFT) {
            return createSvgIcon("/io/softwareecg/awb/explorer/metric/icons/series/logical/openshift-icon.svg");

        } else if (platformType == CloudPlatformType.KUBERNETES) {
            return createSvgIcon("/io/softwareecg/awb/explorer/metric/icons/series/logical/kubernetes-icon.svg");
        }

        return createSvgIcon("/io/softwareecg/awb/explorer/metric/icons/series/logical/namespace-icon.svg");
    }

    public Node getPodItemIcon() {
        return createSvgIcon("/io/softwareecg/awb/explorer/metric/icons/series/logical/pod-icon.svg");
    }

    public Node getServiceGroupItemIcon() {
        return createSvgIcon("/io/softwareecg/awb/explorer/metric/icons/series/logical/service-icon.svg");
    }

    public Node getServiceItemIcon() {
        return createPngIcon("/io/softwareecg/awb/explorer/metric/icons/bookmark/bookmark.png");
    }

    private Node createPngIcon(String path) {
        ImageView icon = new ImageView(new Image(getClass().getResourceAsStream(path)));
        icon.setPreserveRatio(true);
        icon.setFitWidth(18);

        return icon;
    }

    private Node createSvgIcon(String iconResourcePath) {
        return cachingSvgLoader.getSvgImage(iconResourcePath);
    }
}

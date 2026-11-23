package io.softwareecg.awb.explorer.metric;

import io.softwareecg.awb.uitoolkit.tree.api.AbstractItem;
import io.softwareecg.awb.explorer.items.ProjectItem;
import io.softwareecg.awb.projectcontext.model.CloudPlatformType;
import io.softwareecg.awb.runtime.resources.CachingSvgLoader;
import javafx.scene.Node;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

/**
 * The default implementation of the {@link MetricTreeIconsProvider} interface
 * that is optimized to the domain IT-systems and serves icons that will fits optimal to this context.
 *
 * @since 20.09.2018
 */
public class DefaultTreeIconsProvider implements MetricTreeIconsProvider {

    protected CachingSvgLoader cachingSvgLoader = new CachingSvgLoader().setDefaultScale(0.05);

    @Override
    public Node getProjectItemIcon(ProjectItem item) {
        return createSvgIcon("/io/softwareecg/awb/explorer/metric/icons/project-icon3.svg");
    }

    @Override
    public Node getReproBookmarksItemIcon(ContextAwareItem item) {
        return createSvgIcon("/io/softwareecg/awb/explorer/metric/icons/bookmark/bookmark-group-icon.svg");
    }

    @Override
    public Node getBookmarkItemIcon(ContextAwareItem item) {
        return createSvgIcon("/io/softwareecg/awb/explorer/metric/icons/bookmark/bookmark-icon-2.svg");
    }

    @Override
    public Node getBookmarkGroupItemIcon(ContextAwareItem item) {
        return createSvgIcon("/io/softwareecg/awb/explorer/metric/icons/bookmark/bookmark-group-icon3.svg");
    }

    @Override
    public Node getMetricItemIcon(ContextAwareItem item) {
        return createSvgIcon("/io/softwareecg/awb/explorer/metric/icons/series/logical/metric-icon.svg");
    }

    @Override
    public Node getMetricGroupItemIcon(ContextAwareItem item) {
        return createSvgIcon("/io/softwareecg/awb/explorer/metric/icons/series/logical/metric-icon.svg");
    }

    @Override
    public Node getCounterItemIcon(ContextAwareItem item) {
        return createSvgIcon("/io/softwareecg/awb/explorer/metric/icons/series/logical/metric-icon.svg");
    }

    @Override
    public Node getMeasurementItemIcon(ContextAwareItem item) {
        return createSvgIcon("/io/softwareecg/awb/explorer/metric/icons/series/measurements-icon.svg");
    }

    @Override
    public Node getMeasurementGroupItemIcon(ContextAwareItem item) {
        return createSvgIcon("/io/softwareecg/awb/explorer/metric/icons/series/measurements-icon.svg");
    }

    @Override
    public Node getHostGroupItemIcon(ContextAwareItem item) {
        return createSvgIcon("/io/softwareecg/awb/explorer/metric/icons/series/physical/hosts-icon2.svg");
    }

    @Override
    public Node getHostItemIcon(ContextAwareItem item) {
        return createSvgIcon("/io/softwareecg/awb/explorer/metric/icons/series/physical/hosts-icon2.svg");
    }

    @Override
    public Node getProcessGroupItemIcon(ContextAwareItem item) {
        return createSvgIcon("/io/softwareecg/awb/explorer/metric/icons/series/process-icon2.svg");
    }

    @Override
    public Node getProcessItemIcon(ContextAwareItem item) {
        return createSvgIcon("/io/softwareecg/awb/explorer/metric/icons/series/process-icon2.svg");
    }

    @Override
    public Node getContainerItemIcon(ContextAwareItem item) {
        return createSvgIcon("/io/softwareecg/awb/explorer/metric/icons/series/logical/container-icon.svg");
    }

    @Override
    public Node getNamespaceGroupItemIcon(ContextAwareItem item) {
        CloudPlatformType platformType = item.getProject().getCloudPlatformType();

        if (platformType == CloudPlatformType.OPEN_SHIFT) {
            return createSvgIcon("/io/softwareecg/awb/explorer/metric/icons/series/logical/openshift-icon.svg");

        } else if (platformType == CloudPlatformType.KUBERNETES) {
            return createSvgIcon("/io/softwareecg/awb/explorer/metric/icons/series/logical/kubernetes-icon.svg");
        }

        return createSvgIcon("/io/softwareecg/awb/explorer/metric/icons/series/logical/namespace-icon.svg");
    }

    @Override
    public Node getNamespaceItemIcon(ContextAwareItem item) {
        CloudPlatformType platformType = item.getProject().getCloudPlatformType();

        if (platformType == CloudPlatformType.OPEN_SHIFT) {
            return createSvgIcon("/io/softwareecg/awb/explorer/metric/icons/series/logical/openshift-icon.svg");

        } else if (platformType == CloudPlatformType.KUBERNETES) {
            return createSvgIcon("/io/softwareecg/awb/explorer/metric/icons/series/logical/kubernetes-icon.svg");
        }

        return createSvgIcon("/io/softwareecg/awb/explorer/metric/icons/series/logical/namespace-icon.svg");
    }

    @Override
    public Node getPodItemIcon(ContextAwareItem item) {
        return createSvgIcon("/io/softwareecg/awb/explorer/metric/icons/series/logical/pod-icon.svg");
    }

    @Override
    public Node getServiceGroupItemIcon(ContextAwareItem item) {
        return createSvgIcon("/io/softwareecg/awb/explorer/metric/icons/series/logical/service-icon.svg");
    }

    @Override
    public Node getServiceItemIcon(ContextAwareItem item) {
        return createPngIcon("/io/softwareecg/awb/explorer/metric/icons/bookmark/bookmark.png");
    }

    @Override
    public Node getGenericItemIcon(AbstractItem treeItem) {
        return createPngIcon("/io/softwareecg/awb/explorer/metric/icons/bookmark/bookmark.png");
    }

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

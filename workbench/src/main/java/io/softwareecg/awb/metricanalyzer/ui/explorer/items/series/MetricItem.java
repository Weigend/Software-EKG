//______________________________________________________________________________
//
//          ProjectConfiguration:    Software EKG
//______________________________________________________________________________
//
//         Author:      Weigend AM 2009 - 2014
//______________________________________________________________________________
//
// Notice: This piece of software was created, designed and implemented by
// experienced craftsmen and innovators in Munich, Germany.
// Changes should be done with respect to the original design.
//______________________________________________________________________________
package io.softwareecg.awb.metricanalyzer.ui.explorer.items.series;

import io.softwareecg.awb.common.ui.events.OpeningMode;
import io.softwareecg.awb.metricanalyzer.bl.tsquery.query.QueryContextEvent;
import io.softwareecg.awb.metricanalyzer.bl.tsquery.query.QueryFilterParams;
import io.softwareecg.awb.metricanalyzer.ui.api.ContextAwareItem;
import io.softwareecg.awb.metricanalyzer.ui.api.FilterContext;
import io.softwareecg.awb.repository.api.EkgRepository;
import io.softwareecg.wfx.platform.api.EventBus;
import io.softwareecg.awb.sdk.core.log.EkgLogger;
import io.softwareecg.wfx.lookup.api.Lookup;
import io.softwareecg.awb.sdk.datamodel.Counter;
import javafx.event.EventHandler;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import org.slf4j.Logger;
import java.util.EventObject;

/**
 * Explorer tree item for a single counter metric.
 *
 */
public class MetricItem extends ContextAwareItem {

    private static final Logger LOGGER = EkgLogger.get();

    private final QueryFilterParams queryParams;

    private final EventBus<EventObject> eventBus = Lookup.lookup(EventBus.class);

    /**
     * Creates a MetricItem with the given parameter.
     *
     * @param counter    the counter to use as name
     * @param filterContext bean that define filters that relating to series data or the view flavor and control the item rendering
     * @param queryParams an MetricQueryParams instance with all filter params need to get the data this item represents
     * @param repository the containing types.
     */
    public MetricItem(Counter counter, FilterContext filterContext, QueryFilterParams queryParams, EkgRepository repository) {
        super(counter.getCounterName(), filterContext, repository);
        super.setGraphic(getIconProvider().getMetricItemIcon(this));
        this.queryParams = queryParams;


        addContextMenuEntry("Show and keep existing metrics", e -> eventBus.publish(
                new QueryContextEvent(this, queryParams, getRepository(), OpeningMode.MERGE_VIEW)));

        addContextMenuEntry("Show and replace existing metrics", e -> eventBus.publish(
                new QueryContextEvent(this, queryParams, getRepository(), OpeningMode.CLEAR_VIEW)));

    }

    @Override
    public boolean isLeaf() {
        return true;
    }

    /**
     * Gets the context of the current item
     *
     * @return the query context
     */
    public QueryFilterParams getQueryParams() {
        return queryParams;
    }

    @Override
    @SuppressWarnings("unchecked")
    public EventHandler<MouseEvent> getDoubleClickHandler() {
        return e -> {
            QueryContextEvent event = new QueryContextEvent(this, new QueryFilterParams.Builder(getQueryParams())
                    .withMultiMetricMode(false).build(), getRepository(), OpeningMode.CLEAR_VIEW);
            eventBus.publish(event);
        };
    }

    /**
     * Get the event handler for a enter pressed event on the explorerView item.
     * <p>
     * The default handler do nothing.
     *
     * @return The event handler.
     */
    @Override
    @SuppressWarnings("unchecked")
    public EventHandler<KeyEvent> getEnterPressedHandler() {
        return e -> {
            //Avoid stack overflow for to many values
            if ("*".equals(getQueryParams().getMetricName())) {
                return;
            }

            eventBus.publish(
                    new QueryContextEvent(this, new QueryFilterParams.Builder(getQueryParams())
                            .withMultiMetricMode(true).build(), getRepository(), OpeningMode.CLEAR_VIEW));
        };
    }
}
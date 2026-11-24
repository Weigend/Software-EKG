package io.softwareecg.awb.metricview.queryeditor;

import io.softwareecg.awb.query.model.ComputationSpec;
import io.softwareecg.awb.query.model.QuerySpec;
import io.softwareecg.awb.query.model.SeriesCombineMode;
import io.softwareecg.awb.query.model.SeriesSmoothingGranularity;
import io.softwareecg.awb.query.model.SeriesSmoothingType;
import io.softwareecg.awb.repository.api.Repository;
import io.softwareecg.awb.uitoolkit.events.OpeningMode;
import javafx.stage.Window;

/** Boundary used by the metric view to drive a query editor without knowing its controller. */
public interface QueryEditorPort {
    QuerySpec getDefinedFilterParameters();
    ComputationSpec getDefinedComputedParameters();
    Window getOwnerWindow();
    void setQueryContext(QuerySpec queryContext, OpeningMode openingMode);
    void setRepository(Repository repository);
    void setComputeParameters(ComputationSpec parameters);
    void initButtonHandler(Actions actions);

    /** Optional callbacks emitted by a query editor. */
    class Actions {
        public void pushSnapshot() {}
        public void clearCharts() {}
        public void updateMetricsGraph() {}
        public void createBookmark() {}
        public void changeSmoothingGranularity(SeriesSmoothingGranularity value) {}
        public void changeSmoothingType(SeriesSmoothingType value) {}
        public void changeSeriesCombineMode(SeriesCombineMode value) {}
        public void changeGraphType(String value) {}
        public void changeThreshold(int value) {}
        public void changeMaxDisplaySeries(int value) {}
    }
}

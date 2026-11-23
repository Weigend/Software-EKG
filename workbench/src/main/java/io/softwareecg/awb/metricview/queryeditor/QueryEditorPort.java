package io.softwareecg.awb.metricview.queryeditor;

import io.softwareecg.awb.investigation.model.ComputationSpec;
import io.softwareecg.awb.investigation.model.QuerySpec;
import io.softwareecg.awb.investigation.model.SeriesCombineMode;
import io.softwareecg.awb.investigation.model.SeriesSmoothingGranularity;
import io.softwareecg.awb.investigation.model.SeriesSmoothingType;
import io.softwareecg.awb.repository.api.EkgRepository;
import io.softwareecg.awb.uitoolkit.events.OpeningMode;
import javafx.stage.Window;

/** Boundary used by the metric view to drive a query editor without knowing its controller. */
public interface QueryEditorPort {
    QuerySpec getDefinedFilterParameters();
    ComputationSpec getDefinedComputedParameters();
    Window getOwnerWindow();
    void setQueryContext(QuerySpec queryContext, OpeningMode openingMode);
    void setRepository(EkgRepository repository);
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

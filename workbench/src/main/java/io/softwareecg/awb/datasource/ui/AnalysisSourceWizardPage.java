package io.softwareecg.awb.datasource.ui;

import io.softwareecg.awb.datasource.model.CollectionPlan;
import io.softwareecg.awb.datasource.spi.AnalysisTopic;
import javafx.scene.Node;

import java.util.Set;

/** UI contribution for one source-specific "Where" step of New Analysis. */
public interface AnalysisSourceWizardPage {
    String sourceId();
    Node node();
    String title(Set<AnalysisTopic> topics);
    String hint(Set<AnalysisTopic> topics, String existingAnalysis);
    boolean isComplete();
    CollectionPlan plan(String projectName);

    default void selectedTopics(Set<AnalysisTopic> topics) { }
    default void focus() { }
    default void showMessage(String text, boolean problem) { }
}

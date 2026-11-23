package io.softwareecg.awb.datasource.ui;

import io.softwareecg.awb.datasource.model.CollectionPlan;
import io.softwareecg.awb.datasource.spi.AnalysisTopic;
import javafx.beans.property.StringProperty;
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

    /**
     * Called once when the dialog is built. The analysis has one name, asked on the last step,
     * whichever page that is: the page shows a field for it and binds the field to
     * {@code analysisName}. With {@code nameIsFixed} the source is added to an existing analysis;
     * the name is given and must not be changed. The page calls {@code changed} whenever what
     * {@link #isComplete()} answers may have changed, so that the dialog can offer the start.
     */
    default void attach(StringProperty analysisName, boolean nameIsFixed, Runnable changed) { }

    default void selectedTopics(Set<AnalysisTopic> topics) { }
    default void focus() { }
    default void showMessage(String text, boolean problem) { }
}

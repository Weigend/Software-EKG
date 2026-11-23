package io.softwareecg.awb.datasource.spi;

import java.util.Map;

/**
 * Workbench extension point for a source shown by New Analysis. The map describes the topics for
 * which the source is offered and the text shown for each topic.
 */
public interface AnalysisSourceProvider {
    AnalysisSourceDefinition definition();
    Map<AnalysisTopic, AnalysisSourceOption> topics();
}

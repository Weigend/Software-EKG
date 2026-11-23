package io.softwareecg.awb.datasource.spi;

/** A source offered for one analysis topic. */
public record AnalysisSourceOption(AnalysisSourceDefinition source, String description, boolean available) {
    public AnalysisSourceOption(AnalysisSourceDefinition source, String description) {
        this(source, description, source.isAvailable());
    }
}

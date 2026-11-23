package io.softwareecg.awb.metadata.model;

import java.time.LocalDateTime;

/**
 * Data Transfer Object fuer eine Zeile in der Projects-Tabelle.
 */
public class ProjectRow {
    public String id;
    public String name;
    public String description;
    public String projectFlavor;
    public String cloudPlatformType;
    public boolean isSplitSource;
    public String importerId;
    public String targetRepositoryId;
    public LocalDateTime createdDate;
    public LocalDateTime lastModifiedDate;
}

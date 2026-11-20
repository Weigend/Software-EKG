package io.softwareecg.awb.da.api.projects;

/**
 * DTO fuer eine Zeile in der project_dimension_aliases-Tabelle.
 */
public class DimensionAliasRow {
    public String id;
    public String projectName;
    public String aliasHostGroup;
    public String aliasHost;
    public String aliasNamespace;
    public String aliasService;
    public String aliasPod;
    public String aliasContainer;
    public String aliasMeasurement;
    public String aliasProcess;
    public String aliasMetricGroup;
    public String aliasMetricName;
}

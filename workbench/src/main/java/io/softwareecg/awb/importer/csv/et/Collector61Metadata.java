package io.softwareecg.awb.importer.csv.et;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * An simple data entity that represents the metadata information
 * the EKG-Collector uses in 6.1 format to describe labels/filter dimension
 * that belongs to the measured metric values.
 * <p>
 * What a newer collector adds to the metadata is ignored instead of making the file unreadable.
 *
 * @since 08.12.2018
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Collector61Metadata {

    private double logFormatVersion;

    @JsonProperty("metadata")
    private DimensionData metadata;

    public double getLogFormatVersion() {
        return logFormatVersion;
    }

    public String getEkgProjectName() {
        return metadata.getEkgProjectName();
    }

    public String getHostGroupName() {
        return metadata.getHostGroupName();
    }

    public String getHostName() {
        return metadata.getHostName();
    }

    public String getNamespace() {
        return metadata.getNamespace();
    }

    public String getService() {
        return metadata.getService();
    }

    public String getPod() {
        return metadata.getPod();
    }

    public String getContainer() {
        return metadata.getContainer();
    }

    /**
     * @return the id of the process group of the collector configuration; {@code null} for a
     *         file of a collector that did not write it yet
     */
    public String getProcessGroup() {
        return metadata.getProcessGroup();
    }

    public String getProcess() {
        return metadata.getProcess();
    }

    public String getMeasurement() {
        return metadata.getMeasurement();
    }

    public String getMetricGroup() {
        return metadata.getMetricGroup();
    }


    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class DimensionData {

        private String ekgProjectName;

        private String hostGroupName;

        private String hostName;

        private String namespace;

        private String service;

        private String pod;

        private String container;

        private String processGroup;

        private String process;

        private String measurement;

        private String metricGroup;

        public String getEkgProjectName() {
            return ekgProjectName;
        }

        public String getHostGroupName() {
            return hostGroupName;
        }

        public String getHostName() {
            return hostName;
        }

        public String getNamespace() {
            return namespace;
        }

        public String getService() {
            return service;
        }

        public String getPod() {
            return pod;
        }

        public String getContainer() {
            return container;
        }

        public String getProcessGroup() {
            return processGroup;
        }

        public String getProcess() {
            return process;
        }

        public String getMeasurement() {
            return measurement;
        }

        public String getMetricGroup() {
            return metricGroup;
        }
    }
}

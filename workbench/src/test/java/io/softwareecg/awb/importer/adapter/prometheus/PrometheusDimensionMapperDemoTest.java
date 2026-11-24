package io.softwareecg.awb.importer.adapter.prometheus;

import io.softwareecg.awb.importer.model.RawSeriesData;
import io.softwareecg.awb.importer.model.SeriesData;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

/**
 * Demo test that shows the mapping results with REAL Prometheus data
 * from http://node0.cloud.local:9090
 * 
 * Uses actual metrics from:
 * - job=node (node_exporter on node0-4.cloud.local:9100)
 * - job=solr (Solr on node1.cloud.local:9854)
 * - job=zookeeper (ZooKeeper on node1-3.cloud.local:7070)
 * - job=spark-master (Spark on node0.cloud.local:8081)
 * - job=spark-worker (Spark workers on node1-4.cloud.local:8082)
 */
public class PrometheusDimensionMapperDemoTest {

    @Test
    public void testMappingWithRealPrometheusData() {
        System.out.println("\n========================================");
        System.out.println("PROMETHEUS DIMENSION MAPPING - REAL DATA");
        System.out.println("Source: http://node0.cloud.local:9090");
        System.out.println("========================================\n");

        // Create metrics from REAL Prometheus data
        List<RawSeriesData> metrics = createRealPrometheusMetrics();

        // First, analyze all metrics
        GenericPrometheusMapper prometheusMapper = new GenericPrometheusMapper();
        prometheusMapper.analyzeAll(metrics);
        System.out.println("Analyzed " + prometheusMapper.getAnalyzedCount() + " metrics\n");

        // Create the dimension mapper with the pre-analyzed mapper
        PrometheusDimensionMapper mapper = new PrometheusDimensionMapper("TestProject", prometheusMapper);

        // Map each metric and print results
        System.out.println("MAPPING RESULTS:");
        System.out.println("----------------\n");

        for (RawSeriesData raw : metrics) {
            SeriesData result = mapper.apply(raw);
            
            if (result == null) {
                System.out.println("METRIC: " + raw.getMetricName());
                System.out.println("  -> SKIPPED (info metric or filtered)");
                System.out.println();
                continue;
            }

            System.out.println("METRIC: " + raw.getMetricName());
            System.out.println("  Labels: " + raw.getSeriesLabels());
            System.out.println("  ---");
            System.out.println("  ProcessGroup : " + result.getProcessGroup());
            System.out.println("  Process      : " + result.getProcess());
            System.out.println("  HostGroup    : " + result.getHostGroup());
            System.out.println("  Host         : " + result.getHost());
            System.out.println("  MetricGroup  : " + result.getMetricGroup());
            System.out.println("  MetricName   : " + result.getMetricName());
            System.out.println();
        }

        System.out.println("========================================");
        System.out.println("SUMMARY: Successfully mapped " + metrics.size() + " metrics");
        System.out.println("========================================\n");
    }

    /**
     * Creates RawSeriesData from REAL Prometheus metrics queried from
     * http://node0.cloud.local:9090
     */
    private List<RawSeriesData> createRealPrometheusMetrics() {
        List<RawSeriesData> metrics = new ArrayList<>();

        // ===================================================================
        // REAL: node_cpu_seconds_total from job=node
        // Query: curl "http://node0.cloud.local:9090/api/v1/query?query=node_cpu_seconds_total"
        // ===================================================================
        RawSeriesData nodeCpu1 = new RawSeriesData("node_cpu_seconds_total");
        nodeCpu1.addLabel("job", "node");
        nodeCpu1.addLabel("instance", "node1.cloud.local:9100");
        nodeCpu1.addLabel("cpu", "0");
        nodeCpu1.addLabel("mode", "idle");
        metrics.add(nodeCpu1);

        RawSeriesData nodeCpu2 = new RawSeriesData("node_cpu_seconds_total");
        nodeCpu2.addLabel("job", "node");
        nodeCpu2.addLabel("instance", "node1.cloud.local:9100");
        nodeCpu2.addLabel("cpu", "0");
        nodeCpu2.addLabel("mode", "iowait");
        metrics.add(nodeCpu2);

        RawSeriesData nodeCpu3 = new RawSeriesData("node_cpu_seconds_total");
        nodeCpu3.addLabel("job", "node");
        nodeCpu3.addLabel("instance", "node2.cloud.local:9100");
        nodeCpu3.addLabel("cpu", "1");
        nodeCpu3.addLabel("mode", "user");
        metrics.add(nodeCpu3);

        // ===================================================================
        // REAL: node_memory_MemAvailable_bytes from job=node
        // Query: curl "http://node0.cloud.local:9090/api/v1/query?query=node_memory_MemAvailable_bytes"
        // ===================================================================
        RawSeriesData nodeMemory1 = new RawSeriesData("node_memory_MemAvailable_bytes");
        nodeMemory1.addLabel("job", "node");
        nodeMemory1.addLabel("instance", "node1.cloud.local:9100");
        metrics.add(nodeMemory1);

        RawSeriesData nodeMemory2 = new RawSeriesData("node_memory_MemAvailable_bytes");
        nodeMemory2.addLabel("job", "node");
        nodeMemory2.addLabel("instance", "node0.cloud.local:9100");
        metrics.add(nodeMemory2);

        // ===================================================================
        // REAL: jvm_memory_bytes_used from job=zookeeper
        // Query: curl "http://node0.cloud.local:9090/api/v1/query?query=jvm_memory_bytes_used"
        // ===================================================================
        RawSeriesData jvmMemHeap = new RawSeriesData("jvm_memory_bytes_used");
        jvmMemHeap.addLabel("job", "zookeeper");
        jvmMemHeap.addLabel("instance", "node1.cloud.local:7070");
        jvmMemHeap.addLabel("area", "heap");
        metrics.add(jvmMemHeap);

        RawSeriesData jvmMemNonHeap = new RawSeriesData("jvm_memory_bytes_used");
        jvmMemNonHeap.addLabel("job", "zookeeper");
        jvmMemNonHeap.addLabel("instance", "node1.cloud.local:7070");
        jvmMemNonHeap.addLabel("area", "nonheap");
        metrics.add(jvmMemNonHeap);

        RawSeriesData jvmMemHeap2 = new RawSeriesData("jvm_memory_bytes_used");
        jvmMemHeap2.addLabel("job", "zookeeper");
        jvmMemHeap2.addLabel("instance", "node2.cloud.local:7070");
        jvmMemHeap2.addLabel("area", "heap");
        metrics.add(jvmMemHeap2);

        // ===================================================================
        // REAL: solr_metrics_core_query_requests_total from job=solr
        // Query: curl "http://node0.cloud.local:9090/api/v1/query?query=solr_metrics_core_query_requests_total"
        // ===================================================================
        RawSeriesData solrQuery1 = new RawSeriesData("solr_metrics_core_query_requests_total");
        solrQuery1.addLabel("job", "solr");
        solrQuery1.addLabel("instance", "node1.cloud.local:9854");
        solrQuery1.addLabel("base_url", "http://node3.cloud.local:8983/solr");
        solrQuery1.addLabel("category", "QUERY");
        solrQuery1.addLabel("cluster_id", "c2ede9b7a3");
        solrQuery1.addLabel("collection", "ekg");
        solrQuery1.addLabel("core", "ekg_shard12_replica_n30");
        solrQuery1.addLabel("internal", "false");
        solrQuery1.addLabel("replica", "replica_n30");
        solrQuery1.addLabel("searchHandler", "/debug/dump");
        solrQuery1.addLabel("shard", "shard12");
        metrics.add(solrQuery1);

        RawSeriesData solrQuery2 = new RawSeriesData("solr_metrics_core_query_requests_total");
        solrQuery2.addLabel("job", "solr");
        solrQuery2.addLabel("instance", "node1.cloud.local:9854");
        solrQuery2.addLabel("base_url", "http://node3.cloud.local:8983/solr");
        solrQuery2.addLabel("category", "QUERY");
        solrQuery2.addLabel("cluster_id", "c2ede9b7a3");
        solrQuery2.addLabel("collection", "ekg");
        solrQuery2.addLabel("core", "ekg_shard12_replica_n30");
        solrQuery2.addLabel("internal", "false");
        solrQuery2.addLabel("replica", "replica_n30");
        solrQuery2.addLabel("searchHandler", "/select");
        solrQuery2.addLabel("shard", "shard12");
        metrics.add(solrQuery2);

        // ===================================================================
        // REAL: node_uname_info from job=node (INFO METRIC - should be filtered!)
        // Query: curl "http://node0.cloud.local:9090/api/v1/query?query=node_uname_info"
        // ===================================================================
        RawSeriesData nodeInfo = new RawSeriesData("node_uname_info");
        nodeInfo.addLabel("job", "node");
        nodeInfo.addLabel("instance", "node1.cloud.local:9100");
        nodeInfo.addLabel("domainname", "(none)");
        nodeInfo.addLabel("machine", "x86_64");
        nodeInfo.addLabel("nodename", "node1");
        nodeInfo.addLabel("release", "6.8.0-94-generic");
        nodeInfo.addLabel("sysname", "Linux");
        nodeInfo.addLabel("version", "#96-Ubuntu SMP PREEMPT_DYNAMIC Fri Jan  9 20:36:55 UTC 2026");
        metrics.add(nodeInfo);

        return metrics;
    }
}

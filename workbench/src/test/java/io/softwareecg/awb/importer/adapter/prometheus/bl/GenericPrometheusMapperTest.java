package io.softwareecg.awb.importer.adapter.prometheus.bl;

import io.softwareecg.awb.timeseries.RawSeriesData;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.*;

import io.softwareecg.awb.importer.adapter.prometheus.bl.GenericPrometheusMapper;
/**
 * Tests for the {@link GenericPrometheusMapper} class.
 * 
 * Tests the deterministic mapping rules:
 * - ProcessGroup = job
 * - Process = instance URL
 * - Host = first part of instance
 * - HostGroup = "cluster"
 * - MetricGroup = first segment after job prefix strip
 * - Metric = remaining segments + label suffixes
 */
public class GenericPrometheusMapperTest {

    private GenericPrometheusMapper mapper;

    @Before
    public void setUp() {
        mapper = new GenericPrometheusMapper(100);
    }

    @Test
    public void testBasicTransform_WithJobAndInstance() {
        RawSeriesData raw = new RawSeriesData("jvm_memory_bytes_used");
        raw.addLabel("job", "myapp");
        raw.addLabel("instance", "node1.cloud.local:9404");
        raw.addLabel("area", "heap");
        
        mapper.analyze(raw);
        GenericPrometheusMapper.DimensionalMetric result = mapper.transform(raw);
        
        assertNotNull(result);
        assertEquals("myapp", result.processGroup());
        assertEquals("node1.cloud.local:9404", result.process());
        assertEquals("cluster", result.hostGroup());
        assertEquals("node1", result.host());
        // MetricGroup = first segment after stripping job prefix
        assertEquals("jvm", result.metricGroup());
        // Metric = remaining segments + label suffix
        assertEquals("memory_bytes_used_area_heap", result.metric());
    }

    @Test
    public void testTransform_HostExtraction_WithDot() {
        RawSeriesData raw = new RawSeriesData("cpu_seconds_total");
        raw.addLabel("job", "node");
        raw.addLabel("instance", "server1.example.com:9100");
        
        mapper.analyze(raw);
        GenericPrometheusMapper.DimensionalMetric result = mapper.transform(raw);
        
        assertEquals("server1", result.host());
    }

    @Test
    public void testTransform_HostExtraction_WithoutDot() {
        RawSeriesData raw = new RawSeriesData("cpu_seconds_total");
        raw.addLabel("job", "node");
        raw.addLabel("instance", "localhost:9100");
        
        mapper.analyze(raw);
        GenericPrometheusMapper.DimensionalMetric result = mapper.transform(raw);
        
        assertEquals("localhost", result.host());
    }

    @Test
    public void testTransform_InfoMetricReturnsNull() {
        RawSeriesData raw = new RawSeriesData("node_uname_info");
        raw.addLabel("job", "node");
        raw.addLabel("instance", "server1:9100");
        
        mapper.analyze(raw);
        GenericPrometheusMapper.DimensionalMetric result = mapper.transform(raw);
        
        assertNull("Info metrics should be filtered out", result);
    }

    @Test
    public void testTransform_StripJobPrefix() {
        RawSeriesData raw = new RawSeriesData("node_cpu_seconds_total");
        raw.addLabel("job", "node");
        raw.addLabel("instance", "server1:9100");
        raw.addLabel("cpu", "0");
        raw.addLabel("mode", "idle");
        
        mapper.analyze(raw);
        GenericPrometheusMapper.DimensionalMetric result = mapper.transform(raw);
        
        assertNotNull(result);
        // Job prefix "node_" should be stripped from metric name
        assertFalse(result.metric().startsWith("node_"));
        assertTrue(result.metric().contains("cpu"));
    }

    @Test
    public void testTransform_QuantileSuffix() {
        RawSeriesData raw = new RawSeriesData("http_request_duration_seconds");
        raw.addLabel("job", "api");
        raw.addLabel("instance", "api1:8080");
        raw.addLabel("quantile", "0.99");
        
        mapper.analyze(raw);
        GenericPrometheusMapper.DimensionalMetric result = mapper.transform(raw);
        
        assertNotNull(result);
        assertTrue("Expected _p99 suffix for quantile 0.99", result.metric().endsWith("_p99"));
    }

    @Test
    public void testTransform_WithoutJobLabel() {
        RawSeriesData raw = new RawSeriesData("go_memstats_heap_alloc_bytes");
        raw.addLabel("instance", "server1:9100");
        
        mapper.analyze(raw);
        GenericPrometheusMapper.DimensionalMetric result = mapper.transform(raw);
        
        assertNotNull(result);
        assertEquals("", result.processGroup());
        assertEquals("go", result.metricGroup()); // First prefix
    }

    @Test
    public void testMetricGroupIsAlwaysFirstSegment() {
        // Even with many metrics sharing the same first segment,
        // MetricGroup should always be just the first segment (no expansion)
        for (int i = 0; i < 150; i++) {
            RawSeriesData raw = new RawSeriesData("solr_metrics_core_query_requests_" + i);
            raw.addLabel("job", "solr");
            raw.addLabel("instance", "solr" + i + ":9854");
            mapper.analyze(raw);
        }
        
        RawSeriesData testRaw = new RawSeriesData("solr_metrics_core_query_requests_test");
        testRaw.addLabel("job", "solr");
        testRaw.addLabel("instance", "solr1:9854");
        
        GenericPrometheusMapper.DimensionalMetric result = mapper.transform(testRaw);
        
        assertNotNull(result);
        // After stripping job prefix "solr_", first segment is "metrics"
        assertEquals("metrics", result.metricGroup());
        assertEquals("core_query_requests_test", result.metric());
    }

    @Test
    public void testAnalyzeAll() {
        List<RawSeriesData> metrics = java.util.Arrays.asList(
            createRawData("jvm_memory_used", "app1", "host1:9000"),
            createRawData("jvm_memory_max", "app1", "host1:9000"),
            createRawData("jvm_gc_time", "app1", "host1:9000"),
            createRawData("http_requests_total", "app1", "host1:9000")
        );
        
        mapper.analyzeAll(metrics);
        
        assertEquals(4, mapper.getAnalyzedCount());
        assertTrue(mapper.isAnalysisComplete());
    }

    @Test
    public void testTransformAll() {
        List<RawSeriesData> metrics = java.util.Arrays.asList(
            createRawData("jvm_memory_used", "app1", "host1:9000"),
            createRawData("jvm_memory_max", "app1", "host1:9000"),
            createRawData("node_info", "node", "host1:9100") // Info metric - should be filtered
        );
        
        mapper.analyzeAll(metrics);
        List<GenericPrometheusMapper.DimensionalMetric> results = mapper.transformAll(metrics);
        
        assertEquals("Info metric should be filtered out", 2, results.size());
    }

    @Test
    public void testReset() {
        RawSeriesData raw = createRawData("test_metric", "test", "host:9000");
        mapper.analyze(raw);
        
        assertTrue(mapper.getAnalyzedCount() > 0);
        
        mapper.reset();
        
        assertEquals(0, mapper.getAnalyzedCount());
        assertFalse(mapper.isAnalysisComplete());
    }

    @Test
    public void testIgnoredLabels() {
        RawSeriesData raw = new RawSeriesData("test_metric");
        raw.addLabel("job", "test");
        raw.addLabel("instance", "host:9000");
        raw.addLabel("__name__", "test_metric");  // Should be ignored
        raw.addLabel("fstype", "ext4");           // Should be ignored
        raw.addLabel("cluster_id", "cluster1");   // Should be ignored
        raw.addLabel("mode", "normal");           // Should be included
        
        mapper.analyze(raw);
        GenericPrometheusMapper.DimensionalMetric result = mapper.transform(raw);
        
        assertNotNull(result);
        assertTrue(result.metric().contains("mode_normal"));
        assertFalse(result.metric().contains("fstype"));
        assertFalse(result.metric().contains("cluster_id"));
        assertFalse(result.metric().contains("__name__"));
    }

    @Test
    public void testLabelValueSanitization() {
        RawSeriesData raw = new RawSeriesData("test_metric");
        raw.addLabel("job", "test");
        raw.addLabel("instance", "host:9000");
        raw.addLabel("path", "/api/v1/users");
        
        mapper.analyze(raw);
        GenericPrometheusMapper.DimensionalMetric result = mapper.transform(raw);
        
        assertNotNull(result);
        // Slashes should be replaced with underscores
        assertTrue(result.metric().contains("path__api_v1_users"));
    }

    private RawSeriesData createRawData(String metricName, String job, String instance) {
        RawSeriesData raw = new RawSeriesData(metricName);
        raw.addLabel("job", job);
        raw.addLabel("instance", instance);
        return raw;
    }
}

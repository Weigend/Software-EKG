package io.softwareecg.collector.repository.rdb.solr.client.util;

import io.softwareecg.collector.dt.TimeSeriesHeader;
import io.softwareecg.collector.repository.rdb.api.TimeSeries;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.SolrInputDocument;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;

public class ObjectBinderTest {

    @Test
    public void theDocumentOfASeriesNamesTheGroupOfItsProcess() {
        SolrInputDocument document = ObjectBinder.mapToSolrInputDocument(new TimeSeries("m", header("app-jvm"), "heap"));

        assertEquals("app-servers", document.getFieldValue("ts_hostGroup"));
        assertEquals("app-jvm", document.getFieldValue("ts_processGroup"));
        assertEquals("Checkout API", document.getFieldValue("ts_process"));
        assertEquals("jvm", document.getFieldValue("ts_metricGroup"));
    }

    @Test
    public void aSeriesWithoutAGroupSendsNoSuchField() {
        SolrInputDocument document = ObjectBinder.mapToSolrInputDocument(new TimeSeries("m", header(null), "heap"));

        assertFalse("a collection whose schema has no such field still takes the document",
                document.getFieldNames().contains("ts_processGroup"));
    }

    @Test
    public void theGroupIsReadBackAndARecordWithoutItStaysWithout() {
        SolrDocument withGroup = new SolrDocument();
        withGroup.setField("ts_processGroup", "app-jvm");
        withGroup.setField("ts_process", "Checkout API");
        withGroup.setField("ts_isMerged", false);
        SolrDocument written = new SolrDocument();   // by a collector that did not write the group yet
        written.setField("ts_process", "Checkout API");
        written.setField("ts_isMerged", false);
        SolrDocumentList documents = new SolrDocumentList();
        documents.add(withGroup);
        documents.add(written);

        java.util.List<TimeSeries> series = ObjectBinder.mapToBean(TimeSeries.class, documents);

        assertEquals("app-jvm", series.get(0).getProcessGroupName());
        assertNull(series.get(1).getProcessGroupName());
        assertEquals("Checkout API", series.get(1).getProcessName());
    }

    private static TimeSeriesHeader header(String processGroup) {
        TimeSeriesHeader header = new TimeSeriesHeader();
        header.setEkgProjectName("Shop");
        header.setHostGroupId("app-servers");
        header.setHostName("app-01");
        header.setProcessGroupId(processGroup);
        header.setProcessLabel("Checkout API");
        header.setMetricGroupId("jvm");
        return header;
    }
}

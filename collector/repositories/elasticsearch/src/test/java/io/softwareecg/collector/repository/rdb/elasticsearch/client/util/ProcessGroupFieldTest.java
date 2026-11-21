package io.softwareecg.collector.repository.rdb.elasticsearch.client.util;

import io.softwareecg.collector.dt.TimeSeriesHeader;
import io.softwareecg.collector.repository.rdb.api.TimeSeries;
import org.elasticsearch.common.Strings;
import org.junit.Test;

import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ProcessGroupFieldTest {

    @Test
    public void theDocumentOfASeriesNamesTheGroupOfItsProcess() {
        Map<String, Object> document = ObjectBinder.mapToContentMap(new TimeSeries("m", header("app-jvm"), "heap"));

        assertEquals("app-jvm", document.get("ts_processGroup"));
        assertEquals("Checkout API", document.get("ts_process"));

        assertFalse(ObjectBinder.mapToContentMap(new TimeSeries("m", header(null), "heap")).containsKey("ts_processGroup"));
    }

    @Test
    public void aNewIndexKnowsTheGroupAsAKeywordLikeTheOtherDimensions() throws Exception {
        String mapping = Strings.toString(SchemaBuilder.buildSchema());

        assertTrue(mapping, mapping.contains("\"ts_processGroup\":{\"type\":\"keyword\""));
        assertTrue(mapping, mapping.contains("\"ts_process\":{\"type\":\"keyword\""));
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

package io.softwareecg.collector.source.prometheus;

import org.junit.Test;

import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

public class JsonReaderTest {

    @Test
    @SuppressWarnings("unchecked")
    public void readsAPrometheusAnswer() {
        String json = "{\"status\":\"success\",\"data\":{\"resultType\":\"vector\",\"result\":["
                + "{\"metric\":{\"__name__\":\"up\",\"note\":\"a \\\"b\\\" \\u00e4\\n\"},\"value\":[1763560000.5,\"1\"]}]},"
                + " \"warnings\": [], \"flag\": true, \"nothing\": null, \"n\": -1.5e3}";

        Map<String, Object> answer = (Map<String, Object>) JsonReader.parse(json);

        assertEquals("success", answer.get("status"));
        assertEquals(Boolean.TRUE, answer.get("flag"));
        assertNull(answer.get("nothing"));
        assertEquals(-1500d, answer.get("n"));
        Map<String, Object> series = (Map<String, Object>) ((List<Object>) ((Map<String, Object>) answer.get("data")).get("result")).get(0);
        assertEquals("a \"b\" ä\n", ((Map<String, Object>) series.get("metric")).get("note"));
        assertEquals(List.of(1763560000.5d, "1"), series.get("value"));
    }

    @Test
    public void rejectsBrokenInput() {
        for (String broken : List.of("", "{", "{\"a\":}", "[1,2", "{\"a\":1} x", "<html>login</html>")) {
            try {
                JsonReader.parse(broken);
                fail("accepted: " + broken);
            } catch (IllegalArgumentException expected) {
                // fine
            }
        }
    }
}

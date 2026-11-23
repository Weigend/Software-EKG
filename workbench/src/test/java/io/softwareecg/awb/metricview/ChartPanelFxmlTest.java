package io.softwareecg.awb.metricview;

import org.junit.jupiter.api.Test;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilderFactory;
import java.net.URL;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class ChartPanelFxmlTest {

    @Test
    void every_included_fxml_resource_exists() throws Exception {
        URL chartPanel = ChartPanelFxmlTest.class.getResource("ChartPanel.fxml");
        assertNotNull(chartPanel, "ChartPanel.fxml");

        var document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(chartPanel.openStream());
        var includes = document.getElementsByTagName("fx:include");
        for (int index = 0; index < includes.getLength(); index++) {
            Element include = (Element) includes.item(index);
            String source = include.getAttribute("source");
            URL resolved = new URL(chartPanel, source);
            try (var ignored = resolved.openStream()) {
                // Resolving and opening the resource is the contract under test.
            }
        }
    }
}

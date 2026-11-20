/**
 * ______________________________________________________________________________
 * <p>
 * Project: Software EKG
 * ______________________________________________________________________________
 * <p>
 * created by: f.lautenschlager
 * creation date: 07.03.14 08:34
 * description:
 * ______________________________________________________________________________
 * <p>
 * Copyright: (c) Weigend AM, all rights reserved
 * ______________________________________________________________________________
 */
package io.softwareecg.collector.source.logevents.events;

import io.softwareecg.collector.source.logevents.EventIterator;
import io.softwareecg.collector.source.logevents.types.BaseEvent;
import io.softwareecg.collector.source.logevents.types.BaseType;
import io.softwareecg.collector.source.logevents.types.events.LogType;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.FileReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 */
public class EventIteratorTest {

    /**
     * Sample log file with 10 log entries
     */
    private static String salesLog;
    private static String salesLogStartingWithMultiLine;
    private static String firstSnippet;
    private static String secondSnippet;
    private static String thirdSnippet;
    private static String fourthSnippet;
    private static final String EOL = System.lineSeparator();

    /**
     * The log content
     */
    private static final String salesLogContent = "2014-02-05 10:39:15,141 INFO  [ol-1; w: 8] [tmr ] [     RemoteServicesBackendImpl.callMethod                         ] [                    ,                     ] [  98]: [UserId: 123456] MethodCall: GetTypausstattungRowsCall" + EOL +
            "AngebotsbereichId: 1000110" + EOL +
            "Filter: " + EOL +
            "EG_VERBAUTE_AUSSTATTUNG : true" + EOL +
            "EG_AUSGABE_ID : 1001380" + EOL +
            "EG_BAUREIHE : F30;F31" + EOL +
            "EG_SA_GA_NR : *S255A*;*S2PAA*;*S2VFA*;*S2XEA*" + EOL +
            "EG_DARSTELLUNGSTERMIN : 01.03.2014" + EOL + EOL +
            "2014-02-05 10:39:15,171 INFO  [ol-1; w: 8] [tmr ] [           ProductMasterDataCC.doCall                             ] [                    ,                     ] [1583]: Method to call: getTypausstattungen(). Delegating to ExpertengridBA." + EOL +
            "2014-02-05 10:39:36,583 INFO  [ol-1; w: 8] [tmr ] [   TAEGridGeneratorKommentarBA.collectAndConvertKommentare        ] [                    ,                     ] [  65]: Didnt find any Kommentare, returning empty list!" + EOL +
            "2014-02-05 10:39:36,698 INFO  [ol-1; w: 8] [tmr ] [     RemoteServicesBackendImpl.callMethod                         ] [                    ,                     ] [ 218]: [UserId: 1000075] MethodCall: GetTypausstattungRowsCall" + EOL +
            "Start: 2014-02-05 10:39:15.142; End: 2014-02-05 10:39:36.698; Duration: 0:00:21.556" + EOL + EOL +
            "2014-02-05 10:39:45,304 INFO  [l-1; w: 11] [tmr ] [        DataStorageBackendImpl.save                               ] [                    ,                     ] [ 220]: [UserId: 123456] Save:" + EOL +
            "Filter: " + EOL +
            "EG_VERBAUTE_AUSSTATTUNG : true" + EOL +
            "EG_AUSGABE_ID : 1001380" + EOL +
            "EG_BAUREIHE : F30;F31" + EOL +
            "EG_SA_GA_NR : *S255A*;*S2PAA*;*S2VFA*;*S2XEA*" + EOL +
            "EG_DARSTELLUNGSTERMIN : 01.03.2014" + EOL +
            "dtosToSave: Class: TypausstattungRowDTO, size: 4" + EOL +
            "dtosToDelete: empty" + EOL + EOL +
            "2014-02-05 10:39:45,321 INFO  [l-1; w: 11] [tmr ] [     TransformerFactoryLocator.getFactoryFor                      ] [                    ,                     ] [  98]: Using factory com.xyz.sales.server.transformerFactory.PreiszeileExpertgridRowTransformerFactory@1b4cdc7 to transform the dto of type class com.xyz.sales.server.api.dto.grid.redaktion.TypausstattungRowDTO." + EOL +
            "2014-02-05 10:40:11,151 INFO  [l-1; w: 11] [tmr ] [     TransformerFactoryLocator.getFactoryFor                      ] [                    ,                     ] [  98]: Using factory com.xyz.sales.server.transformerFactory.PreiszeileExpertgridRowTransformerFactory@1b4cdc7 to transform the dto of type class com.xyz.sales.server.api.dto.grid.redaktion.TypausstattungRowDTO." + EOL +
            "2014-02-05 10:40:28,499 INFO  [l-1; w: 11] [tmr ] [     TransformerFactoryLocator.getFactoryFor                      ] [                    ,                     ] [  98]: Using factory com.xyz.sales.server.transformerFactory.PreiszeileExpertgridRowTransformerFactory@1b4cdc7 to transform the dto of type class com.xyz.sales.server.api.dto.grid.redaktion.TypausstattungRowDTO." + EOL +
            "2014-02-05 10:40:44,902 INFO  [l-1; w: 11] [tmr ] [     TransformerFactoryLocator.getFactoryFor                      ] [                    ,                     ] [  98]: Using factory com.xyz.sales.server.transformerFactory.PreiszeileExpertgridRowTransformerFactory@1b4cdc7 to transform the dto of type class com.xyz.sales.server.api.dto.grid.redaktion.TypausstattungRowDTO." + EOL +
            "2014-02-05 10:41:01,493 INFO  [l-1; w: 11] [tmr ] [        DataStorageBackendImpl.save                               ] [                    ,                     ] [ 310]: [UserId: 1000075] Save:" + EOL +
            "Start: 2014-02-05 10:39:45.304; End: 2014-02-05 10:41:01.493; Duration: 0:01:16.189" + EOL;

    private static final String salesLogContentStartingWithMultiLine = "EG_VERBAUTE_AUSSTATTUNG : true" + EOL +
            "EG_AUSGABE_ID : 1001380" + EOL +
            "EG_BAUREIHE : F30;F31" + EOL +
            "EG_SA_GA_NR : *S255A*;*S2PAA*;*S2VFA*;*S2XEA*" + EOL +
            "EG_DARSTELLUNGSTERMIN : 01.03.2014" + EOL + EOL +
            "2014-02-05 10:39:15,171 INFO  [ol-1; w: 8] [tmr ] [           ProductMasterDataCC.doCall                             ] [                    ,                     ] [1583]: Method to call: getTypausstattungen(). Delegating to ExpertengridBA." + EOL +
            "2014-02-05 10:39:36,583 INFO  [ol-1; w: 8] [tmr ] [   TAEGridGeneratorKommentarBA.collectAndConvertKommentare        ] [                    ,                     ] [  65]: Didnt find any Kommentare, returning empty list!" + EOL +
            "2014-02-05 10:39:36,698 INFO  [ol-1; w: 8] [tmr ] [     RemoteServicesBackendImpl.callMethod                         ] [                    ,                     ] [ 218]: [UserId: 1000075] MethodCall: GetTypausstattungRowsCall" + EOL +
            "Start: 2014-02-05 10:39:15.142; End: 2014-02-05 10:39:36.698; Duration: 0:00:21.556" + EOL + EOL +
            "2014-02-05 10:39:45,304 INFO  [l-1; w: 11] [tmr ] [        DataStorageBackendImpl.save                               ] [                    ,                     ] [ 220]: [UserId: 123456] Save:" + EOL +
            "Filter: " + EOL +
            "EG_VERBAUTE_AUSSTATTUNG : true" + EOL +
            "EG_AUSGABE_ID : 1001380" + EOL +
            "EG_BAUREIHE : F30;F31" + EOL +
            "EG_SA_GA_NR : *S255A*;*S2PAA*;*S2VFA*;*S2XEA*" + EOL +
            "EG_DARSTELLUNGSTERMIN : 01.03.2014" + EOL +
            "dtosToSave: Class: TypausstattungRowDTO, size: 4" + EOL +
            "dtosToDelete: empty" + EOL + EOL +
            "2014-02-05 10:39:45,321 INFO  [l-1; w: 11] [tmr ] [     TransformerFactoryLocator.getFactoryFor                      ] [                    ,                     ] [  98]: Using factory com.xyz.sales.server.transformerFactory.PreiszeileExpertgridRowTransformerFactory@1b4cdc7 to transform the dto of type class com.xyz.sales.server.api.dto.grid.redaktion.TypausstattungRowDTO." + EOL +
            "2014-02-05 10:40:11,151 INFO  [l-1; w: 11] [tmr ] [     TransformerFactoryLocator.getFactoryFor                      ] [                    ,                     ] [  98]: Using factory com.xyz.sales.server.transformerFactory.PreiszeileExpertgridRowTransformerFactory@1b4cdc7 to transform the dto of type class com.xyz.sales.server.api.dto.grid.redaktion.TypausstattungRowDTO." + EOL +
            "2014-02-05 10:40:28,499 INFO  [l-1; w: 11] [tmr ] [     TransformerFactoryLocator.getFactoryFor                      ] [                    ,                     ] [  98]: Using factory com.xyz.sales.server.transformerFactory.PreiszeileExpertgridRowTransformerFactory@1b4cdc7 to transform the dto of type class com.xyz.sales.server.api.dto.grid.redaktion.TypausstattungRowDTO." + EOL +
            "2014-02-05 10:40:44,902 INFO  [l-1; w: 11] [tmr ] [     TransformerFactoryLocator.getFactoryFor                      ] [                    ,                     ] [  98]: Using factory com.xyz.sales.server.transformerFactory.PreiszeileExpertgridRowTransformerFactory@1b4cdc7 to transform the dto of type class com.xyz.sales.server.api.dto.grid.redaktion.TypausstattungRowDTO." + EOL +
            "2014-02-05 10:41:01,493 INFO  [l-1; w: 11] [tmr ] [        DataStorageBackendImpl.save                               ] [                    ,                     ] [ 310]: [UserId: 1000075] Save:" + EOL +
            "Start: 2014-02-05 10:39:45.304; End: 2014-02-05 10:41:01.493; Duration: 0:01:16.189" + EOL;

    /**
     * Date format from sales log: 2014-02-05 10:39:15,141
     */
    private final Pattern logLineStartPattern = Pattern.compile("^\\d{4}-\\d{2}-\\d{2}\\s\\d{2}:\\d{2}:\\d{2},\\d{3}.*");

    /**
     * Our test object
     */
    private EventIterator lineIterator;


    @BeforeClass
    public static void beforeTestClass() throws Exception {
        String path = EventIteratorTest.class.getResource("/log-event-config.properties").getPath();
        System.setProperty("collectorPropertiesPath", path);

        // The old multi-repository build referenced sample files that were never
        // part of the module. Build them from the canonical strings in this test,
        // making the test self-contained and independent of a checkout layout.
        Path sampleDir = Files.createTempDirectory("software-ekg-logevents-");
        salesLog = write(sampleDir, "sales.log", salesLogContent);
        salesLogStartingWithMultiLine = write(sampleDir, "salesStartingWithMultiLine.log", salesLogContentStartingWithMultiLine);

        List<String> lines = salesLogContent.lines().toList();
        String storageStart = lines.stream().filter(line -> line.contains("DataStorageBackendImpl.save")).findFirst().orElseThrow();
        String storageEnd = lines.stream().filter(line -> line.contains("Start: 2014-02-05 10:39:45.304")).findFirst().orElseThrow();
        List<String> transformerLines = lines.stream().filter(line -> line.contains("TransformerFactoryLocator.getFactoryFor")).toList();

        firstSnippet = write(sampleDir, "firstSnippet.log", storageStart + EOL);
        secondSnippet = write(sampleDir, "secondSnippet.log", storageEnd + EOL + transformerLines.getFirst() + EOL);
        String fourTransformerLines = String.join(EOL, transformerLines.subList(0, 4)) + EOL;
        thirdSnippet = write(sampleDir, "thirdSnippet.log", fourTransformerLines);
        fourthSnippet = write(sampleDir, "fourthSnippet.log", fourTransformerLines);
    }

    private static String write(Path directory, String name, String content) throws Exception {
        return Files.writeString(directory.resolve(name), content).toString();
    }

    @Before
    public void beforeEachTest() throws Exception {
        lineIterator = new EventIterator(logLineStartPattern, true);
        lineIterator.setReader(new FileReader(salesLog));
    }

    @Test
    public void testLineCount() throws Exception {
        int lineCounter = 0;
        while (lineIterator.hasNext()) {
            lineCounter++;
            lineIterator.next();
        }
        assertThat(lineCounter, is(10));
    }

    @Test
    public void testLineCountForSingleLineStrategy() throws Exception {
        lineIterator = new EventIterator(logLineStartPattern, false);
        lineIterator.setReader(new FileReader(salesLog));

        int lineCounter = 0;
        while (lineIterator.hasNext()) {
            lineCounter++;
            lineIterator.next();
        }
        assertThat(lineCounter, is(30));
    }

    @Test
    public void testLogLinesWithWrongPattern() throws Exception {
        //We have a not matching regex with a "\\." instead a ","
        lineIterator = new EventIterator(Pattern.compile("^\\d{4}-\\d{2}-\\d{2}\\s\\d{2}:\\d{2}:\\d{2}\\.\\d{3}.*"), true);
        lineIterator.setReader(new FileReader(salesLog));
        List<String> logLines = new ArrayList<>();

        while (lineIterator.hasNext()) {
            logLines.add(lineIterator.next());
        }
        assertThat(logLines.size(), is(1));
    }

    @Test
    public void testLogLines() throws Exception {
        StringBuilder sb = new StringBuilder(salesLogContent.length());

        while (lineIterator.hasNext()) {
            sb.append(lineIterator.next()).append("\n");
        }
        sb.delete(sb.length() - 1, sb.length());
        sb.append(lineIterator.flush()).append("\n");

        assertThat(sb.toString(), is(salesLogContent));
    }

    @Test
    public void testLineCountStartingInMultiLine() throws Exception {
        lineIterator = new EventIterator(logLineStartPattern, true);
        lineIterator.setReader(new FileReader(salesLogStartingWithMultiLine));

        int lineCounter = 0;
        while (lineIterator.hasNext()) {
            lineCounter++;
            lineIterator.next();
        }
        assertThat(lineCounter, is(10));
    }

    @Test
    public void testLogLinesStartingInMultiLine() throws Exception {
        lineIterator = new EventIterator(logLineStartPattern, true);
        lineIterator.setReader(new FileReader(salesLogStartingWithMultiLine));


        StringBuilder sb = new StringBuilder(salesLogContent.length());

        while (lineIterator.hasNext()) {
            sb.append(lineIterator.next()).append("\n");
        }
        sb.delete(sb.length() - 1, sb.length());
        sb.append(lineIterator.flush()).append("\n");

        assertThat(sb.toString(), is(salesLogContentStartingWithMultiLine));
    }


    @Test
    public void testNextEvents() throws Exception {
        lineIterator.setReader(new FileReader(firstSnippet));
        List<BaseEvent> events = new ArrayList<>();

        //Iterate over first snippet, only contains the first line of a log event
        List<BaseType> eventTypes = new ArrayList<>();
        eventTypes.add(new LogType("salesDataStorageBackendImpl"));
        while (lineIterator.hasNext()) {
            events.addAll(lineIterator.nextEvents(eventTypes));
        }
        assertThat(events.size(), is(0));

        //Iterate over the second snippet, starting with the second line of the log event
        lineIterator.setReader(new FileReader(secondSnippet));
        while (lineIterator.hasNext()) {
            events.addAll(lineIterator.nextEvents(eventTypes));
        }
        assertThat(events.size(), is(1));
    }

    @Test
    public void testNext() throws Exception {
        String thirdSnippetContent = "2014-02-05 10:39:45,321 INFO  [l-1; w: 11] [tmr ] [     TransformerFactoryLocator.getFactoryFor                      ] [                    ,                     ] [  98]: Using factory com.xyz.sales.server.transformerFactory.PreiszeileExpertgridRowTransformerFactory@1b4cdc7 to transform the dto of type class com.xyz.sales.server.api.dto.grid.redaktion.TypausstattungRowDTO." + EOL +
                "2014-02-05 10:40:11,151 INFO  [l-1; w: 11] [tmr ] [     TransformerFactoryLocator.getFactoryFor                      ] [                    ,                     ] [  98]: Using factory com.xyz.sales.server.transformerFactory.PreiszeileExpertgridRowTransformerFactory@1b4cdc7 to transform the dto of type class com.xyz.sales.server.api.dto.grid.redaktion.TypausstattungRowDTO." + EOL +
                "2014-02-05 10:40:28,499 INFO  [l-1; w: 11] [tmr ] [     TransformerFactoryLocator.getFactoryFor                      ] [                    ,                     ] [  98]: Using factory com.xyz.sales.server.transformerFactory.PreiszeileExpertgridRowTransformerFactory@1b4cdc7 to transform the dto of type class com.xyz.sales.server.api.dto.grid.redaktion.TypausstattungRowDTO." + EOL;
        lineIterator.setReader(new FileReader(thirdSnippet));
        StringBuilder sb = new StringBuilder();

        //Iterate over the snippet. The last line is buffered.
        while (lineIterator.hasNext()) {
            sb.append(lineIterator.next()).append("\n");
        }
        assertThat(sb.toString(), is(thirdSnippetContent));

        String thirdAndFourthSnippetContent = "2014-02-05 10:39:45,321 INFO  [l-1; w: 11] [tmr ] [     TransformerFactoryLocator.getFactoryFor                      ] [                    ,                     ] [  98]: Using factory com.xyz.sales.server.transformerFactory.PreiszeileExpertgridRowTransformerFactory@1b4cdc7 to transform the dto of type class com.xyz.sales.server.api.dto.grid.redaktion.TypausstattungRowDTO." + EOL +
                "2014-02-05 10:40:11,151 INFO  [l-1; w: 11] [tmr ] [     TransformerFactoryLocator.getFactoryFor                      ] [                    ,                     ] [  98]: Using factory com.xyz.sales.server.transformerFactory.PreiszeileExpertgridRowTransformerFactory@1b4cdc7 to transform the dto of type class com.xyz.sales.server.api.dto.grid.redaktion.TypausstattungRowDTO." + EOL +
                "2014-02-05 10:40:28,499 INFO  [l-1; w: 11] [tmr ] [     TransformerFactoryLocator.getFactoryFor                      ] [                    ,                     ] [  98]: Using factory com.xyz.sales.server.transformerFactory.PreiszeileExpertgridRowTransformerFactory@1b4cdc7 to transform the dto of type class com.xyz.sales.server.api.dto.grid.redaktion.TypausstattungRowDTO." + EOL +
                "2014-02-05 10:40:44,902 INFO  [l-1; w: 11] [tmr ] [     TransformerFactoryLocator.getFactoryFor                      ] [                    ,                     ] [  98]: Using factory com.xyz.sales.server.transformerFactory.PreiszeileExpertgridRowTransformerFactory@1b4cdc7 to transform the dto of type class com.xyz.sales.server.api.dto.grid.redaktion.TypausstattungRowDTO." + EOL +
                "2014-02-05 10:39:45,321 INFO  [l-1; w: 11] [tmr ] [     TransformerFactoryLocator.getFactoryFor                      ] [                    ,                     ] [  98]: Using factory com.xyz.sales.server.transformerFactory.PreiszeileExpertgridRowTransformerFactory@1b4cdc7 to transform the dto of type class com.xyz.sales.server.api.dto.grid.redaktion.TypausstattungRowDTO." + EOL +
                "2014-02-05 10:40:11,151 INFO  [l-1; w: 11] [tmr ] [     TransformerFactoryLocator.getFactoryFor                      ] [                    ,                     ] [  98]: Using factory com.xyz.sales.server.transformerFactory.PreiszeileExpertgridRowTransformerFactory@1b4cdc7 to transform the dto of type class com.xyz.sales.server.api.dto.grid.redaktion.TypausstattungRowDTO." + EOL +
                "2014-02-05 10:40:28,499 INFO  [l-1; w: 11] [tmr ] [     TransformerFactoryLocator.getFactoryFor                      ] [                    ,                     ] [  98]: Using factory com.xyz.sales.server.transformerFactory.PreiszeileExpertgridRowTransformerFactory@1b4cdc7 to transform the dto of type class com.xyz.sales.server.api.dto.grid.redaktion.TypausstattungRowDTO." + EOL;

        //Iterate over the second snippet, starting with the second line of the log event
        lineIterator.setReader(new FileReader(fourthSnippet));
        while (lineIterator.hasNext()) {
            sb.append(lineIterator.next()).append("\n");
        }
        //Iterate over the snippet. The last line is buffered.
        assertThat(sb.toString(), is(thirdAndFourthSnippetContent));
    }
}

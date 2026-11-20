/**
 * ______________________________________________________________________________
 *
 * Project: Software EKG
 * ______________________________________________________________________________
 *
 * created by: f.lautenschlager
 * creation date: 19.03.14 09:56
 * description: 
 * ______________________________________________________________________________
 *
 * Copyright: (c) Weigend AM, all rights reserved
 * ______________________________________________________________________________
 */
package io.softwareecg.collector.source.logevents.events;


import io.softwareecg.collector.source.logevents.types.PayloadExtractor;
import org.junit.Test;

import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 */
public class PayloadExtractorTest {

    @Test
    public void testExtractPayloadGroups() throws Exception {
        //given the pattern with two regex groups
        Pattern regexWithPayloadGroups = Pattern.compile("^(?<xxFragexx>.*\\?)\\s(?<xxAntwortxx>.*)$");

        //when the method extract payload groups is called
        Set<String> groups = new PayloadExtractor().extractPayloadGroups(regexWithPayloadGroups.toString());

        //expect that the two groups xxFragexx and xxAntwortxx are contained
        assertThat(groups, hasItems("xxFragexx", "xxAntwortxx"));
    }

    @Test
    public void testExtractPayloadProperties() throws Exception {
        //given
        Pattern regexWithPayloadGroups = Pattern.compile("^(?<xxFragexx>.*\\?)\\s(?<xxAntwortxx>.*)$");
        String text = "Wer reitet da durch Sturm und Wind? Es ist der Vater mit seinem Kind.";

        //when the method extract payload properties is called
        Map<String, String> properties = new PayloadExtractor().extractPayloadProperties(text, regexWithPayloadGroups);

        //expect that
        assertThat(properties.values(), hasItems("Wer reitet da durch Sturm und Wind?", "Es ist der Vater mit seinem Kind."));
    }

    @Test
    public void testExtractPayloadPropertiesWithNullValue() throws Exception{
        //given
        Pattern regexWithPayloadGroups = Pattern.compile("^(?<xxFragexx>.*\\?)\\s((?<xxAntwortxx>Es ist der Vater mit seinem Kind.)|Es ist die Mutter mit dem Kind.)$");
        String text = "Wer reitet da durch Sturm und Wind? Es ist die Mutter mit dem Kind.";

        //when the method extract payload properties is called
        Map<String, String> properties = new PayloadExtractor().extractPayloadProperties(text, regexWithPayloadGroups);

        //expect that
        assertThat(properties.values(), hasItems("Wer reitet da durch Sturm und Wind?", "null"));
    }
}

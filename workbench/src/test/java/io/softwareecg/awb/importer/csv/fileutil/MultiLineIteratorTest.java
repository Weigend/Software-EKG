//______________________________________________________________________________
//
//                  Project:    Software EKG
//______________________________________________________________________________
//
//                   Author:    Weigend AM 2009 - 2017
//______________________________________________________________________________
//
// Notice: This piece of software was created, designed and implemented by
// experienced craftsmen and innovators in Munich, Germany.
// Changes should be done with respect to the original design.
//______________________________________________________________________________
package io.softwareecg.awb.importer.csv.fileutil;

import org.junit.Test;

import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.regex.Pattern;

import static org.junit.Assert.*;

/**
 */
public class MultiLineIteratorTest {

    private final InputStreamReader reader = new InputStreamReader(getClass().getResourceAsStream("multiLineTest.csv"));

    @Test
    public void testStartDetectedAndEntryIsNotEmptyCaseBothFalse() throws Exception {
        MultiLineIterator lineIterator = new MultiLineIterator(reader, Pattern.compile(""));
        Method booleanExpression = MultiLineIterator.class.getDeclaredMethod("startDetectedAndEntryIsNotEmpty", String.class);
        booleanExpression.setAccessible(true);
        boolean result = (boolean) booleanExpression.invoke(lineIterator, "");//invoke with empty string

        assertFalse(result);
    }

    @Test
    public void testStartDetectedAndEntryIsNotEmptyCaseJustNotEmptyQuery() throws Exception {
        MultiLineIterator lineIterator = new MultiLineIterator(reader, Pattern.compile(""));
        Method booleanExpression = MultiLineIterator.class.getDeclaredMethod("startDetectedAndEntryIsNotEmpty", String.class);
        booleanExpression.setAccessible(true);
        boolean result = (boolean) booleanExpression.invoke(lineIterator, "notEmpty");

        assertFalse(result);
    }

    @Test
    public void testStartDetectedAndEntryNotEmptyCaseJustStartDeclared() throws Exception {
        MultiLineIterator lineIterator = new MultiLineIterator(reader, Pattern.compile("A;1;"));
        Method booleanExpression = MultiLineIterator.class.getDeclaredMethod("startDetectedAndEntryIsNotEmpty", String.class);
        booleanExpression.setAccessible(true);
        boolean result = (boolean) booleanExpression.invoke(lineIterator, "A;1;");//invoke with empty string

        assertFalse(result);
    }

    @Test
    public void testStartDetectedAndEntryNotEmptyCaseTrue() throws Exception {
        MultiLineIterator lineIterator = new MultiLineIterator(reader, Pattern.compile("A;1;"));

        Method append = MultiLineIterator.class.getDeclaredMethod("append", String.class);
        append.setAccessible(true);
        append.invoke(lineIterator, "notEmpty");

        Method booleanExpression = MultiLineIterator.class.getDeclaredMethod("startDetectedAndEntryIsNotEmpty", String.class);
        booleanExpression.setAccessible(true);
        boolean result = (boolean) booleanExpression.invoke(lineIterator, "A;1;");//invoke with empty string

        assertTrue(result);
    }

    @Test
    public void testNextLineWorks() throws Exception {
        MultiLineIterator lineIterator = new MultiLineIterator(reader, Pattern.compile("A;1;"));
        lineIterator.nextLine();

        Field stringBuilder = MultiLineIterator.class.getDeclaredField("entry");
        stringBuilder.setAccessible(true);
        StringBuilder builder = (StringBuilder) stringBuilder.get(lineIterator);

        assertEquals("", builder.toString());
    }

    /**
     * If the start is valid and there is already
     * text in the buffer, then only the first line will be added
     * to the buffer and be returned.
     */
    @Test
    public void testNextLineInvalidStart() throws Exception {
        MultiLineIterator lineIterator = new MultiLineIterator(reader, Pattern.compile("A;1;"));

        Field oldBuilder = MultiLineIterator.class.getDeclaredField("entry");
        oldBuilder.setAccessible(true);
        StringBuilder notEmptyBuilder = new StringBuilder();
        notEmptyBuilder.append("not empty");
        oldBuilder.set(lineIterator, notEmptyBuilder);

        lineIterator.nextLine();

        Field stringBuilder = MultiLineIterator.class.getDeclaredField("entry");
        stringBuilder.setAccessible(true);
        StringBuilder builder = (StringBuilder) stringBuilder.get(lineIterator);

        //note all lines in CSV, even the last line, end with a line-breaker
        //don't use \n, line breaker depends on the system we're running on
        String firstLine = "A;1;" + System.lineSeparator();

        assertEquals(firstLine, builder.toString());
    }
}

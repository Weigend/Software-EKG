//______________________________________________________________________________
//
//          Project:    Software EKG
//______________________________________________________________________________
//
//         Author:      Weigend AM 2009 - 2014
//______________________________________________________________________________
//
// Notice: This piece of software was created, designed and implemented by
// experienced craftsmen and innovators in Munich, Germany.
// Changes should be done with respect to the original design.
//______________________________________________________________________________
package io.softwareecg.collector.source.logevents.events;

import io.softwareecg.collector.source.logevents.types.BaseEvent;
import io.softwareecg.collector.source.logevents.types.BaseType;
import io.softwareecg.collector.source.logevents.types.ValueTypes;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsNull.nullValue;

/**
 * Created by f.lautenschlager on 04.04.14.
 */
public class BaseTypeTest {

    private BaseType testObject;

    @Before
    public void beforeEachTest() {
        testObject = new BaseType("^\\d{4}-\\d{2}", "yyyy-mm-DD HH:mm:ss,SSS", "NUMERIC", "DE", "TestGroup", "TestName") {
            @Override
            public BaseEvent parseType(String line) {
                return null;
            }
        };
    }

    @Test
    public void testParseType() throws Exception {
        assertThat(testObject.parseType("line"), nullValue());
    }

    @Test
    public void testStartMatches() throws Exception {
        assertThat(testObject.startMatches("2014-03"), is(true));
    }

    @Test
    public void testEndMatches() throws Exception {
        //Default behaviour
        assertThat(testObject.endMatches(""), is(false));
    }

    @Test
    public void testGetStartPattern() throws Exception {
        assertThat(testObject.getStartPattern().toString(), is("^\\d{4}-\\d{2}"));
    }

    @Test
    public void testGetValueFormat() throws Exception {
        assertThat(testObject.getValueFormat(), is("DE"));
    }

    @Test
    public void testGetValueType() throws Exception {
        assertThat(testObject.getValueType(), is(ValueTypes.NUMERIC));
    }

    @Test
    public void testGetGroup() throws Exception {
        assertThat(testObject.getGroup(), is("TestGroup"));

    }

    @Test
    public void testGetName() throws Exception {
        assertThat(testObject.getName(), is("TestName"));
    }

}

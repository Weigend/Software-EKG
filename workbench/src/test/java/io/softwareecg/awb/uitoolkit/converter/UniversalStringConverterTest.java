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
package io.softwareecg.awb.uitoolkit.converter;

import org.junit.Test;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import io.softwareecg.awb.uitoolkit.converter.Converter;
import io.softwareecg.awb.uitoolkit.converter.UniversalStringConverter;
/**
 * Unit test for the {@link UniversalStringConverter}.
 *
 */
public class UniversalStringConverterTest {

    private final Converter<Integer, String> converter = new UniversalStringConverter<Integer>() {
        @Override
        public String toString(Integer object) {
            return object.toString();
        }

        @Override
        public Integer fromString(String string) {
            return Integer.parseInt(string);
        }
    };

    @Test
    public void testFromFirst() throws Exception {
        assertThat(converter.fromFirst(5), is(equalTo("5")));
    }

    @Test
    public void testFromSecond() throws Exception {
        assertThat(converter.fromSecond("5"), is(equalTo(5)));
    }

    @Test(expected = NumberFormatException.class)
    public void testFromSecondUnableToParse() throws Exception {
        converter.fromSecond("abc5abc");
    }
}

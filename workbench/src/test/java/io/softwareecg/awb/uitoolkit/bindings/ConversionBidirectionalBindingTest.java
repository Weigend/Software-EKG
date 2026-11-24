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
package io.softwareecg.awb.uitoolkit.bindings;

import javafx.beans.property.*;
import org.junit.Before;
import org.junit.Test;

import java.lang.ref.WeakReference;
import java.lang.reflect.Field;

import static io.softwareecg.awb.uitoolkit.bindings.ConversionBidirectionalBinding.getPropertyName;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;

/**
 * Unit test for the {@link ConversionBidirectionalBinding}.
 *
 */
public class ConversionBidirectionalBindingTest {

    private final IntegerProperty property1 = new SimpleIntegerProperty(0);
    private StringProperty property2 = new SimpleStringProperty("null");
    private final StringProperty property2w = new SimpleStringProperty("null");
    private ConversionBidirectionalBinding<Number, String> binding;

    @Before
    public void setUp() throws Exception {
        binding = new ConversionBidirectionalBinding<>(property1, property2, Object::toString, Integer::parseInt);
        property1.addListener(binding);
        property2.addListener(binding);
    }

    @Test
    public void testSetFirst() throws Exception {
        property1.setValue(5);
        assertThat(property2.getValue(), is(equalTo("5")));

        ((WeakReference) getInternalState(binding, "propertyRef2W")).clear();
        property1.setValue(10);
        assertThat(property2.getValue(), is(equalTo("5")));
    }

    @Test
    public void testSetSecond() throws Exception {
        property2.setValue("10");
        assertThat(property1.getValue(), is(equalTo(10)));

        ((WeakReference) getInternalState(binding, "propertyRef1")).clear();
        property2.setValue("5");
        assertThat(property1.getValue(), is(equalTo(10)));
    }

    /**
     * Helper method to access private fields via reflection (replaces deprecated Mockito Whitebox).
     */
    private static Object getInternalState(Object target, String fieldName) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        return field.get(target);
    }

    @Test
    public void testAsymetricBinding() throws Exception {
        property2 = new SimpleStringProperty();
        property2.setValue("0");
        ConversionBidirectionalBinding<Number, String> binding = new ConversionBidirectionalBinding<>(property1, property2, property2w, Object::toString, Integer::parseInt);
        property1.addListener(binding);
        property2.addListener(binding);

        property1.setValue(10);
        assertThat(property2w.get(), is(equalTo("10")));
        assertThat(property2.get(), is(equalTo("0")));

        property2.setValue("15");
        assertThat(property1.get(), is(equalTo(15)));
        assertThat(property2w.get(), is(equalTo("10")));

        property2w.addListener((o, ov, nv) -> property2.setValue(nv));
        property1.setValue(5);
        assertThat(property2.get(), is(equalTo("5")));
        assertThat(property2w.get(), is(equalTo("5")));

        property2.setValue("6");
        assertThat(property1.get(), is(equalTo(6)));
        assertThat(property2w.get(), is(equalTo("5")));
    }

    @Test
    public void testGetPropertyName() throws Exception {
        assertThat(getPropertyName(null), is(equalTo("")));
        assertThat(getPropertyName(new SimpleBooleanProperty(this, "")), is(equalTo("")));
        assertThat(getPropertyName(new SimpleBooleanProperty(null, "abc")), is(equalTo("")));
        assertThat(getPropertyName(new SimpleBooleanProperty(this, "abc")), endsWith(":abc"));
    }
}

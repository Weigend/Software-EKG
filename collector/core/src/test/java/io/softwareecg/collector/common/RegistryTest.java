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

package io.softwareecg.collector.common;

import io.softwareecg.collector.common.util.IService;
import org.junit.Test;

import java.util.Iterator;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;

/**
 * Created by johannes.weigend on 06.03.14.
 */
public class RegistryTest {

    @Test
    public void testGetDefault() throws Exception {
        Registry registry = Registry.getDefault();
        assertNotNull(registry);
    }

    @Test
    public void testLookup() throws Exception {
        Registry registry = Registry.getDefault();
        IService service1 = registry.lookup(IService.class);
        assertNotNull(service1);
        IService service2 = registry.lookup(IService.class);
        assertNotNull(service2);
    }

    @Test
    public void testLookupAll() throws Exception {
        Registry registry = Registry.getDefault();
        Iterator<IService> result = registry.lookupAll(IService.class);
        int size = 0;
        while (result.hasNext()) {
            Object next = result.next();
            assertNotNull(next);
            size++;
        }
        assertSame(size, 2);

    }
}

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
package io.softwareecg.awb.repository.events;

import io.softwareecg.awb.repository.api.EkgRepository;
import io.softwareecg.awb.runtime.events.ChangedEvent;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.mock;

/**
 * Unit test for the {@link RepositoryConfigChangedEvent}
 *
 */
public class RepositoryConfigChangedEventTest {

    @Test(expected = IllegalArgumentException.class)
    public void testNullAsSource() {
        new RepositoryConfigChangedEvent(mock(EkgRepository.class), ChangedEvent.Change.UPDATE, null);
    }

    @Test
    public void testObjectNull() {
        RepositoryConfigChangedEvent event = new RepositoryConfigChangedEvent(null, ChangedEvent.Change.UPDATE, this);
        assertThat(event.getObj(), is(nullValue()));
    }

    @Test
    public void testChangeNull() {
        RepositoryConfigChangedEvent event = new RepositoryConfigChangedEvent(mock(EkgRepository.class), null, this);
        assertThat(event.getChange(), is(nullValue()));
    }

    @Test
    public void testGetObj() {
        EkgRepository mock = mock(EkgRepository.class);
        RepositoryConfigChangedEvent event = new RepositoryConfigChangedEvent(mock, ChangedEvent.Change.UPDATE, this);
        MatcherAssert.assertThat(event.getChange(), Matchers.is(ChangedEvent.Change.UPDATE));
        assertThat(event.getObj(), is(equalTo(mock)));
    }
}

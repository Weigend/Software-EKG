package io.softwareecg.awb.bookmark;

import io.softwareecg.awb.bookmark.model.BookmarkGroup;
import io.softwareecg.awb.bookmark.model.MetricQueryBookmark;
import io.softwareecg.awb.metadata.model.BookmarkGroupRow;
import io.softwareecg.awb.metadata.model.MetricQueryBookmarkRow;
import io.softwareecg.awb.metadata.port.BookmarkStore;
import io.softwareecg.awb.query.model.QuerySpec;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * Which group and which project a new bookmark belongs to.
 */
public class MetricsBookmarkServiceImplTest {

    private final BookmarkStore store = mock(BookmarkStore.class);

    @Test
    public void withoutAGroupItBelongsToTheProjectItWasMadeIn() {
        MetricQueryBookmarkRow row = storedIn(new BookmarkGroup("", BookmarkGroup.DEFAULT_GLOBAL_GROUP_ID));

        assertEquals("p", row.projectName);
        assertEquals(BookmarkGroup.DEFAULT_GLOBAL_GROUP_ID, row.bookmarkGroupId);
        verify(store, never()).insertGroup(any());
    }

    @Test
    public void aNewGroupIsStoredAndIsGlobal() {
        MetricQueryBookmarkRow row = storedIn(new BookmarkGroup("Shared"));

        ArgumentCaptor<BookmarkGroupRow> group = ArgumentCaptor.forClass(BookmarkGroupRow.class);
        verify(store).insertGroup(group.capture());
        assertEquals("Shared", group.getValue().name);
        assertNull(group.getValue().projectName);
        assertNotNull(group.getValue().id);
        assertEquals(group.getValue().id, row.bookmarkGroupId);
        assertNull(row.projectName);
    }

    @Test
    public void inAGroupOfAProjectItBelongsToThatProject() {
        BookmarkGroup group = new BookmarkGroup("Findings", "g");
        group.setProjectName("other");
        MetricQueryBookmarkRow row = storedIn(group);

        assertEquals("other", row.projectName);
        assertEquals("g", row.bookmarkGroupId);
        verify(store, never()).insertGroup(any());
    }

    @Test
    public void inAGlobalGroupItBelongsToNoProject() {
        MetricQueryBookmarkRow row = storedIn(new BookmarkGroup("Shared", "g"));

        assertNull(row.projectName);
        assertEquals("g", row.bookmarkGroupId);
    }

    private MetricQueryBookmarkRow storedIn(BookmarkGroup group) {
        MetricQueryBookmark bookmark = new MetricQueryBookmark.Builder(
                new QuerySpec.Builder<>().withProject("p").withMetric("m").build()).build();
        bookmark.setBookmarkGroupId(group.getId());

        new MetricsBookmarkServiceImpl(store).persistNewBookmark(bookmark, group);

        ArgumentCaptor<MetricQueryBookmarkRow> row = ArgumentCaptor.forClass(MetricQueryBookmarkRow.class);
        verify(store).insertQueryBookmark(row.capture());
        return row.getValue();
    }
}

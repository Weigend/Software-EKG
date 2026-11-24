package io.softwareecg.awb.bookmark.store;

import io.softwareecg.awb.bookmark.api.Bookmark;
import io.softwareecg.awb.bookmark.api.BookmarkContent;
import io.softwareecg.awb.bookmark.api.BookmarkGroup;
import io.softwareecg.awb.metadata.model.BookmarkGroupRow;
import io.softwareecg.awb.metadata.model.MetricQueryBookmarkRow;
import io.softwareecg.awb.metadata.port.BookmarkStore;
import io.softwareecg.awb.query.api.QuerySpec;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Which group and which project a new bookmark belongs to, which bookmarks are where, and what
 * they bring back.
 */
public class MetricsBookmarkServiceImplTest {

    private final BookmarkStore store = mock(BookmarkStore.class);

    private final MetricsBookmarkServiceImpl service = new MetricsBookmarkServiceImpl(store);

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

    @Test
    public void whatIsInNoGroupIsOfItsProjectOrGlobal() {
        stored(BookmarkGroup.DEFAULT_GLOBAL_GROUP_ID, "p", "q", null, "");

        assertEquals(List.of("in null", "in "), names(service.globalBookmarks()));
        assertEquals(List.of("in p"), names(service.bookmarksOfProject("p")));
    }

    @Test
    public void aGroupHasAllItsBookmarks() {
        stored("g", "p", null);

        assertEquals(List.of("in p", "in null"), names(service.bookmarksIn(new BookmarkGroup("Findings", "g"))));
    }

    @Test
    public void aBookmarkOfAProjectBringsItsProjectBack() {
        stored("g", "p");
        Bookmark bookmark = service.bookmarksIn(new BookmarkGroup("Findings", "g")).get(0);

        BookmarkContent content = service.contentOf(bookmark, "in use");

        assertEquals("p", content.query().getProjectName());
        assertEquals("m", content.query().getMetricName());
        assertEquals("hg", content.query().getHostGroupName());
        assertEquals(1000, content.query().getStart());
        assertEquals(2000, content.query().getEnd());
        assertEquals("AREA", content.chartType());
    }

    @Test
    public void aBookmarkOfNoProjectBringsTheProjectInUseEveryTime() {
        stored("g", (String) null);
        Bookmark bookmark = service.bookmarksIn(new BookmarkGroup("Findings", "g")).get(0);

        assertEquals("first", service.contentOf(bookmark, "first").query().getProjectName());
        BookmarkContent second = service.contentOf(bookmark, "second");
        assertEquals("second", second.query().getProjectName());
        assertEquals("m", second.query().getMetricName());
        assertEquals("hg", second.query().getHostGroupName());
        assertEquals(1000, second.query().getStart());
        assertEquals(2000, second.query().getEnd());
        assertNull(bookmark.getProject());
    }

    @Test
    public void aBookmarkInAGlobalGroupIsGlobalWhateverProjectItWasMadeIn() {
        BookmarkGroupRow group = new BookmarkGroupRow();
        group.id = "g";
        group.name = "Shared";
        when(store.listGlobalGroups()).thenReturn(List.of(group));
        stored("g", "p");
        Bookmark bookmark = service.bookmarksIn(new BookmarkGroup("Shared", "g")).get(0);

        assertEquals("in use", service.contentOf(bookmark, "in use").query().getProjectName());
        assertEquals("p", service.contentOf(bookmark, null).query().getProjectName());
    }

    /** One bookmark of every project, in the group. */
    private void stored(String groupId, String... projects) {
        when(store.listQueryBookmarks(groupId)).thenReturn(Arrays.stream(projects).map(project -> {
            MetricQueryBookmarkRow row = new MetricQueryBookmarkRow();
            row.id = "id of " + project;
            row.name = "in " + project;
            row.projectName = project;
            row.bookmarkGroupId = groupId;
            row.queryData = new MetricQueryBookmark.Builder(new QuerySpec.Builder<>().withProject("made in")
                    .withMetric("m").withHostGroup("hg").withStart(1000).withStop(2000).build())
                    .withChartType("AREA").build().toJson();
            return row;
        }).toList());
    }

    private static List<String> names(List<Bookmark> bookmarks) {
        return bookmarks.stream().map(Bookmark::getName).toList();
    }

    private MetricQueryBookmarkRow storedIn(BookmarkGroup group) {
        MetricQueryBookmark bookmark = new MetricQueryBookmark.Builder(
                new QuerySpec.Builder<>().withProject("p").withMetric("m").build()).build();
        bookmark.setBookmarkGroupId(group.getId());

        service.persistNewBookmark(bookmark, group);

        ArgumentCaptor<MetricQueryBookmarkRow> row = ArgumentCaptor.forClass(MetricQueryBookmarkRow.class);
        verify(store).insertQueryBookmark(row.capture());
        return row.getValue();
    }
}

package io.softwareecg.awb.datasource.adapter.collector;

import io.softwareecg.awb.repository.api.Repository;
import io.softwareecg.awb.timeseries.port.RepositoryException;
import io.softwareecg.awb.timeseries.port.TimeSeriesWriteService;
import io.softwareecg.awb.timeseries.TimeSeries;
import io.softwareecg.collector.repository.StaticTimeSeriesRepository;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import java.util.Map;
import java.util.HashMap;
import java.util.stream.Stream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import io.softwareecg.awb.datasource.adapter.collector.ProjectSeriesOutput;
/**
 * The output of the embedded collector towards the project repository.
 */
public class ProjectSeriesOutputTest {

    private static final String[] SERIES = {
            "cpu_usage_percent{instance=\"server-01\",job=\"node\"}",
            "cpu_usage_percent{instance=\"server-02\",job=\"node\"}"};

    private final FakeRepository repository = new FakeRepository();

    private final List<Long> notifications = new ArrayList<>();

    /** Where the stored values of each series end, by host; a series not listed has none. */
    private final Map<String, Long> storedEnds = new HashMap<>();

    private final List<String> endQuestions = new ArrayList<>();

    private long now = 10_000_000;

    private final ProjectSeriesOutput output = new ProjectSeriesOutput("Shop", "live", repository,
            (project, measurement, series) -> {
                if (repository.failing) {
                    throw new RepositoryException(new IllegalStateException("disk full"));
                }
                endQuestions.add(measurement + ":" + series.host());
                return storedEnds.getOrDefault(series.host(), 0L);
            }, notifications::add, () -> now);

    private final StaticTimeSeriesRepository writer = output.createStaticRepository(null);

    @Test
    public void valuesOfAnUpdateAreWrittenAsOneChunkPerSeriesUnderTheLiveMeasurement() {
        writer.createMeasurement(null, SERIES);
        writer.addValueSetToMeasurement(new Date(1_000), new Double[]{1d, 2d});
        writer.addValueSetToMeasurement(new Date(2_000), new Double[]{3d, null});
        assertTrue("nothing is written before the flush", repository.written.isEmpty());

        writer.flush();

        assertEquals(2, repository.written.size());
        TimeSeries first = repository.written.get(0);
        assertEquals("Shop", first.getProject());
        assertEquals("live", first.getMeasurement());
        assertEquals("node", first.getHostGroup());
        assertEquals("server-01", first.getHost());
        assertEquals("cpu", first.getMetricGroup());
        assertEquals("cpu_usage_percent", first.getMetricName());
        assertEquals(2, first.getPointCount());
        assertEquals("a missing value is left out", 1, repository.written.get(1).getPointCount());
        assertEquals(1, repository.commits);
        assertEquals("the AWB is told the time of the newest value", List.of(2_000L), notifications);
        assertEquals(new Date(2_000), writer.getLastCaptureTime());
    }

    @Test
    public void failingRepositoryIsReportedAndNothingPilesUp() {
        writer.createMeasurement(null, SERIES);
        writer.addValueSetToMeasurement(new Date(1_000), new Double[]{1d, 2d});
        writer.flush();

        repository.failing = true;
        writer.addValueSetToMeasurement(new Date(2_000), new Double[]{3d, 4d});
        try {
            writer.flush();
            fail("the failure must reach the collector job");
        } catch (IllegalStateException expected) {
            assertTrue(expected.getMessage(), expected.getMessage().contains("disk full"));
        }
        assertEquals("no notification for values that were not written", List.of(1_000L), notifications);
        try {
            writer.getLastCaptureTime();
            fail("while the repository is down nobody knows where the data ends");
        } catch (IllegalStateException expected) {
            // fine
        }

        // the job collects the rejected values again once writing works. Part of what failed may
        // be stored all the same, so the repository is asked anew where each series ends.
        repository.failing = false;
        repository.lastUpdate = 1_000;
        storedEnds.put("server-01", 1_000L);
        storedEnds.put("server-02", 1_000L);
        endQuestions.clear();
        assertEquals("the data ends where it was really written", new Date(1_000), writer.getLastCaptureTime());
        assertEquals(List.of("live:server-01", "live:server-02"), endQuestions);
        repository.written.clear();
        writer.addValueSetToMeasurement(new Date(2_000), new Double[]{3d, 4d});
        writer.addValueSetToMeasurement(new Date(3_000), new Double[]{5d, 6d});
        writer.flush();

        assertEquals(2, repository.written.size());
        assertEquals("the rejected values were dropped, not kept and written twice",
                2, repository.written.get(0).getPointCount());
    }

    @Test
    public void emptyRepositoryMeansStartFromTheBeginning() {
        writer.createMeasurement(null, SERIES);
        repository.lastUpdate = -1;

        assertNull(writer.getLastCaptureTime());
    }

    @Test
    public void existingDataTellsWhereToResume() {
        writer.createMeasurement(null, SERIES);
        repository.lastUpdate = 50_000;
        storedEnds.put("server-01", 50_000L);
        storedEnds.put("server-02", 50_000L);

        assertEquals(new Date(50_000), writer.getLastCaptureTime());
        assertEquals("only this group of the project is asked for", "node", repository.lastUpdateQuery.getHostGroup());
        assertEquals("live", repository.lastUpdateQuery.getMeasurement());
    }

    @Test
    public void anEmptyRepositoryIsNotAskedSeriesBySeries() {
        writer.createMeasurement(null, SERIES);
        repository.lastUpdate = -1;

        assertNull(writer.getLastCaptureTime());
        assertTrue("one question tells that nothing is stored: " + endQuestions, endQuestions.isEmpty());
    }

    @Test
    public void everySeriesResumesAtItsOwnEndAndNothingIsStoredTwice() {
        writer.createMeasurement(null, SERIES);
        repository.lastUpdate = 50_000;
        storedEnds.put("server-01", 50_000L);
        storedEnds.put("server-02", 30_000L);

        assertEquals("collecting resumes where the first series ends",
                new Date(30_000), writer.getLastCaptureTime());

        // the job delivers everything after that for both series
        writer.addValueSetToMeasurement(new Date(40_000), new Double[]{1d, 2d});
        writer.addValueSetToMeasurement(new Date(50_000), new Double[]{3d, 4d});
        writer.addValueSetToMeasurement(new Date(60_000), new Double[]{5d, 6d});
        writer.flush();

        assertEquals(List.of("server-01 60000", "server-02 40000 50000 60000"), written());
        assertEquals(new Date(60_000), writer.getLastCaptureTime());
    }

    @Test
    public void aSeriesNewAtTheStartIsFilledFromTheBeginningBesideTheOnesThatGoOn() {
        writer.createMeasurement(null, SERIES);
        repository.lastUpdate = 50_000;
        storedEnds.put("server-01", 50_000L);      // server-02 joined while nothing was collecting

        assertNull("the new series has to be filled in full", writer.getLastCaptureTime());

        writer.addValueSetToMeasurement(new Date(20_000), new Double[]{1d, 2d});
        writer.addValueSetToMeasurement(new Date(60_000), new Double[]{3d, 4d});
        writer.flush();

        assertEquals(List.of("server-01 60000", "server-02 20000 60000"), written());
    }

    @Test
    public void aSystemThatJoinsWhileCollectingIsFilledFromTheLastLookOnly() {
        writer.createMeasurement(null, new String[]{SERIES[0]});
        writer.addValueSetToMeasurement(new Date(10_300_000), new Double[]{1d});
        writer.flush();
        repository.lastUpdate = 10_300_000;
        storedEnds.put("server-01", 10_300_000L);
        repository.written.clear();

        // ten minutes later the job finds a second system and asks for a storage for both
        now = 10_600_000;
        StaticTimeSeriesRepository next = output.createStaticRepository(null);
        next.createMeasurement(null, SERIES);

        assertEquals("not from the very beginning: the system was not there when the job last looked",
                new Date(10_000_000 - 60_000), next.getLastCaptureTime());

        next.addValueSetToMeasurement(new Date(10_200_000), new Double[]{1d, 2d});
        next.addValueSetToMeasurement(new Date(10_400_000), new Double[]{3d, 4d});
        next.flush();

        assertEquals(List.of("server-01 10400000", "server-02 10200000 10400000"), written());
    }

    @Test
    public void everySourceWritesUnderItsOwnMeasurement() {
        StaticTimeSeriesRepository other = new ProjectSeriesOutput("Shop", "live-other.example", repository,
                (project, measurement, series) -> {
                    endQuestions.add(measurement + ":" + series.host());
                    return 0;
                }, notifications::add).createStaticRepository(null);
        other.createMeasurement(null, SERIES);
        repository.lastUpdate = 70_000;

        assertNull(other.getLastCaptureTime());
        assertEquals("live-other.example", repository.lastUpdateQuery.getMeasurement());
        assertEquals(List.of("live-other.example:server-01", "live-other.example:server-02"), endQuestions);

        other.addValueSetToMeasurement(new Date(80_000), new Double[]{1d, 2d});
        other.flush();
        assertEquals("live-other.example", repository.written.get(0).getMeasurement());
    }

    /** What was written, as host and timestamps per chunk. */
    private List<String> written() {
        return repository.written.stream()
                .map(series -> series.getHost() + " " + series.getValues().stream()
                        .map(value -> String.valueOf(value.getTimestamp())).collect(Collectors.joining(" ")))
                .collect(Collectors.toList());
    }

    @Test
    public void unknownResumePointIsAFailureNotAnEmptyRepository() {
        writer.createMeasurement(null, SERIES);
        repository.failing = true;

        try {
            writer.getLastCaptureTime();
            fail("'cannot tell' must not look like 'empty': the history would be stored twice");
        } catch (IllegalStateException expected) {
            // fine
        }
    }

    /** Stands in for the repository of the project. */
    private static final class FakeRepository implements TimeSeriesWriteService {

        final List<TimeSeries> written = new ArrayList<>();
        int commits;
        boolean failing;
        long lastUpdate = -1;
        TimeSeries lastUpdateQuery;

        @Override
        public String getRepositoryId() {
            return null;
        }

        @Override
        public long resolveLastUpdate(TimeSeries seriesWithoutValues) throws RepositoryException {
            if (failing) {
                throw new RepositoryException(new IllegalStateException("disk full"));
            }
            lastUpdateQuery = seriesWithoutValues;
            return lastUpdate;
        }

        @Override
        public void add(TimeSeries series) throws RepositoryException {
            add(List.of(series));
        }

        @Override
        public void add(Stream<TimeSeries> series) throws RepositoryException {
            add(series.toList());
        }

        @Override
        public void add(Iterable<TimeSeries> series) throws RepositoryException {
            if (failing) {
                throw new RepositoryException(new IllegalStateException("disk full"));
            }
            series.forEach(written::add);
        }

        @Override
        public void commit() {
            commits++;
        }
    }
}

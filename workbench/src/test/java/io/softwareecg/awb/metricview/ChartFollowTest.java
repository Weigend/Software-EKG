package io.softwareecg.awb.metricview;

import io.softwareecg.awb.metricview.ChartFollow.Action;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

import io.softwareecg.awb.metricview.ChartFollow;
public class ChartFollowTest {

    private static final long T = 1_795_000_000_000L;

    @Test
    public void chartShowingTheWholeRangeIsLoadedAgain() {
        assertEquals(new ChartFollow(Action.RELOAD_WHOLE_RANGE, 0), ChartFollow.decide(true, 0, 0, T));
        assertEquals(new ChartFollow(Action.RELOAD_WHOLE_RANGE, 0), ChartFollow.decide(true, T, T, T + 60_000));
    }

    @Test
    public void zoomedChartAtThePresentMovesOnByWhatWasAdded() {
        // the right edge lies a little beyond the newest value
        assertEquals(new ChartFollow(Action.MOVE_WINDOW, 60_000), ChartFollow.decide(false, T + 15_000, T, T + 60_000));
        // or exactly on it
        assertEquals(new ChartFollow(Action.MOVE_WINDOW, 15_000), ChartFollow.decide(false, T, T, T + 15_000));
    }

    @Test
    public void zoomedChartInThePastStaysWhereItIs() {
        assertEquals(new ChartFollow(Action.NONE, 0), ChartFollow.decide(false, T - 600_000, T, T + 60_000));
    }

    @Test
    public void zoomedChartDoesNotMoveBeforeItKnowsWhereTheDataEnded() {
        assertEquals(new ChartFollow(Action.NONE, 0), ChartFollow.decide(false, T + 15_000, 0, T));
    }

    @Test
    public void reportOfOlderOrSameDataMovesNothing() {
        assertEquals(new ChartFollow(Action.NONE, 0), ChartFollow.decide(false, T + 15_000, T, T));
        assertEquals(new ChartFollow(Action.NONE, 0), ChartFollow.decide(false, T + 15_000, T, T - 60_000));
    }
}

package lt.tomexas.serverselector.selector.ui;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CursorTrackerTest {

    private static final double BOUNDS_X = 3.55;
    private static final double BOUNDS_Y = 2.0;

    private CursorTracker tracker() {
        return new CursorTracker(BOUNDS_X, BOUNDS_Y, 0, 0);
    }

    @Test
    void looksStraightAheadAtOrigin() {
        CursorTracker tracker = tracker();
        tracker.update(0, 0);
        assertEquals(0.0, tracker.x(), 1e-9);
        assertEquals(0.0, tracker.y(), 1e-9);
    }

    @Test
    void turningRightMovesCursorByProjection() {
        CursorTracker tracker = tracker();
        tracker.update(30, 0);
        assertEquals(UiMath.project(30), tracker.x(), 1e-9);
        assertEquals(0.0, tracker.y(), 1e-9);
    }

    @Test
    void overshootingPastTheEdgeSticksAtTheBoundThenReleasesImmediately() {
        CursorTracker tracker = tracker();
        // Turn far past where the cursor would naturally hit the edge
        tracker.update(120, 0);
        assertEquals(BOUNDS_X, tracker.x(), 1e-9);

        // Turning back even one degree should move the cursor at once, not after undoing the whole overshoot
        tracker.update(119, 0);
        assertTrue(tracker.x() < BOUNDS_X);
    }

    @Test
    void overshootingPastTheBottomEdgeSticksThenReleasesImmediately() {
        CursorTracker tracker = tracker();
        // Pitch is positive looking down; 80 deg down should pin the cursor to the bottom edge
        tracker.update(0, 80);
        assertEquals(-BOUNDS_Y, tracker.y(), 1e-9);

        tracker.update(0, 79);
        assertTrue(tracker.y() > -BOUNDS_Y);
    }

    @Test
    void turningLeftMovesCursorNegative() {
        CursorTracker tracker = tracker();
        tracker.update(-30, 0);
        assertEquals(-UiMath.project(30), tracker.x(), 1e-9);
    }

    @Test
    void yawWrapAroundIsHandled() {
        // Anchor at 170 degrees, head turns 20 degrees further right, wrapping past 180 to -170
        CursorTracker tracker = new CursorTracker(BOUNDS_X, BOUNDS_Y, 170, 0);
        tracker.update(-170, 0);
        assertEquals(UiMath.project(20), tracker.x(), 1e-9);
    }
}

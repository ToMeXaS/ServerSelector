package lt.tomexas.serverselector.selector.ui;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RectTest {

    private final Rect rect = new Rect(1.0, -0.5, 2.0, 1.0);

    @Test
    void containsPointsInsideAndOnTheEdge() {
        assertTrue(rect.contains(1.0, -0.5));
        assertTrue(rect.contains(2.0, 0.0));
        assertTrue(rect.contains(0.0, -1.0));
    }

    @Test
    void rejectsPointsOutside() {
        assertFalse(rect.contains(2.01, -0.5));
        assertFalse(rect.contains(1.0, 0.01));
        assertFalse(rect.contains(-0.01, -0.5));
    }
}

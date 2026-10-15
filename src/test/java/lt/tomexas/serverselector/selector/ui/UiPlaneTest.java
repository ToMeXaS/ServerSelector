package lt.tomexas.serverselector.selector.ui;

import org.bukkit.Location;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class UiPlaneTest {

    // Yaw 0 faces +z (south); the player's right is then -x (west)
    private final UiPlane plane = new UiPlane(new Location(null, 0, 0, 0, 0f, 0f));

    private static void assertAt(double x, double y, double z, Location actual) {
        assertEquals(x, actual.getX(), 1e-9);
        assertEquals(y, actual.getY(), 1e-9);
        assertEquals(z, actual.getZ(), 1e-9);
    }

    @Test
    void centreIsStraightAhead() {
        assertAt(0, 0, 2, plane.point(0, 0, 2));
    }

    @Test
    void xGoesRightAndYGoesUp() {
        assertAt(-1, 0, 2, plane.point(1, 0, 2));
        assertAt(0, 1, 2, plane.point(0, 1, 2));
    }

    @Test
    void pointsFurtherAwayStayOnTheSameLineOfSight() {
        assertAt(-2, 1, 4, plane.point(1, 0.5, 4));
    }

    @Test
    void exposesTheCameraYaw() {
        assertEquals(37f, new UiPlane(new Location(null, 0, 0, 0, 37f, 10f)).yaw());
    }
}

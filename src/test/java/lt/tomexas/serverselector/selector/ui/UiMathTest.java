package lt.tomexas.serverselector.selector.ui;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class UiMathTest {

    @Test
    void wrapDegreesKeepsAnglesInRange() {
        assertEquals(0.0, UiMath.wrapDegrees(0), 1e-9);
        assertEquals(-170.0, UiMath.wrapDegrees(190), 1e-9);
        assertEquals(170.0, UiMath.wrapDegrees(-190), 1e-9);
        assertEquals(-180.0, UiMath.wrapDegrees(180), 1e-9);
        assertEquals(10.0, UiMath.wrapDegrees(370), 1e-9);
    }

    @Test
    void projectUsesTangentOnTheReferencePlane() {
        assertEquals(0.0, UiMath.project(0), 1e-9);
        assertEquals(UiMath.REFERENCE_DISTANCE, UiMath.project(45), 1e-9);
        assertEquals(-UiMath.REFERENCE_DISTANCE, UiMath.project(-45), 1e-9);
    }

    @Test
    void projectClampsNearNinetyDegrees() {
        assertEquals(UiMath.project(80), UiMath.project(89.9), 1e-9);
        assertEquals(UiMath.project(-80), UiMath.project(-120), 1e-9);
    }

    @Test
    void clampLimitsBothEnds() {
        assertEquals(1.0, UiMath.clamp(5, -1, 1));
        assertEquals(-1.0, UiMath.clamp(-5, -1, 1));
        assertEquals(0.5, UiMath.clamp(0.5, -1, 1));
    }
}

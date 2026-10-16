package lt.tomexas.serverselector.selector.screen;

import lt.tomexas.serverselector.selector.screen.CarouselLayout.Slot;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CarouselLayoutTest {

    @Test
    void threeServersWrapAroundTheSelectedOne() {
        assertEquals(Slot.CENTER, CarouselLayout.slot(0, 0, 3));
        assertEquals(Slot.RIGHT, CarouselLayout.slot(1, 0, 3));
        assertEquals(Slot.LEFT, CarouselLayout.slot(2, 0, 3));
    }

    @Test
    void twoServersPutTheOtherOneOnTheRight() {
        assertEquals(Slot.RIGHT, CarouselLayout.slot(1, 0, 2));
        assertEquals(Slot.RIGHT, CarouselLayout.slot(0, 1, 2));
    }

    @Test
    void fiveServersHideTheFarOnes() {
        assertEquals(Slot.HIDDEN_RIGHT, CarouselLayout.slot(2, 0, 5));
        assertEquals(Slot.HIDDEN_LEFT, CarouselLayout.slot(3, 0, 5));
        assertEquals(Slot.LEFT, CarouselLayout.slot(4, 0, 5));
    }

    @Test
    void offsetIsSignedAndWrapped() {
        assertEquals(-1, CarouselLayout.offset(4, 0, 5));
        assertEquals(2, CarouselLayout.offset(2, 0, 5));
        assertEquals(1, CarouselLayout.offset(0, 4, 5));
    }

    @Test
    void wrapHandlesNegativeIndices() {
        assertEquals(4, CarouselLayout.wrap(-1, 5));
        assertEquals(0, CarouselLayout.wrap(5, 5));
    }

    @Test
    void slotGeometry() {
        assertEquals(1.0f, CarouselLayout.scale(Slot.CENTER));
        assertEquals(CarouselLayout.SIDE_SCALE, CarouselLayout.scale(Slot.RIGHT));
        assertTrue(CarouselLayout.visible(Slot.LEFT));
        assertFalse(CarouselLayout.visible(Slot.HIDDEN_LEFT));
    }

    // SIDE_SCALE is a float, so allow for its rounding
    private static final double EPSILON = 1e-6;

    @Test
    void sideCardsSitAGapBeyondTheCentreCardsEdge() {
        // 1.6 / 2 + 0.2 + 2.0 * 0.7 / 2
        assertEquals(1.7, CarouselLayout.sideX(1.6, 2.0), EPSILON);
        assertEquals(2.8 / 2 + 0.2 + 1.6 * 0.7 / 2, CarouselLayout.sideX(2.8, 1.6), EPSILON);
    }

    @Test
    void hiddenCardsWaitWellPastTheCentreCard() {
        assertEquals(4.0, CarouselLayout.hiddenX(1.6), EPSILON);
    }
}

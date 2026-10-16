package lt.tomexas.serverselector.selector.ui;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TextWidthTest {

    @Test
    void emptyTextHasNoWidth() {
        assertEquals(0, TextWidth.of("", false));
    }

    @Test
    void narrowCharactersAreNarrower() {
        assertEquals(5, TextWidth.of("il", false));
    }

    @Test
    void sumsTheAdvanceOfEveryCharacter() {
        // S u r v i v a l = 6 + 6 + 6 + 6 + 2 + 6 + 6 + 3
        assertEquals(41, TextWidth.of("Survival", false));
    }

    @Test
    void boldAddsOnePerNonSpaceCharacter() {
        assertEquals(41 + 8, TextWidth.of("Survival", true));
        assertEquals(6 + 4 + 6 + 2, TextWidth.of("a a", true));
    }

    @Test
    void statusSymbolsHaveTheirOwnWidths() {
        assertEquals(6 + 4 + 2, TextWidth.of("\u25CF \u00B7", false));
    }
}

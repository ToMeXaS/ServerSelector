package lt.tomexas.serverselector.selector.ui;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.TextComponent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CardArtTest {

    private static final double EPSILON = 1e-9;

    @AfterEach
    void reset() {
        Glyph.setNexoAvailable(false);
    }

    @Test
    void fitPicksTheSmallestSizeThatHoldsTheContent() {
        CardArt.Size size = CardArt.fit(1.0, 1.0);
        assertEquals(1.6, size.width(), EPSILON);
        assertEquals(1.8, size.height(), EPSILON);
    }

    @Test
    void fitPicksWidthAndHeightIndependently() {
        CardArt.Size size = CardArt.fit(1.7, 1.9);
        assertEquals(2.0, size.width(), EPSILON);
        assertEquals(2.05, size.height(), EPSILON);
    }

    @Test
    void fitClampsToTheLargestSize() {
        CardArt.Size size = CardArt.fit(9, 9);
        assertEquals(2.8, size.width(), EPSILON);
        assertEquals(2.3, size.height(), EPSILON);
    }

    @Test
    void componentUsesTheSizedGlyphChar() {
        Glyph.setNexoAvailable(true);
        TextComponent selected = (TextComponent) CardArt.component(new CardArt.Size(1, 2), true);
        assertEquals("\uE331", selected.content());
        assertEquals(Key.key("nexo", "default"), selected.font());
        assertEquals("\uE320", ((TextComponent) CardArt.component(new CardArt.Size(0, 0), false)).content());
    }

    @Test
    void componentIsEmptyWithoutNexo() {
        assertEquals("", ((TextComponent) CardArt.component(new CardArt.Size(0, 0), true)).content());
    }

    @Test
    void everySizeIsDefinedInTheGeneratedNexoConfig() throws Exception {
        String yml = Files.readString(Path.of("nexo/glyphs/selector.yml"))
                .replace("\r", ""); // entries end at a blank line, whatever the checkout's line endings
        String[] styles = {"normal", "selected"};
        for (int style = 0; style < styles.length; style++) {
            for (int wi = 0; wi < CardArt.WIDTHS.length; wi++) {
                for (int hi = 0; hi < CardArt.HEIGHTS.length; hi++) {
                    String id = "selector_card_" + styles[style] + "_" + CardArt.WIDTHS[wi] + "x" + CardArt.HEIGHTS[hi];
                    int code = 0xE320 + style * 12 + wi * 3 + hi;
                    int at = yml.indexOf(id + ":");
                    assertTrue(at >= 0, id + " missing from selector.yml");
                    int next = yml.indexOf("\n\n", at);
                    if (next < 0) next = yml.length();
                    assertTrue(yml.substring(at, next).contains(String.format("char: \"\\u%04X\"", code)),
                            id + " has a different char in selector.yml");
                }
            }
        }
    }
}

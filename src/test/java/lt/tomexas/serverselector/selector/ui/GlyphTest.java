package lt.tomexas.serverselector.selector.ui;

import net.kyori.adventure.text.TextComponent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GlyphTest {

    @AfterEach
    void reset() {
        Glyph.setNexoAvailable(false);
    }

    @Test
    void fromIdFindsGlyphsCaseInsensitively() {
        assertEquals(Glyph.ICON_SWORD, Glyph.fromId("selector_icon_sword"));
        assertEquals(Glyph.ICON_SWORD, Glyph.fromId("SELECTOR_ICON_SWORD"));
        assertNull(Glyph.fromId("selector_icon_nope"));
    }

    @Test
    void componentUsesTheGlyphCharWithNexoAndTheFallbackWithout() {
        Glyph.setNexoAvailable(true);
        assertEquals("\uE300", ((TextComponent) Glyph.CURSOR_ARROW.component()).content());
        Glyph.setNexoAvailable(false);
        assertEquals("\u2196", ((TextComponent) Glyph.CURSOR_ARROW.component()).content());
    }

    @Test
    void everyGlyphIsDefinedInTheGeneratedNexoConfig() throws Exception {
        String yml = Files.readString(Path.of("nexo/glyphs/selector.yml"));
        Set<Character> chars = new HashSet<>();
        for (Glyph glyph : Glyph.values()) {
            assertTrue(yml.contains(glyph.id() + ":"), glyph.id() + " missing from selector.yml");
            assertTrue(yml.contains(String.format("char: \"\\u%04X\"", (int) glyph.character())),
                    glyph.id() + " has a different char in selector.yml");
            assertTrue(chars.add(glyph.character()), "duplicate char for " + glyph.id());
        }
    }
}

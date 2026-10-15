package lt.tomexas.serverselector.server;

import lt.tomexas.serverselector.selector.ui.Glyph;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.io.StringReader;
import java.util.List;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SelectorConfigTest {

    private static SelectorConfig parse(String yaml) {
        return SelectorConfig.from(YamlConfiguration.loadConfiguration(new StringReader(yaml)), Logger.getLogger("test"));
    }

    @Test
    void readsServersInConfigOrder() {
        SelectorConfig config = parse("""
                title: "PICK ONE"
                servers:
                  survival:
                    display-name: "Survival"
                    icon: selector_icon_sword
                    description: ["Line one", "Line two"]
                  creative:
                    display-name: "Creative"
                    icon: selector_icon_pickaxe
                """);
        assertEquals("PICK ONE", config.title());
        assertEquals(List.of("survival", "creative"), config.servers().stream().map(ServerEntry::id).toList());
        ServerEntry survival = config.servers().get(0);
        assertEquals("Survival", survival.displayName());
        assertEquals(Glyph.ICON_SWORD, survival.icon());
        assertEquals(List.of("Line one", "Line two"), survival.description());
        assertTrue(config.servers().get(1).description().isEmpty());
    }

    @Test
    void fallsBackForMissingValues() {
        SelectorConfig config = parse("""
                servers:
                  skyblock:
                    icon: selector_icon_unknown
                """);
        assertEquals("CHOOSE YOUR REALM", config.title());
        ServerEntry skyblock = config.servers().get(0);
        assertEquals("skyblock", skyblock.displayName());
        assertEquals(Glyph.ICON_STAR, skyblock.icon());
    }

    @Test
    void noServersSectionMeansNoServers() {
        assertTrue(parse("title: x").servers().isEmpty());
    }
}

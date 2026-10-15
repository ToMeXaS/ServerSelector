package lt.tomexas.serverselector.server;

import lt.tomexas.serverselector.selector.ui.Glyph;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

class ServerRegistryTest {

    private final ServerEntry survival = new ServerEntry("survival", "Survival", Glyph.ICON_SWORD, List.of());
    private final ServerRegistry registry = new ServerRegistry(List.of(survival));

    @Test
    void unknownUntilUpdated() {
        assertSame(ServerRegistry.Live.UNKNOWN, registry.live("survival"));
    }

    @Test
    void keepsStatusAndPlayersSeparately() {
        registry.updateStatus("survival", ServerStatus.ONLINE, "Online");
        registry.updatePlayers("survival", 12);
        registry.updateStatus("survival", ServerStatus.LIMITED, "Full");
        assertEquals(new ServerRegistry.Live(ServerStatus.LIMITED, "Full", 12), registry.live("survival"));
    }

    @Test
    void entryLookupIsCaseInsensitive() {
        assertSame(survival, registry.entry("Survival"));
        assertNull(registry.entry("creative"));
    }
}

package lt.tomexas.serverselector.server;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import lt.tomexas.serverselector.selector.ui.Glyph;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ProxyPollerTest {

    @Test
    void playerCountRepliesUpdateTheRegistry() {
        ServerRegistry registry = new ServerRegistry(List.of(new ServerEntry("survival", "Survival", Glyph.ICON_SWORD, List.of())));
        ProxyPoller poller = new ProxyPoller(null, registry);
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF("PlayerCount");
        out.writeUTF("survival");
        out.writeInt(7);
        poller.handle(out.toByteArray());
        assertEquals(7, registry.live("survival").players());
    }

    @Test
    void otherSubchannelsAreIgnored() {
        ServerRegistry registry = new ServerRegistry(List.of());
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF("GetServers");
        out.writeUTF("lobby, survival");
        ProxyPoller poller = new ProxyPoller(null, registry);
        poller.handle(out.toByteArray());
        assertEquals(0, registry.live("survival").players());
        assertNull(poller.address("survival"));
    }

    @Test
    void serverIpRepliesAreRememberedCaseInsensitively() {
        ProxyPoller poller = new ProxyPoller(null, new ServerRegistry(List.of()));
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF("ServerIP");
        out.writeUTF("Survival");
        out.writeUTF("10.0.0.5");
        out.writeShort(40000); // above Short.MAX_VALUE: must be read unsigned
        poller.handle(out.toByteArray());
        assertEquals("10.0.0.5", poller.address("survival").getHostString());
        assertEquals(40000, poller.address("SURVIVAL").getPort());
    }

    @Test
    void unknownServersHaveNoAddress() {
        assertNull(new ProxyPoller(null, new ServerRegistry(List.of())).address("survival"));
    }
}

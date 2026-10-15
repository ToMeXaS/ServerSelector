package lt.tomexas.serverselector.server;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ServerStatusTest {

    @Test
    void mapsAjQueueStatusMessages() {
        assertEquals(ServerStatus.ONLINE, ServerStatus.fromAjQueue("&aOnline"));
        assertEquals(ServerStatus.ONLINE, ServerStatus.fromAjQueue("<green>Online"));
        assertEquals(ServerStatus.OFFLINE, ServerStatus.fromAjQueue("&cOffline"));
        assertEquals(ServerStatus.OFFLINE, ServerStatus.fromAjQueue("&cRestarting"));
        assertEquals(ServerStatus.LIMITED, ServerStatus.fromAjQueue("&eFull"));
        assertEquals(ServerStatus.LIMITED, ServerStatus.fromAjQueue("&eWhitelisted"));
        assertEquals(ServerStatus.OFFLINE, ServerStatus.fromAjQueue("Invalid server"));
        assertEquals(ServerStatus.UNKNOWN, ServerStatus.fromAjQueue(""));
        assertEquals(ServerStatus.UNKNOWN, ServerStatus.fromAjQueue(null));
    }

    @Test
    void plainTextStripsFormatting() {
        assertEquals("Full", ServerStatus.plainText("&eFull"));
        assertEquals("Online", ServerStatus.plainText("<green>Online</green>"));
        assertEquals("", ServerStatus.plainText(null));
    }

    @Test
    void onlyOnlineAndLimitedAreJoinable() {
        assertTrue(ServerStatus.ONLINE.joinable());
        assertTrue(ServerStatus.LIMITED.joinable());
        assertFalse(ServerStatus.OFFLINE.joinable());
        assertFalse(ServerStatus.UNKNOWN.joinable());
    }
}

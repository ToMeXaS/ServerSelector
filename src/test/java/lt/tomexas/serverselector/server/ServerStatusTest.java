package lt.tomexas.serverselector.server;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ServerStatusTest {

    @Test
    void onlyOnlineIsJoinable() {
        assertTrue(ServerStatus.ONLINE.joinable());
        assertFalse(ServerStatus.OFFLINE.joinable());
        assertFalse(ServerStatus.UNKNOWN.joinable());
    }

    @Test
    void hasOnlyOnlineOfflineAndUnknown() {
        assertArrayEquals(new ServerStatus[] {ServerStatus.ONLINE, ServerStatus.OFFLINE, ServerStatus.UNKNOWN},
                ServerStatus.values());
    }
}

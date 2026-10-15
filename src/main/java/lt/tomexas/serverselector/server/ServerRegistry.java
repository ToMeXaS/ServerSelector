package lt.tomexas.serverselector.server;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Configured servers plus their live status and player count. Main-thread only. */
public final class ServerRegistry {

    public record Live(ServerStatus status, String label, int players) {
        public static final Live UNKNOWN = new Live(ServerStatus.UNKNOWN, "", 0);
    }

    private final List<ServerEntry> servers;
    private final Map<String, Live> live = new HashMap<>();

    public ServerRegistry(List<ServerEntry> servers) {
        this.servers = List.copyOf(servers);
    }

    public List<ServerEntry> servers() {
        return servers;
    }

    public ServerEntry entry(String id) {
        if (id == null) return null;
        for (ServerEntry server : servers) {
            if (server.id().equalsIgnoreCase(id)) return server;
        }
        return null;
    }

    public Live live(String id) {
        return live.getOrDefault(key(id), Live.UNKNOWN);
    }

    public void updateStatus(String id, ServerStatus status, String label) {
        Live current = live(id);
        live.put(key(id), new Live(status, label, current.players()));
    }

    public void updatePlayers(String id, int players) {
        Live current = live(id);
        live.put(key(id), new Live(current.status(), current.label(), players));
    }

    private static String key(String id) {
        return id.toLowerCase(Locale.ROOT);
    }
}

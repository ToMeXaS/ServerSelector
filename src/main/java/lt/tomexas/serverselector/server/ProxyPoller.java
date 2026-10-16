package lt.tomexas.serverselector.server;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Keeps the registry fresh: player counts and server addresses come from the proxy over the BungeeCord channel,
 * statuses from pinging those addresses.
 */
public final class ProxyPoller {

    private static final long INTERVAL_TICKS = 100;
    private static final int PING_TIMEOUT_MS = 2000;
    // The proxy answers ServerIP for every server it knows; still no address after this many polls means it doesn't
    private static final int ADDRESS_POLLS = 2;

    private final Plugin plugin;
    private final ServerRegistry registry;
    // Main-thread only, like the registry; pings get a snapshot
    private final Map<String, InetSocketAddress> addresses = new HashMap<>();
    private int polls;

    public ProxyPoller(Plugin plugin, ServerRegistry registry) {
        this.plugin = plugin;
        this.registry = registry;
    }

    public void start() {
        Bukkit.getScheduler().runTaskTimer(plugin, this::poll, 20L, INTERVAL_TICKS);
    }

    /** Handles a message on the BungeeCord channel: PlayerCount and ServerIP replies. */
    public void handle(byte[] message) {
        ByteArrayDataInput in = ByteStreams.newDataInput(message);
        switch (in.readUTF()) {
            case "PlayerCount" -> {
                String server = in.readUTF();
                registry.updatePlayers(server, in.readInt());
            }
            case "ServerIP" -> {
                String server = in.readUTF();
                String ip = in.readUTF();
                int port = in.readUnsignedShort();
                // Unresolved: the ping resolves it off the main thread
                addresses.put(key(server), InetSocketAddress.createUnresolved(ip, port));
            }
            default -> {
            }
        }
    }

    /** The server's address as the proxy reported it, or null if it has not. */
    InetSocketAddress address(String id) {
        return addresses.get(key(id));
    }

    private void poll() {
        // Plugin messages travel over a player's connection
        Player carrier = Bukkit.getOnlinePlayers().stream().findFirst().orElse(null);
        if (carrier == null) return;

        List<String> ids = registry.servers().stream().map(ServerEntry::id).toList();
        for (String id : ids) {
            carrier.sendPluginMessage(plugin, "BungeeCord", request("PlayerCount", id));
            carrier.sendPluginMessage(plugin, "BungeeCord", request("ServerIP", id));
        }

        for (String id : ids) {
            InetSocketAddress address = address(id);
            if (address != null) {
                ping(id, address);
            } else if (polls >= ADDRESS_POLLS) {
                registry.updateStatus(id, ServerStatus.OFFLINE, "Offline");
            }
        }
        polls++;
    }

    /** Pings off the main thread, then applies the result on it. */
    private void ping(String id, InetSocketAddress address) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            boolean online = ServerPinger.isOnline(address.getHostString(), address.getPort(), PING_TIMEOUT_MS);
            if (!plugin.isEnabled()) return;
            Bukkit.getScheduler().runTask(plugin, () -> registry.updateStatus(id,
                    online ? ServerStatus.ONLINE : ServerStatus.OFFLINE, online ? "Online" : "Offline"));
        });
    }

    private static byte[] request(String subchannel, String server) {
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF(subchannel);
        out.writeUTF(server);
        return out.toByteArray();
    }

    private static String key(String id) {
        return id.toLowerCase(Locale.ROOT);
    }
}

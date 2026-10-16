package lt.tomexas.serverselector.server;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.List;

/** Keeps the registry's player counts fresh over the BungeeCord channel. */
public final class ProxyPoller {

    private static final long INTERVAL_TICKS = 100;

    private final Plugin plugin;
    private final ServerRegistry registry;

    public ProxyPoller(Plugin plugin, ServerRegistry registry) {
        this.plugin = plugin;
        this.registry = registry;
    }

    public void start() {
        Bukkit.getScheduler().runTaskTimer(plugin, this::poll, 20L, INTERVAL_TICKS);
    }

    /** Handles a message on the BungeeCord channel. Only PlayerCount replies are used. */
    public void handle(byte[] message) {
        ByteArrayDataInput in = ByteStreams.newDataInput(message);
        if (!"PlayerCount".equals(in.readUTF())) return;
        String server = in.readUTF();
        int count = in.readInt();
        registry.updatePlayers(server, count);
    }

    private void poll() {
        // Plugin messages travel over a player's connection
        Player carrier = Bukkit.getOnlinePlayers().stream().findFirst().orElse(null);
        if (carrier == null) return;

        List<String> ids = registry.servers().stream().map(ServerEntry::id).toList();
        for (String id : ids) {
            ByteArrayDataOutput out = ByteStreams.newDataOutput();
            out.writeUTF("PlayerCount");
            out.writeUTF(id);
            carrier.sendPluginMessage(plugin, "BungeeCord", out.toByteArray());
        }
    }
}

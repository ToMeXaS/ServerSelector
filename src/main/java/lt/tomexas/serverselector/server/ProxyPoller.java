package lt.tomexas.serverselector.server;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import us.ajg0702.queue.api.spigot.AjQueueSpigotAPI;

import java.util.List;
import java.util.concurrent.TimeUnit;

/** Keeps the registry's player counts (BungeeCord channel) and statuses (ajQueue) fresh. */
public final class ProxyPoller {

    private static final long INTERVAL_TICKS = 100;
    private static final long TIMEOUT_MS = 3000;

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
        // Plugin messages and ajQueue's API both travel over a player's connection
        Player carrier = Bukkit.getOnlinePlayers().stream().findFirst().orElse(null);
        if (carrier == null) return;

        List<String> ids = registry.servers().stream().map(ServerEntry::id).toList();
        for (String id : ids) {
            ByteArrayDataOutput out = ByteStreams.newDataOutput();
            out.writeUTF("PlayerCount");
            out.writeUTF(id);
            carrier.sendPluginMessage(plugin, "BungeeCord", out.toByteArray());
        }

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            for (String id : ids) {
                ServerStatus status;
                String label;
                try {
                    String raw = AjQueueSpigotAPI.getInstance().getServerStatusString(id).get(TIMEOUT_MS, TimeUnit.MILLISECONDS);
                    status = ServerStatus.fromAjQueue(raw);
                    label = ServerStatus.plainText(raw);
                } catch (Exception e) {
                    status = ServerStatus.OFFLINE;
                    label = "Offline";
                }
                ServerStatus result = status;
                String resultLabel = label;
                Bukkit.getScheduler().runTask(plugin, () -> registry.updateStatus(id, result, resultLabel));
            }
        });
    }
}

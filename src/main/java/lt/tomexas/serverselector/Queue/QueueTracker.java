package lt.tomexas.serverselector.Queue;

import lt.tomexas.serverselector.Queue.Events.QueueJoinEvent;
import lt.tomexas.serverselector.Queue.Events.QueueLeaveEvent;
import lt.tomexas.serverselector.server.ServerEntry;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import us.ajg0702.queue.api.spigot.AjQueueSpigotAPI;
import us.ajg0702.queue.api.spigot.MessagedResponse;

import java.util.UUID;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/** Joins, leaves and reads ajQueue queues for a player. */
public final class QueueTracker {

    private static final long TIMEOUT_MS = 2000;

    private final Plugin plugin;

    public QueueTracker(Plugin plugin) {
        this.plugin = plugin;
    }

    public void join(Player player, ServerEntry server) {
        AjQueueSpigotAPI.getInstance().sudoQueue(player.getUniqueId(), server.id());
        Bukkit.getPluginManager().callEvent(new QueueJoinEvent(player, server.id()));
    }

    public void leave(Player player, String serverId) {
        // ajQueue has no API for leaving; its backend command forwards to the proxy
        player.performCommand("leavequeue " + serverId);
        Bukkit.getPluginManager().callEvent(new QueueLeaveEvent(player));
    }

    /** Reads the player's queue state off the main thread and hands it back on the main thread. */
    public void poll(Player player, Consumer<QueueState> callback) {
        UUID id = player.getUniqueId();
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            QueueState state;
            try {
                AjQueueSpigotAPI api = AjQueueSpigotAPI.getInstance();
                if (!Boolean.TRUE.equals(await(api.isInQueue(id)))) {
                    state = QueueState.NOT_QUEUED;
                } else {
                    state = QueueState.queued(
                            response(api.getRawQueueName(id), null),
                            response(api.getPosition(id), 0),
                            response(api.getTotalPositions(id), 0));
                }
            } catch (Exception e) {
                state = QueueState.UNAVAILABLE;
            }
            QueueState result = state;
            Bukkit.getScheduler().runTask(plugin, () -> callback.accept(result));
        });
    }

    private static <T> T await(Future<T> future) throws Exception {
        return future.get(TIMEOUT_MS, TimeUnit.MILLISECONDS);
    }

    private static <T> T response(Future<MessagedResponse<T>> future, T fallback) throws Exception {
        MessagedResponse<T> response = await(future);
        return response == null || response.getResponse() == null ? fallback : response.getResponse();
    }
}

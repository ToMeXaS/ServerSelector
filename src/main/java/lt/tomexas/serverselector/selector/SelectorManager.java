package lt.tomexas.serverselector.selector;

import lt.tomexas.serverselector.Queue.QueueTracker;
import lt.tomexas.serverselector.server.SelectorConfig;
import lt.tomexas.serverselector.server.ServerRegistry;
import org.bukkit.Bukkit;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

/** Opens and closes selector sessions and drives them every tick. */
public final class SelectorManager {

    private final Plugin plugin;
    private final Supplier<ArmorStand> camera;
    private final SelectorConfig config;
    private final ServerRegistry registry;
    private final QueueTracker queue;
    private final Map<UUID, SelectorSession> sessions = new HashMap<>();
    // Which server was in the middle when the player last closed the selector
    private final Map<UUID, Integer> lastSelected = new HashMap<>();
    private boolean warnedNoCamera;

    public SelectorManager(Plugin plugin, Supplier<ArmorStand> camera, SelectorConfig config, ServerRegistry registry,
                           QueueTracker queue) {
        this.plugin = plugin;
        this.camera = camera;
        this.config = config;
        this.registry = registry;
        this.queue = queue;
        Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 1L, 1L);
    }

    public void open(Player player) {
        close(player);
        ArmorStand stand = camera.get();
        if (stand == null || !stand.isValid()) {
            if (!warnedNoCamera) {
                plugin.getLogger().warning("Camera armor stand is missing or not loaded, the selector is disabled.");
                warnedNoCamera = true;
            }
            return;
        }
        sessions.put(player.getUniqueId(), new SelectorSession(plugin, player, stand, config, registry, queue,
                lastSelected.getOrDefault(player.getUniqueId(), 0)));
    }

    public void close(Player player) {
        // Removed from the map first so the dismount listener lets the player off the seat
        SelectorSession session = sessions.remove(player.getUniqueId());
        if (session == null) return;
        lastSelected.put(player.getUniqueId(), session.selectedIndex());
        session.close();
    }

    public void closeAll() {
        new ArrayList<>(sessions.values()).forEach(SelectorSession::close);
        sessions.clear();
    }

    public boolean isAttached(Player player) {
        return sessions.containsKey(player.getUniqueId());
    }

    public void click(Player player) {
        SelectorSession session = sessions.get(player.getUniqueId());
        if (session != null) session.click();
    }

    public void scroll(Player player, int direction) {
        SelectorSession session = sessions.get(player.getUniqueId());
        if (session != null) session.scroll(direction);
    }

    private void tick() {
        new ArrayList<>(sessions.values()).forEach(SelectorSession::tick);
    }
}

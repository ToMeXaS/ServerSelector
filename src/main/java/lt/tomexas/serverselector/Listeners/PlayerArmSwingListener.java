package lt.tomexas.serverselector.Listeners;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import io.papermc.paper.event.player.PlayerArmSwingEvent;
import lt.tomexas.serverselector.Main;
import lt.tomexas.serverselector.Queue.Events.QueueJoinEvent;
import lt.tomexas.serverselector.Queue.Events.QueueLeaveEvent;
import lt.tomexas.serverselector.Utils.HudManager;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.inventory.EquipmentSlot;
import us.ajg0702.queue.api.spigot.AjQueueSpigotAPI;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PlayerArmSwingListener implements Listener {

    private final Main plugin = Main.getInstance();
    private final HudManager hudManager = plugin.getHudManager();
    private final Map<UUID, Long> cooldowns = new HashMap<>();
    private static final long COOLDOWN_TIME = 3000; // 3 seconds in milliseconds

    @EventHandler
    public void onPlayerInteract(PlayerArmSwingEvent event) {
        Player player = event.getPlayer();
        if (isOnCooldown(player)) {
            player.sendMessage(MiniMessage.miniMessage().deserialize("<red>You're clicking too fast!"));
            return;
        }
        if (event.getHand().equals(EquipmentSlot.HAND)) {
            setCooldown(player);
            int currentIndex = hudManager.getPlayerServerIndex().get(player);

            if (plugin.getPlayerQueueStatus().getOrDefault(player.getUniqueId(), false)) {
                ByteArrayDataOutput stream = ByteStreams.newDataOutput();
                stream.writeUTF("RemoveFromQueue");
                stream.writeUTF(player.getUniqueId().toString());
                stream.writeUTF(plugin.getServers().get(currentIndex));
                player.sendPluginMessage(plugin, "lobby:server_selector", stream.toByteArray());
                plugin.getPlayerQueueStatus().put(player.getUniqueId(), false);
                Bukkit.getServer().getPluginManager().callEvent(new QueueLeaveEvent(player));
                return;
            }

            //ByteArrayDataOutput stream = ByteStreams.newDataOutput();
            //stream.writeUTF("Connect");
            //stream.writeUTF(plugin.getServers().get(currentIndex));
            //player.sendPluginMessage(plugin, "BungeeCord", stream.toByteArray());
            AjQueueSpigotAPI.getInstance().sudoQueue(player.getUniqueId(), plugin.getServers().get(currentIndex));
            Bukkit.getServer().getPluginManager().callEvent(new QueueJoinEvent(player, plugin.getServers().get(currentIndex)));
        }
    }

    private boolean isOnCooldown(Player player) {
        if (!cooldowns.containsKey(player.getUniqueId())) {
            return false;
        }
        long timeLeft = cooldowns.get(player.getUniqueId()) + COOLDOWN_TIME - System.currentTimeMillis();
        return timeLeft > 0;
    }

    private void setCooldown(Player player) {
        cooldowns.put(player.getUniqueId(), System.currentTimeMillis());
    }

}
